package com.nhavronskyi.progressmebackend.chat;

import com.nhavronskyi.progressmebackend.ai.AiContextService;
import com.nhavronskyi.progressmebackend.ai.SystemPromptBuilder;
import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.chat.dto.ChatMessageResponse;
import com.nhavronskyi.progressmebackend.chat.dto.ChatSessionResponse;
import com.nhavronskyi.progressmebackend.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AiContextService contextService;
    private final ChatClient chatClient;
    private final SystemPromptBuilder promptBuilder;

    @Transactional
    public ChatSessionResponse createSession(UUID userId, String messageContent) {
        ChatSession session = sessionRepository.save(new ChatSession(userId));
        ChatMessageResponse firstResponse = sendMessage(userId, session.getId(), messageContent);
        generateTitle(session.getId(), messageContent, firstResponse.content());
        return ChatSessionResponse.from(sessionRepository.findById(session.getId()).orElseThrow());
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID userId, UUID sessionId, String messageContent) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        ChatMessage userMessage = messageRepository.save(
                new ChatMessage(session, ChatMessage.Role.USER, messageContent));

        List<ChatMessage> history = loadHistory(sessionId);

        AiContext context = contextService.buildContext(userId);
        String systemPrompt = promptBuilder.build(context);

        List<Message> messages = buildMessageList(systemPrompt, history);

        log.info("Sending chat message for user={}, session={}, historySize={}",
                userId, sessionId, history.size());

        ChatResponse chatResponse = chatClient.prompt()
                .messages(messages)
                .call()
                .chatResponse();

        String responseText = Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(org.springframework.ai.chat.messages.AbstractMessage::getText)
                .orElseThrow(() -> new IllegalStateException("Empty AI response"));

        int tokens = chatResponse.getMetadata().getUsage().getTotalTokens();

        ChatMessage assistantMessage = new ChatMessage(session, ChatMessage.Role.ASSISTANT, responseText);
        assistantMessage.setTokensUsed(tokens);
        messageRepository.save(assistantMessage);

        return ChatMessageResponse.from(assistantMessage);
    }

    @Transactional(readOnly = true)
    public Page<ChatSessionResponse> getSessions(UUID userId, Pageable pageable) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(ChatSessionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getSessionHistory(UUID userId, UUID sessionId, Pageable pageable) {
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, pageable)
                .map(ChatMessageResponse::from);
    }

    @Async
    @Transactional
    public void generateTitle(UUID sessionId, String userMessage, String assistantResponse) {
        try {
            String titlePrompt = "Generate a short title (max 6 words) for this conversation. " +
                    "Return ONLY the title, no quotes or extra text.\n\n" +
                    "User: " + userMessage.substring(0, Math.min(userMessage.length(), 500)) + "\n" +
                    "Assistant: " + assistantResponse.substring(0, Math.min(assistantResponse.length(), 500));

            ChatResponse response = chatClient.prompt()
                    .user(titlePrompt)
                    .call()
                    .chatResponse();

            String title = Optional.ofNullable(response)
                    .map(ChatResponse::getResult)
                    .map(Generation::getOutput)
                    .map(org.springframework.ai.chat.messages.AbstractMessage::getText)
                    .orElse(null);

            if (title != null) {
                String trimmed = title.strip();
                String finalTitle = trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
                sessionRepository.findById(sessionId).ifPresent(session -> {
                    session.setTitle(finalTitle);
                    sessionRepository.save(session);
                });
            }
        } catch (Exception exception) {
            log.warn("Failed to generate title for session {}: {}", sessionId, exception.getMessage());
        }
    }

    private List<ChatMessage> loadHistory(UUID sessionId) {
        List<ChatMessage> recent = messageRepository.findTop50BySessionIdOrderByCreatedAtDesc(sessionId);
        List<ChatMessage> chronological = new ArrayList<>(recent);
        Collections.reverse(chronological);
        return chronological;
    }

    private List<Message> buildMessageList(String systemPrompt, List<ChatMessage> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        for (ChatMessage chatMessage : history) {
            Message message = switch (chatMessage.getRole()) {
                case USER -> new UserMessage(chatMessage.getContent());
                case ASSISTANT -> new AssistantMessage(chatMessage.getContent());
            };
            messages.add(message);
        }
        return messages;
    }
}

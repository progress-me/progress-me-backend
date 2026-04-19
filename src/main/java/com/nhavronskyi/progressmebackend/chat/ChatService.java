package com.nhavronskyi.progressmebackend.chat;

import com.nhavronskyi.progressmebackend.ai.AiContextService;
import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.ai.orchestrator.ActionProposal;
import com.nhavronskyi.progressmebackend.ai.orchestrator.OrchestratorService;
import com.nhavronskyi.progressmebackend.ai.orchestrator.StreamingTaskResult;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskResult;
import com.nhavronskyi.progressmebackend.chat.dto.ChatMessageResponse;
import com.nhavronskyi.progressmebackend.chat.dto.ChatSessionResponse;
import com.nhavronskyi.progressmebackend.common.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AiContextService contextService;
    private final OrchestratorService orchestratorService;
    private final ChatClient executionClient;
    private final TransactionTemplate transactionTemplate;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       AiContextService contextService,
                       OrchestratorService orchestratorService,
                       @Qualifier("executionClient") ChatClient executionClient,
                       TransactionTemplate transactionTemplate) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.contextService = contextService;
        this.orchestratorService = orchestratorService;
        this.executionClient = executionClient;
        this.transactionTemplate = transactionTemplate;
    }

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

        messageRepository.save(new ChatMessage(session, ChatMessage.Role.USER, messageContent));

        List<ChatMessage> history = loadHistory(sessionId);
        AiContext context = contextService.buildContext(userId);

        log.info("Sending chat message for user={}, session={}, historySize={}",
                userId, sessionId, history.size());

        TaskResult result = orchestratorService.processMessage(userId, messageContent, history, context);

        ChatMessage assistantMessage = new ChatMessage(session, ChatMessage.Role.ASSISTANT, result.responseText());

        ActionProposal proposal = result.proposal();
        if (proposal != null) {
            assistantMessage.setActionType(proposal.taskType().name());
            assistantMessage.setActionPayload(proposal.payload());
            assistantMessage.setActionStatus(ChatMessage.ActionStatus.PROPOSED.name());
        }

        messageRepository.save(assistantMessage);
        sessionRepository.save(session);
        return ChatMessageResponse.from(assistantMessage);
    }

    public SseEmitter sendMessageStream(UUID userId, UUID sessionId, String messageContent) {
        SseEmitter emitter = new SseEmitter(120_000L);

        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        transactionTemplate.executeWithoutResult(status ->
                messageRepository.save(new ChatMessage(session, ChatMessage.Role.USER, messageContent)));

        List<ChatMessage> history = loadHistory(sessionId);
        AiContext context = contextService.buildContext(userId);

        log.info("Streaming chat message for user={}, session={}, historySize={}",
                userId, sessionId, history.size());

        StreamingTaskResult streamResult = orchestratorService.processMessageStream(
                userId, messageContent, history, context, phase -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("status")
                                .data(Map.of("phase", phase)));
                    } catch (IOException exception) {
                        log.debug("Client disconnected during status phase: {}", exception.getMessage());
                    }
                });

        StringBuilder accumulated = new StringBuilder();

        Disposable subscription = streamResult.tokenStream()
                .doOnNext(token -> {
                    accumulated.append(token);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(Map.of("text", token)));
                    } catch (IOException exception) {
                        log.debug("Client disconnected during token streaming: {}", exception.getMessage());
                    }
                })
                .doOnComplete(() -> {
                    try {
                        String fullText = accumulated.toString();
                        TaskResult result = streamResult.finalizer().apply(fullText);

                        ChatMessage assistantMessage = new ChatMessage(
                                session, ChatMessage.Role.ASSISTANT, result.responseText());

                        ActionProposal proposal = result.proposal();
                        if (proposal != null) {
                            assistantMessage.setActionType(proposal.taskType().name());
                            assistantMessage.setActionPayload(proposal.payload());
                            assistantMessage.setActionStatus(ChatMessage.ActionStatus.PROPOSED.name());
                        }

                        transactionTemplate.executeWithoutResult(status -> {
                            messageRepository.save(assistantMessage);
                            sessionRepository.save(session);
                        });

                        if (proposal != null) {
                            Map<String, Object> proposalData = new LinkedHashMap<>();
                            proposalData.put("actionType", proposal.taskType().name());
                            proposalData.put("actionPayload", proposal.payload());
                            proposalData.put("humanReadableSummary", proposal.humanReadableSummary());
                            emitter.send(SseEmitter.event()
                                    .name("proposal")
                                    .data(proposalData));
                        }

                        Map<String, Object> doneData = new LinkedHashMap<>();
                        doneData.put("messageId", assistantMessage.getId());
                        doneData.put("content", result.responseText());
                        if (proposal != null) {
                            doneData.put("actionType", proposal.taskType().name());
                            doneData.put("actionPayload", proposal.payload());
                            doneData.put("actionStatus", ChatMessage.ActionStatus.PROPOSED.name());
                        }
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(doneData));
                        emitter.complete();
                    } catch (Exception exception) {
                        log.error("Error completing stream for user={}, session={}: {}",
                                userId, sessionId, exception.getMessage(), exception);
                        emitter.completeWithError(exception);
                    }
                })
                .doOnError(error -> {
                    log.error("Streaming error for user={}, session={}: {}",
                            userId, sessionId, error.getMessage(), error);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(Map.of("error", error.getMessage())));
                    } catch (IOException ignored) {
                    }
                    emitter.completeWithError(error);
                })
                .subscribe();

        emitter.onTimeout(subscription::dispose);
        emitter.onError(t -> subscription.dispose());

        return emitter;
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

            ChatResponse response = executionClient.prompt()
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

}

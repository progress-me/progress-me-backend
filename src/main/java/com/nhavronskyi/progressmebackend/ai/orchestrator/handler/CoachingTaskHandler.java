package com.nhavronskyi.progressmebackend.ai.orchestrator.handler;

import com.nhavronskyi.progressmebackend.ai.SystemPromptBuilder;
import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.ai.orchestrator.StreamingTaskResult;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskHandler;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskResult;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskType;
import com.nhavronskyi.progressmebackend.chat.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class CoachingTaskHandler implements TaskHandler {

    private final ChatClient reasoningClient;
    private final ChatClient executionClient;
    private final SystemPromptBuilder promptBuilder;

    public CoachingTaskHandler(@Qualifier("reasoningClient") ChatClient reasoningClient,
                               @Qualifier("executionClient") ChatClient executionClient,
                               SystemPromptBuilder promptBuilder) {
        this.reasoningClient = reasoningClient;
        this.executionClient = executionClient;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.COACHING;
    }

    @Override
    public TaskResult handle(UUID userId, String userMessage, List<ChatMessage> history, AiContext context) {
        String systemPrompt = promptBuilder.build(context);
        List<Message> messages = buildMessages(systemPrompt, history);

        String responseText = callWithFallback(messages);
        return new TaskResult(responseText, null);
    }

    private String callWithFallback(List<Message> messages) {
        try {
            return callClient(reasoningClient, messages);
        } catch (Exception exception) {
            log.warn("Reasoning model failed, falling back to execution model: {}", exception.getMessage());
            return callClient(executionClient, messages);
        }
    }

    private String callClient(ChatClient client, List<Message> messages) {
        ChatResponse chatResponse = client.prompt()
                .messages(messages)
                .call()
                .chatResponse();

        return Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(org.springframework.ai.chat.messages.AbstractMessage::getText)
                .orElseThrow(() -> new IllegalStateException("Empty AI response"));
    }

    @Override
    public StreamingTaskResult handleStream(UUID userId, String userMessage,
                                             List<ChatMessage> history, AiContext context) {
        String systemPrompt = promptBuilder.build(context);
        List<Message> messages = buildMessages(systemPrompt, history);

        Flux<String> tokenStream = streamWithFallback(messages);
        return new StreamingTaskResult(tokenStream, text -> new TaskResult(text, null));
    }

    private Flux<String> streamWithFallback(List<Message> messages) {
        return streamClient(reasoningClient, messages)
                .onErrorResume(exception -> {
                    log.warn("Reasoning model stream failed, falling back to execution model: {}",
                            exception.getMessage());
                    return streamClient(executionClient, messages);
                });
    }

    private Flux<String> streamClient(ChatClient client, List<Message> messages) {
        return client.prompt()
                .messages(messages)
                .stream()
                .content()
                .filter(text -> text != null && !text.isEmpty());
    }

    private List<Message> buildMessages(String systemPrompt, List<ChatMessage> history) {
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

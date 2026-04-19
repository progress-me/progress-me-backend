package com.nhavronskyi.progressmebackend.ai.orchestrator.handler;

import com.nhavronskyi.progressmebackend.ai.SystemPromptBuilder;
import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.ai.orchestrator.StreamingTaskResult;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskHandler;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskResult;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskType;
import com.nhavronskyi.progressmebackend.chat.ChatMessage;
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

@Component
public class SearchTaskHandler implements TaskHandler {

    private static final String SEARCH_SYSTEM_SUFFIX = """

            ## Search Mode
            The user is asking about their history, trends, or past data.
            Answer based ONLY on the log entries, goals, and categories provided in the context above.
            If the data is insufficient to answer, clearly state what data is missing.
            Be concise and factual. Use numbers and dates when available.
            Do not fabricate any entries or data points.
            """;

    private final ChatClient executionClient;
    private final SystemPromptBuilder promptBuilder;

    public SearchTaskHandler(@Qualifier("executionClient") ChatClient executionClient,
                             SystemPromptBuilder promptBuilder) {
        this.executionClient = executionClient;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.SEARCH;
    }

    @Override
    public TaskResult handle(UUID userId, String userMessage, List<ChatMessage> history, AiContext context) {
        String systemPrompt = promptBuilder.build(context) + SEARCH_SYSTEM_SUFFIX;

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        int historyStart = Math.max(0, history.size() - 10);
        for (int i = historyStart; i < history.size(); i++) {
            ChatMessage chatMessage = history.get(i);
            Message message = switch (chatMessage.getRole()) {
                case USER -> new UserMessage(chatMessage.getContent());
                case ASSISTANT -> new AssistantMessage(chatMessage.getContent());
            };
            messages.add(message);
        }

        ChatResponse chatResponse = executionClient.prompt()
                .messages(messages)
                .call()
                .chatResponse();

        String responseText = Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(org.springframework.ai.chat.messages.AbstractMessage::getText)
                .orElseThrow(() -> new IllegalStateException("Empty AI response"));

        return new TaskResult(responseText, null);
    }

    @Override
    public StreamingTaskResult handleStream(UUID userId, String userMessage,
                                             List<ChatMessage> history, AiContext context) {
        String systemPrompt = promptBuilder.build(context) + SEARCH_SYSTEM_SUFFIX;

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        int historyStart = Math.max(0, history.size() - 10);
        for (int i = historyStart; i < history.size(); i++) {
            ChatMessage chatMessage = history.get(i);
            Message message = switch (chatMessage.getRole()) {
                case USER -> new UserMessage(chatMessage.getContent());
                case ASSISTANT -> new AssistantMessage(chatMessage.getContent());
            };
            messages.add(message);
        }

        Flux<String> tokenStream = executionClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .filter(text -> text != null && !text.isEmpty());

        return new StreamingTaskResult(tokenStream, text -> new TaskResult(text, null));
    }
}

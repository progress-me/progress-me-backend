package com.nhavronskyi.progressmebackend.ai.orchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.chat.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class IntentClassifier {

    private static final String CLASSIFICATION_PROMPT = """
            You are an intent classifier for a personal progress tracking app.
            Given the user's message and recent conversation context, classify the intent into exactly one task type.

            Task types:
            - DAILY_NOTE: User is describing their day, activities, feelings, accomplishments, or wants to log/journal something. They are sharing what happened, not asking for advice.
            - COACHING: User wants advice, analysis, pattern detection, motivation, reflection, feedback, or deep conversation about their progress.
            - GOAL_MANAGEMENT: User wants to create, update, pause, complete, or manage goals (e.g. "I want to start learning Spanish", "pause my fitness goal").
            - SEARCH: User asks about their history, trends, past entries, specific dates, statistics (e.g. "how was my mood last week?", "what did I do on Monday?").

            Respond ONLY with valid JSON, no markdown fences:
            {"taskType":"DAILY_NOTE|COACHING|GOAL_MANAGEMENT|SEARCH"}
            """;

    private static final String CONFIRMATION_PROMPT = """
            The AI previously proposed an action (creating a note or managing a goal).
            The user replied with the message below. Classify their response.

            - CONFIRM: User agrees (yes, ok, sure, looks good, confirm, do it, save it, etc.)
            - REJECT: User disagrees (no, cancel, nevermind, don't, discard, etc.)
            - EDIT: User wants to modify the proposal (change the mood, update the title, add something, etc.)

            Respond ONLY with valid JSON, no markdown fences:
            {"response":"CONFIRM|REJECT|EDIT"}
            """;

    public enum ConfirmationResult {
        CONFIRM, REJECT, EDIT
    }

    private final ChatClient orchestratorClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntentClassifier(@Qualifier("orchestratorClient") ChatClient orchestratorClient) {
        this.orchestratorClient = orchestratorClient;
    }

    public TaskType classify(String userMessage, List<ChatMessage> recentHistory) {
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(CLASSIFICATION_PROMPT));

            int historyStart = Math.max(0, recentHistory.size() - 6);
            for (int i = historyStart; i < recentHistory.size(); i++) {
                ChatMessage chatMessage = recentHistory.get(i);
                Message message = switch (chatMessage.getRole()) {
                    case USER -> new UserMessage(chatMessage.getContent());
                    case ASSISTANT -> new AssistantMessage(chatMessage.getContent());
                };
                messages.add(message);
            }

            messages.add(new UserMessage("Classify this message: " + userMessage));

            String response = orchestratorClient.prompt()
                    .messages(messages)
                    .call()
                    .content();

            return parseTaskType(response);
        } catch (Exception exception) {
            log.warn("Intent classification failed, falling back to COACHING: {}", exception.getMessage());
            return TaskType.COACHING;
        }
    }

    public ConfirmationResult classifyConfirmation(String userMessage) {
        try {
            String response = orchestratorClient.prompt()
                    .system(CONFIRMATION_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();

            return parseConfirmation(response);
        } catch (Exception exception) {
            log.warn("Confirmation classification failed, treating as EDIT: {}", exception.getMessage());
            return ConfirmationResult.EDIT;
        }
    }

    private TaskType parseTaskType(String response) {
        try {
            String cleaned = response.strip()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("```$", "")
                    .strip();
            JsonNode node = objectMapper.readTree(cleaned);
            String taskType = node.get("taskType").asText();
            return TaskType.valueOf(taskType);
        } catch (JsonProcessingException | IllegalArgumentException | NullPointerException exception) {
            log.warn("Failed to parse task type from response '{}', falling back to COACHING", response);
            return TaskType.COACHING;
        }
    }

    private ConfirmationResult parseConfirmation(String response) {
        try {
            String cleaned = response.strip()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("```$", "")
                    .strip();
            JsonNode node = objectMapper.readTree(cleaned);
            String result = node.get("response").asText();
            return ConfirmationResult.valueOf(result);
        } catch (JsonProcessingException | IllegalArgumentException | NullPointerException exception) {
            log.warn("Failed to parse confirmation from response '{}', treating as EDIT", response);
            return ConfirmationResult.EDIT;
        }
    }
}

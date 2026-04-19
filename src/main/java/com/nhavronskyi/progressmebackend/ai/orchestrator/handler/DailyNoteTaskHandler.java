package com.nhavronskyi.progressmebackend.ai.orchestrator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.ai.orchestrator.ActionProposal;
import com.nhavronskyi.progressmebackend.ai.orchestrator.StreamingTaskResult;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskHandler;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskResult;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskType;
import com.nhavronskyi.progressmebackend.category.Category;
import com.nhavronskyi.progressmebackend.chat.ChatMessage;
import com.nhavronskyi.progressmebackend.log.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class DailyNoteTaskHandler implements TaskHandler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ChatClient executionClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DailyNoteTaskHandler(@Qualifier("executionClient") ChatClient executionClient) {
        this.executionClient = executionClient;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.DAILY_NOTE;
    }

    @Override
    public TaskResult handle(UUID userId, String userMessage, List<ChatMessage> history, AiContext context) {
        LogEntry existingEntry = findTodayEntry(context.recentEntries());
        String systemPrompt = buildNotePrompt(context, existingEntry);
        List<Message> messages = buildMessages(systemPrompt, history);

        String response = executionClient.prompt()
                .messages(messages)
                .call()
                .content();

        return parseResponse(response, context.categories(), existingEntry);
    }

    @Override
    public StreamingTaskResult handleStream(UUID userId, String userMessage,
                                             List<ChatMessage> history, AiContext context) {
        LogEntry existingEntry = findTodayEntry(context.recentEntries());
        String systemPrompt = buildNotePrompt(context, existingEntry);

        List<Message> messages = buildMessages(systemPrompt, history);

        Flux<String> tokenStream = executionClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .filter(text -> text != null && !text.isEmpty());

        List<Category> categories = context.categories();
        return new StreamingTaskResult(tokenStream,
                fullText -> parseResponse(fullText, categories, existingEntry));
    }

    @Override
    public StreamingTaskResult handleStreamWithPriorProposal(UUID userId, String userMessage,
                                                              List<ChatMessage> history, AiContext context,
                                                              Map<String, Object> priorPayload) {
        String systemPrompt = buildEditPrompt(context, priorPayload);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userMessage));

        Flux<String> tokenStream = executionClient.prompt()
                .messages(messages)
                .stream()
                .content()
                .filter(text -> text != null && !text.isEmpty());

        LogEntry existingEntry = findTodayEntry(context.recentEntries());
        List<Category> categories = context.categories();
        return new StreamingTaskResult(tokenStream,
                fullText -> parseResponse(fullText, categories, existingEntry));
    }

    @Override
    public TaskResult handleWithPriorProposal(UUID userId, String userMessage,
                                               List<ChatMessage> history, AiContext context,
                                               Map<String, Object> priorPayload) {
        String systemPrompt = buildEditPrompt(context, priorPayload);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userMessage));

        String response = executionClient.prompt()
                .messages(messages)
                .call()
                .content();

        LogEntry existingEntry = findTodayEntry(context.recentEntries());
        return parseResponse(response, context.categories(), existingEntry);
    }

    private List<Message> buildMessages(String systemPrompt, List<ChatMessage> history) {
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
        return messages;
    }

    private String buildNotePrompt(AiContext context, LogEntry existingEntry) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are a journaling assistant. The user is describing their day or activities.
                Your job is to create a structured journal entry from what they share.

                """);

        if (existingEntry != null) {
            sb.append("## Existing entry for today\n");
            sb.append("Content: ").append(existingEntry.getContent()).append("\n");
            if (existingEntry.getMood() != null) sb.append("Mood: ").append(existingEntry.getMood()).append("/10\n");
            if (existingEntry.getEnergy() != null) sb.append("Energy: ").append(existingEntry.getEnergy()).append("/10\n");
            sb.append("\nThe user may want to UPDATE this entry with new information. Merge the new info with the existing content.\n\n");
        }

        appendCategories(sb, context.categories());
        appendResponseFormat(sb, existingEntry != null);

        return sb.toString();
    }

    private String buildEditPrompt(AiContext context, Map<String, Object> priorPayload) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are a journaling assistant. The user wants to modify a previously proposed journal entry.
                Here is the prior proposal:

                """);

        try {
            sb.append(objectMapper.writeValueAsString(priorPayload));
        } catch (JsonProcessingException exception) {
            sb.append(priorPayload.toString());
        }

        sb.append("\n\nApply the user's requested changes to this proposal.\n\n");
        appendCategories(sb, context.categories());

        boolean isUpdate = "UPDATE".equals(priorPayload.get("action"));
        appendResponseFormat(sb, isUpdate);

        return sb.toString();
    }

    private void appendCategories(StringBuilder sb, List<Category> categories) {
        if (!categories.isEmpty()) {
            sb.append("## Available categories\n");
            for (Category category : categories) {
                sb.append("- ").append(category.getName());
                if (category.getDescription() != null) {
                    sb.append(": ").append(category.getDescription());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
    }

    private void appendResponseFormat(StringBuilder sb, boolean isUpdate) {
        sb.append("""
                ## Response format
                Respond with a brief conversational acknowledgment, then include a JSON block with the entry data.
                The JSON block MUST be wrapped in ```json fences.

                """);

        if (isUpdate) {
            sb.append("""
                    JSON structure:
                    ```json
                    {
                      "action": "UPDATE",
                      "content": "merged journal content incorporating both existing and new information",
                      "mood": 1-10 or null,
                      "energy": 1-10 or null,
                      "categoryNames": ["matching category names from the list above"]
                    }
                    ```
                    """);
        } else {
            sb.append("""
                    JSON structure:
                    ```json
                    {
                      "action": "CREATE",
                      "type": "journal",
                      "content": "well-written journal entry summarizing what the user shared",
                      "mood": 1-10 or null (infer from tone if possible),
                      "energy": 1-10 or null (infer from context if possible),
                      "categoryNames": ["matching category names from the list above"]
                    }
                    ```
                    """);
        }
    }

    private TaskResult parseResponse(String response, List<Category> categories, LogEntry existingEntry) {
        try {
            int jsonStart = response.indexOf("```json");
            int jsonEnd = response.lastIndexOf("```");

            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                log.warn("No JSON block found in daily note response");
                return new TaskResult(response, null);
            }

            String conversational = response.substring(0, jsonStart).strip();
            String jsonStr = response.substring(jsonStart + 7, jsonEnd).strip();

            JsonNode node = objectMapper.readTree(jsonStr);

            Map<String, Object> payload = new LinkedHashMap<>();
            String action = node.has("action") ? node.get("action").asText() : "CREATE";
            payload.put("action", action);

            if ("UPDATE".equals(action) && existingEntry != null) {
                payload.put("entryId", existingEntry.getId().toString());
            }

            if (node.has("type")) payload.put("type", node.get("type").asText());
            if (node.has("content")) payload.put("content", node.get("content").asText());
            if (node.has("mood") && !node.get("mood").isNull()) payload.put("mood", node.get("mood").asInt());
            if (node.has("energy") && !node.get("energy").isNull()) payload.put("energy", node.get("energy").asInt());

            if (node.has("categoryNames") && node.get("categoryNames").isArray()) {
                List<String> categoryIds = resolveCategoryIds(node.get("categoryNames"), categories);
                if (!categoryIds.isEmpty()) {
                    payload.put("categoryIds", categoryIds);
                }
            }

            String summary = buildSummary(payload);
            ActionProposal proposal = new ActionProposal(TaskType.DAILY_NOTE, payload, summary);

            return new TaskResult(conversational, proposal);
        } catch (Exception exception) {
            log.warn("Failed to parse daily note response, returning as plain text: {}", exception.getMessage());
            return new TaskResult(response, null);
        }
    }

    private List<String> resolveCategoryIds(JsonNode categoryNames, List<Category> categories) {
        Map<String, UUID> categoryMap = new HashMap<>();
        for (Category category : categories) {
            categoryMap.put(category.getName().toLowerCase(), category.getId());
        }

        List<String> resolved = new ArrayList<>();
        for (JsonNode nameNode : categoryNames) {
            String name = nameNode.asText().toLowerCase();
            UUID categoryId = categoryMap.get(name);
            if (categoryId != null) {
                resolved.add(categoryId.toString());
            }
        }
        return resolved;
    }

    private String buildSummary(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder();
        String action = (String) payload.getOrDefault("action", "CREATE");
        sb.append("CREATE".equals(action) ? "Create" : "Update").append(" journal entry");

        Object mood = payload.get("mood");
        Object energy = payload.get("energy");
        if (mood != null) sb.append(" | Mood: ").append(mood).append("/10");
        if (energy != null) sb.append(" | Energy: ").append(energy).append("/10");

        return sb.toString();
    }

    private LogEntry findTodayEntry(List<LogEntry> recentEntries) {
        LocalDate today = LocalDate.now();
        for (LogEntry entry : recentEntries) {
            if ("journal".equals(entry.getType())
                    && entry.getLoggedAt().toLocalDate().equals(today)) {
                return entry;
            }
        }
        return null;
    }
}

package com.nhavronskyi.progressmebackend.ai.orchestrator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.ai.orchestrator.ActionProposal;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskHandler;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskResult;
import com.nhavronskyi.progressmebackend.ai.orchestrator.TaskType;
import com.nhavronskyi.progressmebackend.category.Category;
import com.nhavronskyi.progressmebackend.chat.ChatMessage;
import com.nhavronskyi.progressmebackend.goal.Goal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class GoalManagementTaskHandler implements TaskHandler {

    private final ChatClient executionClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoalManagementTaskHandler(@Qualifier("executionClient") ChatClient executionClient) {
        this.executionClient = executionClient;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GOAL_MANAGEMENT;
    }

    @Override
    public TaskResult handle(UUID userId, String userMessage, List<ChatMessage> history, AiContext context) {
        String systemPrompt = buildGoalPrompt(context);

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

        String response = executionClient.prompt()
                .messages(messages)
                .call()
                .content();

        return parseResponse(response, context);
    }

    @Override
    public TaskResult handleWithPriorProposal(UUID userId, String userMessage,
                                               List<ChatMessage> history, AiContext context,
                                               Map<String, Object> priorPayload) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are a goal management assistant. The user wants to modify a previously proposed goal action.
                Here is the prior proposal:

                """);

        try {
            sb.append(objectMapper.writeValueAsString(priorPayload));
        } catch (JsonProcessingException exception) {
            sb.append(priorPayload.toString());
        }

        sb.append("\n\nApply the user's requested changes to this proposal.\n\n");
        appendGoalContext(sb, context);
        appendResponseFormat(sb);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(sb.toString()));
        messages.add(new UserMessage(userMessage));

        String response = executionClient.prompt()
                .messages(messages)
                .call()
                .content();

        return parseResponse(response, context);
    }

    private String buildGoalPrompt(AiContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are a goal management assistant. The user wants to create, update, pause, or complete a goal.
                Analyze what they want and propose the appropriate action.

                """);

        appendGoalContext(sb, context);
        appendResponseFormat(sb);
        return sb.toString();
    }

    private void appendGoalContext(StringBuilder sb, AiContext context) {
        if (!context.activeGoals().isEmpty()) {
            sb.append("## User's current goals\n");
            for (Goal goal : context.activeGoals()) {
                sb.append("- **").append(goal.getTitle()).append("** (id: ").append(goal.getId()).append(")");
                sb.append(" [status: ").append(goal.getStatus()).append("]");
                if (goal.getDescription() != null) {
                    sb.append(" — ").append(goal.getDescription());
                }
                if (goal.getTargetDate() != null) {
                    sb.append(" (target: ").append(goal.getTargetDate()).append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!context.categories().isEmpty()) {
            sb.append("## Available categories\n");
            for (Category category : context.categories()) {
                sb.append("- ").append(category.getName());
                if (category.getDescription() != null) {
                    sb.append(": ").append(category.getDescription());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
    }

    private void appendResponseFormat(StringBuilder sb) {
        sb.append("""
                ## Response format
                Respond with a brief conversational message, then include a JSON block with the goal action.
                The JSON block MUST be wrapped in ```json fences.

                For CREATE:
                ```json
                {
                  "action": "CREATE",
                  "title": "goal title",
                  "description": "goal description",
                  "targetDate": "YYYY-MM-DD or null",
                  "categoryNames": ["matching category names"]
                }
                ```

                For UPDATE (use the goal id from the list above):
                ```json
                {
                  "action": "UPDATE",
                  "goalId": "uuid of existing goal",
                  "title": "updated title",
                  "description": "updated description",
                  "targetDate": "YYYY-MM-DD or null",
                  "categoryNames": ["matching category names"]
                }
                ```

                For PAUSE or COMPLETE:
                ```json
                {
                  "action": "PAUSE" or "COMPLETE",
                  "goalId": "uuid of existing goal",
                  "title": "goal title for reference"
                }
                ```
                """);
    }

    private TaskResult parseResponse(String response, AiContext context) {
        try {
            int jsonStart = response.indexOf("```json");
            int jsonEnd = response.lastIndexOf("```");

            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                log.warn("No JSON block found in goal management response");
                return new TaskResult(response, null);
            }

            String conversational = response.substring(0, jsonStart).strip();
            String jsonStr = response.substring(jsonStart + 7, jsonEnd).strip();

            JsonNode node = objectMapper.readTree(jsonStr);

            Map<String, Object> payload = new LinkedHashMap<>();
            String action = node.get("action").asText();
            payload.put("action", action);

            if (node.has("goalId") && !node.get("goalId").isNull()) {
                payload.put("goalId", node.get("goalId").asText());
            }
            if (node.has("title")) payload.put("title", node.get("title").asText());
            if (node.has("description") && !node.get("description").isNull()) {
                payload.put("description", node.get("description").asText());
            }
            if (node.has("targetDate") && !node.get("targetDate").isNull()) {
                payload.put("targetDate", node.get("targetDate").asText());
            }

            if (node.has("categoryNames") && node.get("categoryNames").isArray()) {
                List<String> categoryIds = resolveCategoryIds(node.get("categoryNames"), context.categories());
                if (!categoryIds.isEmpty()) {
                    payload.put("categoryIds", categoryIds);
                }
            }

            String summary = buildSummary(action, payload);
            ActionProposal proposal = new ActionProposal(TaskType.GOAL_MANAGEMENT, payload, summary);

            return new TaskResult(conversational, proposal);
        } catch (Exception exception) {
            log.warn("Failed to parse goal management response, returning as plain text: {}", exception.getMessage());
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

    private String buildSummary(String action, Map<String, Object> payload) {
        String title = (String) payload.getOrDefault("title", "goal");
        return switch (action) {
            case "CREATE" -> "Create goal: " + title;
            case "UPDATE" -> "Update goal: " + title;
            case "PAUSE" -> "Pause goal: " + title;
            case "COMPLETE" -> "Complete goal: " + title;
            default -> action + " goal: " + title;
        };
    }
}

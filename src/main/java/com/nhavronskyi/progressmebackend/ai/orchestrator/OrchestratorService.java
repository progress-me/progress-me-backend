package com.nhavronskyi.progressmebackend.ai.orchestrator;

import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.category.Category;
import com.nhavronskyi.progressmebackend.chat.ChatMessage;
import com.nhavronskyi.progressmebackend.chat.ChatMessageRepository;
import com.nhavronskyi.progressmebackend.goal.Goal;
import com.nhavronskyi.progressmebackend.goal.GoalService;
import com.nhavronskyi.progressmebackend.goal.dto.CreateGoalRequest;
import com.nhavronskyi.progressmebackend.goal.dto.UpdateGoalRequest;
import com.nhavronskyi.progressmebackend.goal.dto.UpdateGoalStatusRequest;
import com.nhavronskyi.progressmebackend.log.LogEntryService;
import com.nhavronskyi.progressmebackend.log.dto.CreateLogEntryRequest;
import com.nhavronskyi.progressmebackend.log.dto.UpdateLogEntryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final IntentClassifier intentClassifier;
    private final TaskHandlerRegistry handlerRegistry;
    private final LogEntryService logEntryService;
    private final GoalService goalService;
    private final ChatMessageRepository chatMessageRepository;

    public TaskResult processMessage(UUID userId, String userMessage,
                                     List<ChatMessage> history, AiContext context) {
        ChatMessage pendingProposal = findPendingProposal(history);

        if (pendingProposal != null) {
            return handlePendingProposal(userId, userMessage, history, context, pendingProposal);
        }

        TaskType taskType = intentClassifier.classify(userMessage, history);
        log.info("Classified intent as {} for user={}", taskType, userId);

        TaskHandler handler = handlerRegistry.getHandler(taskType);
        return handler.handle(userId, userMessage, history, context);
    }

    private TaskResult handlePendingProposal(UUID userId, String userMessage,
                                              List<ChatMessage> history, AiContext context,
                                              ChatMessage proposalMessage) {
        IntentClassifier.ConfirmationResult confirmation =
                intentClassifier.classifyConfirmation(userMessage);

        log.info("Confirmation classified as {} for user={}", confirmation, userId);

        return switch (confirmation) {
            case CONFIRM -> executeProposal(userId, proposalMessage);
            case REJECT -> rejectProposal(proposalMessage);
            case EDIT -> {
                markProposalStatus(proposalMessage, ChatMessage.ActionStatus.REJECTED);
                TaskType taskType = TaskType.valueOf(proposalMessage.getActionType());
                TaskHandler handler = handlerRegistry.getHandler(taskType);
                yield handler.handleWithPriorProposal(
                        userId, userMessage, history, context, proposalMessage.getActionPayload());
            }
        };
    }

    private TaskResult executeProposal(UUID userId, ChatMessage proposalMessage) {
        try {
            String actionType = proposalMessage.getActionType();
            Map<String, Object> payload = proposalMessage.getActionPayload();

            String confirmationText = switch (TaskType.valueOf(actionType)) {
                case DAILY_NOTE -> executeDailyNote(userId, payload);
                case GOAL_MANAGEMENT -> executeGoalManagement(userId, payload);
                default -> "Action completed.";
            };

            markProposalStatus(proposalMessage, ChatMessage.ActionStatus.EXECUTED);
            return new TaskResult(confirmationText, null);
        } catch (Exception exception) {
            log.error("Failed to execute proposal for user={}: {}", userId, exception.getMessage(), exception);
            markProposalStatus(proposalMessage, ChatMessage.ActionStatus.REJECTED);
            return new TaskResult("Sorry, I couldn't complete that action: " + exception.getMessage(), null);
        }
    }

    private String executeDailyNote(UUID userId, Map<String, Object> payload) {
        String action = (String) payload.getOrDefault("action", "CREATE");

        if ("UPDATE".equals(action)) {
            String entryIdStr = (String) payload.get("entryId");
            UUID entryId = UUID.fromString(entryIdStr);
            UpdateLogEntryRequest request = new UpdateLogEntryRequest(
                    (String) payload.get("content"),
                    toInteger(payload.get("mood")),
                    toInteger(payload.get("energy")),
                    null,
                    null,
                    toUuidList(payload.get("categoryIds"))
            );
            logEntryService.update(userId, entryId, request);
            return "Done! I've updated your journal entry for today.";
        }

        CreateLogEntryRequest request = new CreateLogEntryRequest(
                (String) payload.getOrDefault("type", "journal"),
                (String) payload.get("content"),
                toInteger(payload.get("mood")),
                toInteger(payload.get("energy")),
                OffsetDateTime.now(),
                null,
                toUuidList(payload.get("categoryIds"))
        );
        logEntryService.create(userId, request);
        return "Done! I've created your journal entry for today.";
    }

    private String executeGoalManagement(UUID userId, Map<String, Object> payload) {
        String action = (String) payload.getOrDefault("action", "CREATE");

        return switch (action) {
            case "CREATE" -> {
                CreateGoalRequest request = new CreateGoalRequest(
                        (String) payload.get("title"),
                        (String) payload.get("description"),
                        toUuidList(payload.get("categoryIds")),
                        toLocalDate(payload.get("startDate")),
                        toLocalDate(payload.get("targetDate")),
                        null,
                        null
                );
                goalService.create(userId, request);
                yield "Done! I've created the goal: " + payload.get("title");
            }
            case "UPDATE" -> {
                UUID goalId = UUID.fromString((String) payload.get("goalId"));
                UpdateGoalRequest request = new UpdateGoalRequest(
                        (String) payload.get("title"),
                        (String) payload.get("description"),
                        toUuidList(payload.get("categoryIds")),
                        toLocalDate(payload.get("startDate")),
                        toLocalDate(payload.get("targetDate")),
                        null,
                        null
                );
                goalService.update(userId, goalId, request);
                yield "Done! I've updated the goal: " + payload.get("title");
            }
            case "PAUSE" -> {
                UUID goalId = UUID.fromString((String) payload.get("goalId"));
                goalService.updateStatus(userId, goalId, new UpdateGoalStatusRequest("paused"));
                yield "Done! I've paused the goal: " + payload.get("title");
            }
            case "COMPLETE" -> {
                UUID goalId = UUID.fromString((String) payload.get("goalId"));
                goalService.updateStatus(userId, goalId, new UpdateGoalStatusRequest("completed"));
                yield "Done! I've marked the goal as completed: " + payload.get("title");
            }
            default -> "Action completed.";
        };
    }

    private TaskResult rejectProposal(ChatMessage proposalMessage) {
        markProposalStatus(proposalMessage, ChatMessage.ActionStatus.REJECTED);
        return new TaskResult("Understood, I've discarded that. How else can I help you?", null);
    }

    private void markProposalStatus(ChatMessage proposalMessage, ChatMessage.ActionStatus status) {
        proposalMessage.setActionStatus(status.name());
        chatMessageRepository.save(proposalMessage);
    }

    private ChatMessage findPendingProposal(List<ChatMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage message = history.get(i);
            if (message.getRole() == ChatMessage.Role.ASSISTANT
                    && ChatMessage.ActionStatus.PROPOSED.name().equals(message.getActionStatus())) {
                return message;
            }
            if (message.getRole() == ChatMessage.Role.ASSISTANT) {
                break;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<UUID> toUuidList(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> UUID.fromString(item.toString()))
                    .toList();
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        return LocalDate.parse(value.toString());
    }
}

package com.nhavronskyi.progressmebackend.ai.orchestrator;

import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.chat.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TaskHandler {

    TaskType getTaskType();

    TaskResult handle(UUID userId, String userMessage, List<ChatMessage> history, AiContext context);

    default TaskResult handleWithPriorProposal(UUID userId, String userMessage,
                                                List<ChatMessage> history, AiContext context,
                                                Map<String, Object> priorPayload) {
        return handle(userId, userMessage, history, context);
    }

    default StreamingTaskResult handleStream(UUID userId, String userMessage,
                                              List<ChatMessage> history, AiContext context) {
        TaskResult result = handle(userId, userMessage, history, context);
        return new StreamingTaskResult(Flux.just(result.responseText()), text -> result);
    }

    default StreamingTaskResult handleStreamWithPriorProposal(UUID userId, String userMessage,
                                                               List<ChatMessage> history, AiContext context,
                                                               Map<String, Object> priorPayload) {
        TaskResult result = handleWithPriorProposal(userId, userMessage, history, context, priorPayload);
        return new StreamingTaskResult(Flux.just(result.responseText()), text -> result);
    }
}

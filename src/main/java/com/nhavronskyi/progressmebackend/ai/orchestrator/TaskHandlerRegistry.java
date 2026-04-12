package com.nhavronskyi.progressmebackend.ai.orchestrator;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class TaskHandlerRegistry {

    private final Map<TaskType, TaskHandler> handlers;

    public TaskHandlerRegistry(List<TaskHandler> handlerList) {
        this.handlers = new EnumMap<>(TaskType.class);
        for (TaskHandler handler : handlerList) {
            handlers.put(handler.getTaskType(), handler);
        }
    }

    public TaskHandler getHandler(TaskType taskType) {
        TaskHandler handler = handlers.get(taskType);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for task type: " + taskType);
        }
        return handler;
    }
}

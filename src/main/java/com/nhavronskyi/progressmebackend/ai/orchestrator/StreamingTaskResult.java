package com.nhavronskyi.progressmebackend.ai.orchestrator;

import reactor.core.publisher.Flux;

import java.util.function.Function;

public record StreamingTaskResult(
        Flux<String> tokenStream,
        Function<String, TaskResult> finalizer
) {
}

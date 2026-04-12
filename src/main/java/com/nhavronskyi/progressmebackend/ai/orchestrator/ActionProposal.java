package com.nhavronskyi.progressmebackend.ai.orchestrator;

import java.util.Map;

public record ActionProposal(
        TaskType taskType,
        Map<String, Object> payload,
        String humanReadableSummary
) {
}

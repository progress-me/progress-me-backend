package com.nhavronskyi.progressmebackend.ai.orchestrator;

public record TaskResult(
        String responseText,
        ActionProposal proposal
) {
}

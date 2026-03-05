package com.nhavronskyi.progressmebackend.ai.dto;

import com.nhavronskyi.progressmebackend.ai.PrebuiltPrompt;

public record PrebuiltPromptResponse(String id, String label, String query, String type) {

    public static PrebuiltPromptResponse from(PrebuiltPrompt prompt) {
        return new PrebuiltPromptResponse(
                prompt.name(),
                prompt.getLabel(),
                prompt.getQuery(),
                prompt.getInsightType()
        );
    }
}

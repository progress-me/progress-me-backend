package com.nhavronskyi.progressmebackend.ai.dto;

import com.nhavronskyi.progressmebackend.ai.AiSettings;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiSettingsResponse(
        UUID id,
        String tone,
        Integer strictness,
        List<String> focusAreas,
        List<String> ignoreAreas,
        String customInstructions,
        String language,
        OffsetDateTime updatedAt
) {
    public static AiSettingsResponse from(AiSettings settings) {
        return new AiSettingsResponse(
                settings.getId(),
                settings.getTone(),
                settings.getStrictness(),
                settings.getFocusAreas(),
                settings.getIgnoreAreas(),
                settings.getCustomInstructions(),
                settings.getLanguage(),
                settings.getUpdatedAt()
        );
    }
}

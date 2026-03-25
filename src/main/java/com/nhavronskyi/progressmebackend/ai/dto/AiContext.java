package com.nhavronskyi.progressmebackend.ai.dto;

import com.nhavronskyi.progressmebackend.ai.ResponseStyleResolver;
import com.nhavronskyi.progressmebackend.goal.Goal;
import com.nhavronskyi.progressmebackend.log.LogEntry;
import com.nhavronskyi.progressmebackend.persona.dto.PersonaResponse;
import com.nhavronskyi.progressmebackend.value.UserValue;

import java.util.List;

public record AiContext(
        PersonaResponse persona,
        AiSettingsSnapshot aiSettings,
        List<UserValue> values,
        List<Goal> activeGoals,
        List<LogEntry> recentEntries
) {

    public record AiSettingsSnapshot(
            ResponseStyleResolver.TONE_OVERLAY tone,
            int strictness,
            List<String> focusAreas,
            List<String> ignoreAreas,
            String customInstructions,
            String language
    ) {
    }
}

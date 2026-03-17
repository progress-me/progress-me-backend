package com.nhavronskyi.progressmebackend.ai.dto;

import com.nhavronskyi.progressmebackend.ai.ResponseStyleResolver;
import com.nhavronskyi.progressmebackend.goal.Goal;
import com.nhavronskyi.progressmebackend.log.LogEntry;
import com.nhavronskyi.progressmebackend.persona.Persona;
import com.nhavronskyi.progressmebackend.value.UserValue;

import java.util.List;

/**
 * Aggregated context for building AI prompts.
 * Contains everything the AI needs to know about the user.
 */
public record AiContext(
        Persona persona,
        AiSettingsSnapshot aiSettings,
        List<UserValue> values,
        List<Goal> activeGoals,
        List<LogEntry> recentEntries,
        ResponseStyleResolver.TYPE_STRUCTURE analysisType
) {

    /**
     * Snapshot of AI settings to avoid entity coupling.
     */
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

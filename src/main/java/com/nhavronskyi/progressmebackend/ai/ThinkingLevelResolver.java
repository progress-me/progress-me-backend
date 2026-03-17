package com.nhavronskyi.progressmebackend.ai;

import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ThinkingLevelResolver {

    private static final Map<ResponseStyleResolver.TYPE_STRUCTURE, GoogleGenAiThinkingLevel> LEVELS = Map.of(
            ResponseStyleResolver.TYPE_STRUCTURE.PATTERN, GoogleGenAiThinkingLevel.HIGH,
            ResponseStyleResolver.TYPE_STRUCTURE.PREDICTION, GoogleGenAiThinkingLevel.HIGH,
            ResponseStyleResolver.TYPE_STRUCTURE.WEEKLY_SUMMARY, GoogleGenAiThinkingLevel.HIGH,
            ResponseStyleResolver.TYPE_STRUCTURE.WARNING, GoogleGenAiThinkingLevel.HIGH,
            ResponseStyleResolver.TYPE_STRUCTURE.ANALYSIS, GoogleGenAiThinkingLevel.THINKING_LEVEL_UNSPECIFIED
    );

    public GoogleGenAiThinkingLevel resolve(ResponseStyleResolver.TYPE_STRUCTURE analysisType) {
        return LEVELS.getOrDefault(analysisType, GoogleGenAiThinkingLevel.THINKING_LEVEL_UNSPECIFIED);
    }
}

package com.nhavronskyi.progressmebackend.ai;

import com.nhavronskyi.progressmebackend.ai.ResponseStyleResolver.TYPE_STRUCTURE;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;

import static org.assertj.core.api.Assertions.assertThat;

class ThinkingLevelResolverTest {

    private final ThinkingLevelResolver resolver = new ThinkingLevelResolver();

    @ParameterizedTest
    @EnumSource(value = TYPE_STRUCTURE.class, names = {"PATTERN", "PREDICTION", "WEEKLY_SUMMARY", "WARNING"})
    void complexTypesResolveToHigh(TYPE_STRUCTURE type) {
        assertThat(resolver.resolve(type)).isEqualTo(GoogleGenAiThinkingLevel.HIGH);
    }

    @Test
    void analysisResolvesToUnspecified() {
        assertThat(resolver.resolve(TYPE_STRUCTURE.ANALYSIS))
                .isEqualTo(GoogleGenAiThinkingLevel.THINKING_LEVEL_UNSPECIFIED);
    }
}

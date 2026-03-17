package com.nhavronskyi.progressmebackend.ai;

import com.nhavronskyi.progressmebackend.ai.ResponseStyleResolver.TONE_OVERLAY;
import com.nhavronskyi.progressmebackend.ai.ResponseStyleResolver.TYPE_STRUCTURE;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseStyleResolverTest {

    private final ResponseStyleResolver resolver = new ResponseStyleResolver();

    @ParameterizedTest
    @EnumSource(TYPE_STRUCTURE.class)
    void allTypesProduceNonBlankFormatting(TYPE_STRUCTURE type) {
        ResponseStyle style = resolver.resolve(type, TONE_OVERLAY.BALANCED);
        assertThat(style.formattingInstructions()).isNotBlank();
    }

    @Test
    void analysisTypeHasTldrStructure() {
        ResponseStyle style = resolver.resolve(TYPE_STRUCTURE.ANALYSIS, TONE_OVERLAY.BALANCED);
        assertThat(style.formattingInstructions()).contains("TL;DR summary");
    }

    @Test
    void weeklySummaryHasMetricsTable() {
        ResponseStyle style = resolver.resolve(TYPE_STRUCTURE.WEEKLY_SUMMARY, TONE_OVERLAY.BALANCED);
        assertThat(style.formattingInstructions()).contains("metrics table");
        assertThat(style.formattingInstructions()).contains("week-over-week");
    }

    @Test
    void warningHasSeverityAndActions() {
        ResponseStyle style = resolver.resolve(TYPE_STRUCTURE.WARNING, TONE_OVERLAY.BALANCED);
        assertThat(style.formattingInstructions()).contains("severity level");
        assertThat(style.formattingInstructions()).contains("action items");
    }

    @Test
    void predictionHasTrajectory() {
        ResponseStyle style = resolver.resolve(TYPE_STRUCTURE.PREDICTION, TONE_OVERLAY.BALANCED);
        assertThat(style.formattingInstructions()).contains("current trajectory");
        assertThat(style.formattingInstructions()).contains("projected outcomes");
    }

    @Test
    void patternHasConfidenceIndicators() {
        ResponseStyle style = resolver.resolve(TYPE_STRUCTURE.PATTERN, TONE_OVERLAY.BALANCED);
        assertThat(style.formattingInstructions()).contains("confidence indicators");
        assertThat(style.formattingInstructions()).contains("correlations");
    }

    @Test
    void toneOverlayAppliesGentleStyle() {
        ResponseStyle style = resolver.resolve(TYPE_STRUCTURE.ANALYSIS, TONE_OVERLAY.GENTLE);
        assertThat(style.formattingInstructions()).contains("warm, narrative prose");
    }

    @Test
    void toneOverlayAppliesStrictStyle() {
        ResponseStyle style = resolver.resolve(TYPE_STRUCTURE.ANALYSIS, TONE_OVERLAY.STRICT);
        assertThat(style.formattingInstructions()).contains("no-nonsense language");
    }

    @Test
    void toneOverlayAppliesAnalyticalStyle() {
        ResponseStyle style = resolver.resolve(TYPE_STRUCTURE.ANALYSIS, TONE_OVERLAY.ANALYTICAL);
        assertThat(style.formattingInstructions()).contains("data tables and percentages");
    }

    @Test
    void differentTypesProduceDifferentOutput() {
        ResponseStyle analysis = resolver.resolve(TYPE_STRUCTURE.ANALYSIS, TONE_OVERLAY.BALANCED);
        ResponseStyle warning = resolver.resolve(TYPE_STRUCTURE.WARNING, TONE_OVERLAY.BALANCED);
        ResponseStyle pattern = resolver.resolve(TYPE_STRUCTURE.PATTERN, TONE_OVERLAY.BALANCED);

        assertThat(analysis.formattingInstructions())
                .isNotEqualTo(warning.formattingInstructions())
                .isNotEqualTo(pattern.formattingInstructions());
    }

    @Test
    void fromStringParsesValidTypes() {
        assertThat(TYPE_STRUCTURE.fromString("analysis")).isEqualTo(TYPE_STRUCTURE.ANALYSIS);
        assertThat(TYPE_STRUCTURE.fromString("weekly_summary")).isEqualTo(TYPE_STRUCTURE.WEEKLY_SUMMARY);
        assertThat(TYPE_STRUCTURE.fromString("warning")).isEqualTo(TYPE_STRUCTURE.WARNING);
        assertThat(TYPE_STRUCTURE.fromString("prediction")).isEqualTo(TYPE_STRUCTURE.PREDICTION);
        assertThat(TYPE_STRUCTURE.fromString("pattern")).isEqualTo(TYPE_STRUCTURE.PATTERN);
    }

    @Test
    void fromStringFallsBackForUnknownType() {
        assertThat(TYPE_STRUCTURE.fromString("unknown")).isEqualTo(TYPE_STRUCTURE.ANALYSIS);
        assertThat(TYPE_STRUCTURE.fromString(null)).isEqualTo(TYPE_STRUCTURE.ANALYSIS);
    }

    @Test
    void fromStringParsesValidTones() {
        assertThat(TONE_OVERLAY.fromString("gentle")).isEqualTo(TONE_OVERLAY.GENTLE);
        assertThat(TONE_OVERLAY.fromString("strict")).isEqualTo(TONE_OVERLAY.STRICT);
        assertThat(TONE_OVERLAY.fromString("analytical")).isEqualTo(TONE_OVERLAY.ANALYTICAL);
        assertThat(TONE_OVERLAY.fromString("balanced")).isEqualTo(TONE_OVERLAY.BALANCED);
    }

    @Test
    void fromStringFallsBackForUnknownTone() {
        assertThat(TONE_OVERLAY.fromString("unknown")).isEqualTo(TONE_OVERLAY.BALANCED);
        assertThat(TONE_OVERLAY.fromString(null)).isEqualTo(TONE_OVERLAY.BALANCED);
    }
}

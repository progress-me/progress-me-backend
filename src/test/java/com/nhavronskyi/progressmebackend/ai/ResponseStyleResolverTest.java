package com.nhavronskyi.progressmebackend.ai;

import com.nhavronskyi.progressmebackend.ai.ResponseStyleResolver.TONE_OVERLAY;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseStyleResolverTest {

    private final ResponseStyleResolver resolver = new ResponseStyleResolver();

    @ParameterizedTest
    @EnumSource(TONE_OVERLAY.class)
    void allTonesProduceNonBlankFormatting(TONE_OVERLAY tone) {
        ResponseStyle style = resolver.resolve(tone);
        assertThat(style.formattingInstructions()).isNotBlank();
    }

    @Test
    void toneOverlayAppliesGentleStyle() {
        ResponseStyle style = resolver.resolve(TONE_OVERLAY.GENTLE);
        assertThat(style.formattingInstructions()).contains("warm, narrative prose");
    }

    @Test
    void toneOverlayAppliesStrictStyle() {
        ResponseStyle style = resolver.resolve(TONE_OVERLAY.STRICT);
        assertThat(style.formattingInstructions()).contains("no-nonsense language");
    }

    @Test
    void toneOverlayAppliesAnalyticalStyle() {
        ResponseStyle style = resolver.resolve(TONE_OVERLAY.ANALYTICAL);
        assertThat(style.formattingInstructions()).contains("data tables and percentages");
    }

    @Test
    void toneOverlayAppliesBalancedStyle() {
        ResponseStyle style = resolver.resolve(TONE_OVERLAY.BALANCED);
        assertThat(style.formattingInstructions()).contains("Mix narrative sentences with bullet points");
    }

    @Test
    void differentTonesProduceDifferentOutput() {
        ResponseStyle gentle = resolver.resolve(TONE_OVERLAY.GENTLE);
        ResponseStyle strict = resolver.resolve(TONE_OVERLAY.STRICT);

        assertThat(gentle.formattingInstructions())
                .isNotEqualTo(strict.formattingInstructions());
    }

    @Test
    void formattingGuidanceIncluded() {
        ResponseStyle style = resolver.resolve(TONE_OVERLAY.BALANCED);
        assertThat(style.formattingInstructions()).contains("Vary your response structure naturally");
        assertThat(style.formattingInstructions()).contains("Adapt length and format");
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

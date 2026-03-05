package com.nhavronskyi.progressmebackend.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAiSettingsRequest(
        @Pattern(regexp = "strict|gentle|analytical|balanced", message = "Tone must be strict, gentle, analytical, or balanced")
        String tone,

        @Min(1) @Max(10)
        Integer strictness,

        List<String> focusAreas,

        List<String> ignoreAreas,

        @Size(max = 2000)
        String customInstructions,

        @Pattern(regexp = "[a-z]{2}", message = "Language must be a 2-letter code")
        String language
) {
}

package com.nhavronskyi.progressmebackend.persona.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdatePersonaRequest(
        @Size(max = 100) String displayName,
        @Min(1) @Max(150) Integer age,
        String bio,
        String strengths,
        String weaknesses,
        String interests,
        String context
) {
}

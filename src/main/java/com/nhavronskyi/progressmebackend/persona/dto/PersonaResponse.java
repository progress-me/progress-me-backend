package com.nhavronskyi.progressmebackend.persona.dto;

import com.nhavronskyi.progressmebackend.persona.Persona;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PersonaResponse(
        UUID id,
        UUID userId,
        String displayName,
        Integer age,
        String bio,
        String strengths,
        String weaknesses,
        String interests,
        String context,
        OffsetDateTime updatedAt
) {
    public static PersonaResponse from(Persona persona) {
        return new PersonaResponse(
                persona.getId(),
                persona.getUserId(),
                persona.getDisplayName(),
                persona.getAge(),
                persona.getBio(),
                persona.getStrengths(),
                persona.getWeaknesses(),
                persona.getInterests(),
                persona.getContext(),
                persona.getUpdatedAt()
        );
    }
}

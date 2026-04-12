package com.nhavronskyi.progressmebackend.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UpdateLogEntryRequest(
        @NotBlank String content,
        @Min(1) @Max(10) Integer mood,
        @Min(1) @Max(10) Integer energy,
        OffsetDateTime loggedAt,
        List<UUID> goalIds,
        List<UUID> categoryIds
) {
}

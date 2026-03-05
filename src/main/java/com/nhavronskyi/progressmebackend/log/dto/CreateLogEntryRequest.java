package com.nhavronskyi.progressmebackend.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateLogEntryRequest(
        @NotBlank @Pattern(regexp = "journal|micro_note|check_in", message = "type must be journal, micro_note, or check_in") String type,
        @NotBlank String content,
        @Min(1) @Max(10) Integer mood,
        @Min(1) @Max(10) Integer energy,
        OffsetDateTime loggedAt,
        List<UUID> goalIds,
        List<UUID> tagValueIds
) {
}

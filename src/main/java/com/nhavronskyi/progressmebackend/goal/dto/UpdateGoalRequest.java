package com.nhavronskyi.progressmebackend.goal.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateGoalRequest(
        @Size(max = 255) String title,
        String description,
        List<UUID> categoryIds,
        LocalDate startDate,
        LocalDate targetDate,
        String templateType,
        Map<String, String> templateData
) {
}

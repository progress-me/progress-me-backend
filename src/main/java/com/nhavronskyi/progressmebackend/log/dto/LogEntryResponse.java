package com.nhavronskyi.progressmebackend.log.dto;

import com.nhavronskyi.progressmebackend.log.LogEntry;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LogEntryResponse(
        UUID id,
        UUID userId,
        List<GoalInfo> goals,
        String type,
        String content,
        Integer mood,
        Integer energy,
        List<CategoryInfo> categories,
        OffsetDateTime loggedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record GoalInfo(UUID id, String title, boolean isDefault) {}

    public record CategoryInfo(UUID id, String name, String color) {}

    public static LogEntryResponse from(LogEntry entry, List<GoalInfo> goals, List<CategoryInfo> categories) {
        return new LogEntryResponse(
                entry.getId(),
                entry.getUserId(),
                goals,
                entry.getType(),
                entry.getContent(),
                entry.getMood(),
                entry.getEnergy(),
                categories,
                entry.getLoggedAt(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}

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
        List<TagResponse> tags,
        OffsetDateTime loggedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record GoalInfo(UUID id, String title, boolean isDefault) {}

    public static LogEntryResponse from(LogEntry entry, List<GoalInfo> goals, List<TagResponse> tags) {
        return new LogEntryResponse(
                entry.getId(),
                entry.getUserId(),
                goals,
                entry.getType(),
                entry.getContent(),
                entry.getMood(),
                entry.getEnergy(),
                tags,
                entry.getLoggedAt(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}

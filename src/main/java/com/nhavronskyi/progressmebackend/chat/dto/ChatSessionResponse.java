package com.nhavronskyi.progressmebackend.chat.dto;

import com.nhavronskyi.progressmebackend.chat.ChatSession;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatSessionResponse(
        UUID id,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}

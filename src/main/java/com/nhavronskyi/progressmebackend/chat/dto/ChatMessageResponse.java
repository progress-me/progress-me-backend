package com.nhavronskyi.progressmebackend.chat.dto;

import com.nhavronskyi.progressmebackend.chat.ChatMessage;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        ChatMessage.Role role,
        String content,
        String actionType,
        Map<String, Object> actionPayload,
        String actionStatus,
        OffsetDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getActionType(),
                message.getActionPayload(),
                message.getActionStatus(),
                message.getCreatedAt()
        );
    }
}

package com.nhavronskyi.progressmebackend.value.dto;

import com.nhavronskyi.progressmebackend.value.UserValue;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ValueResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        Integer sortOrder,
        OffsetDateTime createdAt
) {

    public static ValueResponse from(UserValue value) {
        return new ValueResponse(
                value.getId(),
                value.getCategoryId(),
                value.getName(),
                value.getDescription(),
                value.getSortOrder(),
                value.getCreatedAt()
        );
    }
}

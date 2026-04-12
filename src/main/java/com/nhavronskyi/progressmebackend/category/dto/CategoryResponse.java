package com.nhavronskyi.progressmebackend.category.dto;

import com.nhavronskyi.progressmebackend.category.Category;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        String color,
        String icon,
        Integer sortOrder,
        OffsetDateTime createdAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getColor(),
                category.getIcon(),
                category.getSortOrder(),
                category.getCreatedAt()
        );
    }
}

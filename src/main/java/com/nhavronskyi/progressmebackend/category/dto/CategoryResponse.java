package com.nhavronskyi.progressmebackend.category.dto;

import com.nhavronskyi.progressmebackend.category.Category;
import com.nhavronskyi.progressmebackend.value.dto.ValueResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        String color,
        String icon,
        Integer sortOrder,
        List<ValueResponse> values,
        OffsetDateTime createdAt
) {

    public static CategoryResponse from(Category category, List<ValueResponse> values) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getColor(),
                category.getIcon(),
                category.getSortOrder(),
                values,
                category.getCreatedAt()
        );
    }
}

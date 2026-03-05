package com.nhavronskyi.progressmebackend.category.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 100) String name,
        String description,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a hex color like #4A90D9") String color,
        @Size(max = 50) String icon
) {
}

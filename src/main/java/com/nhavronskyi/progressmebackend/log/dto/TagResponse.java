package com.nhavronskyi.progressmebackend.log.dto;

import java.util.UUID;

public record TagResponse(
        UUID id,
        String name,
        String categoryName,
        String categoryColor
) {
}

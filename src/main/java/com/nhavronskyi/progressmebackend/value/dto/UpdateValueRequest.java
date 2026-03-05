package com.nhavronskyi.progressmebackend.value.dto;

import jakarta.validation.constraints.Size;

public record UpdateValueRequest(
        @Size(max = 100) String name,
        String description
) {
}

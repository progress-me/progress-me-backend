package com.nhavronskyi.progressmebackend.value.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateValueRequest(
        @NotBlank @Size(max = 100) String name,
        String description
) {
}

package com.nhavronskyi.progressmebackend.insight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnalyzeRequest(
        @NotBlank @Size(max = 2000) String query,
        @Pattern(regexp = "analysis|warning|prediction|pattern|weekly_summary",
                message = "type must be one of: analysis, warning, prediction, pattern, weekly_summary")
        String type
) {
    public String resolvedType() {
        return type != null && !type.isBlank() ? type : "analysis";
    }
}

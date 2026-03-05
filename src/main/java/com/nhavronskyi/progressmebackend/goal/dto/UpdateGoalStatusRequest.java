package com.nhavronskyi.progressmebackend.goal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateGoalStatusRequest(
        @NotBlank @Pattern(regexp = "active|completed|paused|abandoned") String status
) {
}

package com.nhavronskyi.progressmebackend.goal.dto;

import com.nhavronskyi.progressmebackend.category.Category;
import com.nhavronskyi.progressmebackend.goal.Goal;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String title,
        String description,
        String status,
        boolean isDefault,
        List<CategoryInfo> categories,
        LocalDate startDate,
        LocalDate targetDate,
        String templateType,
        Map<String, String> templateData,
        long entryCount,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record CategoryInfo(UUID id, String name, String color) {
        public static CategoryInfo from(Category category) {
            return new CategoryInfo(category.getId(), category.getName(), category.getColor());
        }
    }

    public static GoalResponse from(Goal goal, long entryCount) {
        List<CategoryInfo> cats = goal.getCategories().stream()
                .map(CategoryInfo::from)
                .toList();

        return new GoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getStatus(),
                goal.isDefault(),
                cats,
                goal.getStartDate(),
                goal.getTargetDate(),
                goal.getTemplateType(),
                goal.getTemplateData(),
                entryCount,
                goal.getCompletedAt(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

}

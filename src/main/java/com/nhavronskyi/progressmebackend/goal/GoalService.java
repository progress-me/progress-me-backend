package com.nhavronskyi.progressmebackend.goal;

import com.nhavronskyi.progressmebackend.category.Category;
import com.nhavronskyi.progressmebackend.category.CategoryRepository;
import com.nhavronskyi.progressmebackend.common.ResourceNotFoundException;
import com.nhavronskyi.progressmebackend.cache.CacheEvictionService;
import com.nhavronskyi.progressmebackend.goal.dto.CreateGoalRequest;
import com.nhavronskyi.progressmebackend.goal.dto.GoalResponse;
import com.nhavronskyi.progressmebackend.goal.dto.UpdateGoalRequest;
import com.nhavronskyi.progressmebackend.goal.dto.UpdateGoalStatusRequest;
import com.nhavronskyi.progressmebackend.log.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final CategoryRepository categoryRepository;
    private final LogEntryRepository logEntryRepository;
    private final CacheEvictionService cacheEvictionService;

    @Transactional
    public GoalResponse ensureDefaultGoal(UUID userId) {
        return goalRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Goal goal = new Goal();
                    goal.setUserId(userId);
                    goal.setTitle("General");
                    goal.setDescription("Default goal for general entries");
                    goal.setDefault(true);
                    return toResponse(goalRepository.save(goal));
                });
    }

    @Transactional
    public GoalResponse create(UUID userId, CreateGoalRequest request) {
        if (request.targetDate() != null && request.startDate() != null
                && request.targetDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("targetDate must be >= startDate");
        }

        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setTitle(request.title());
        goal.setDescription(request.description());
        goal.setStartDate(request.startDate());
        goal.setTargetDate(request.targetDate());
        goal.setTemplateType(request.templateType());
        goal.setTemplateData(request.templateData());
        goal.setCategories(resolveCategories(userId, request.categoryIds()));

        GoalResponse response = toResponse(goalRepository.save(goal));
        cacheEvictionService.evictDashboard(userId);
        return response;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(UUID userId, String status, UUID categoryId) {
        Specification<Goal> spec = Specification.where(hasUserId(userId));
        if (status != null) {
            spec = spec.and(hasStatus(status));
        }
        if (categoryId != null) {
            spec = spec.and(hasCategoryId(categoryId));
        }
        List<Goal> goals = goalRepository.findAll(spec);
        if (goals.isEmpty()) return List.of();

        List<UUID> goalIds = goals.stream().map(Goal::getId).toList();
        Map<UUID, Long> entryCounts = logEntryRepository.countEntriesByGoalIds(goalIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        return goals.stream()
                .map(g -> GoalResponse.from(g, entryCounts.getOrDefault(g.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse get(UUID userId, UUID goalId) {
        Goal goal = findOwned(userId, goalId);
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse update(UUID userId, UUID goalId, UpdateGoalRequest request) {
        Goal goal = findOwned(userId, goalId);
        if (request.title() != null) {
            goal.setTitle(request.title());
        }
        if (request.description() != null) {
            goal.setDescription(request.description());
        }
        if (request.categoryIds() != null) {
            goal.setCategories(resolveCategories(userId, request.categoryIds()));
        }
        if (request.startDate() != null) {
            goal.setStartDate(request.startDate());
        }
        if (request.targetDate() != null) {
            goal.setTargetDate(request.targetDate());
        }
        if (request.templateType() != null) {
            goal.setTemplateType(request.templateType());
        }
        if (request.templateData() != null) {
            goal.setTemplateData(request.templateData());
        }
        GoalResponse response = toResponse(goalRepository.save(goal));
        cacheEvictionService.evictDashboard(userId);
        return response;
    }

    @Transactional
    public GoalResponse updateStatus(UUID userId, UUID goalId, UpdateGoalStatusRequest request) {
        Goal goal = findOwned(userId, goalId);
        String newStatus = request.status();

        if ("completed".equals(newStatus) && goal.getCompletedAt() == null) {
            goal.setCompletedAt(OffsetDateTime.now());
        } else if (!"completed".equals(newStatus)) {
            goal.setCompletedAt(null);
        }

        goal.setStatus(newStatus);
        GoalResponse response = toResponse(goalRepository.save(goal));
        cacheEvictionService.evictDashboard(userId);
        return response;
    }

    @Transactional
    public void delete(UUID userId, UUID goalId) {
        Goal goal = findOwned(userId, goalId);
        if (goal.isDefault()) {
            throw new IllegalArgumentException("Cannot delete the default goal");
        }
        goalRepository.delete(goal);
        cacheEvictionService.evictDashboard(userId);
    }

    private Set<Category> resolveCategories(UUID userId, List<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Category> categories = new HashSet<>();
        for (UUID catId : categoryIds) {
            Category cat = categoryRepository.findByIdAndUserId(catId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + catId));
            categories.add(cat);
        }
        return categories;
    }

    private Goal findOwned(UUID userId, UUID goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }

    private GoalResponse toResponse(Goal goal) {
        long entryCount = logEntryRepository.countEntriesByGoalId(goal.getId());
        return GoalResponse.from(goal, entryCount);
    }

    private static Specification<Goal> hasUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    private static Specification<Goal> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Goal> hasCategoryId(UUID categoryId) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.join("categories").get("id"), categoryId);
        };
    }
}

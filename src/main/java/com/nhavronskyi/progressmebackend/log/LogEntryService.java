package com.nhavronskyi.progressmebackend.log;

import com.nhavronskyi.progressmebackend.category.Category;
import com.nhavronskyi.progressmebackend.category.CategoryRepository;
import com.nhavronskyi.progressmebackend.common.ResourceNotFoundException;
import com.nhavronskyi.progressmebackend.cache.CacheEvictionService;
import com.nhavronskyi.progressmebackend.goal.Goal;
import com.nhavronskyi.progressmebackend.goal.GoalRepository;
import com.nhavronskyi.progressmebackend.log.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogEntryService {

    private final LogEntryRepository repository;
    private final GoalRepository goalRepository;
    private final CategoryRepository categoryRepository;
    private final CacheEvictionService cacheEvictionService;

    @Transactional
    public LogEntryResponse create(UUID userId, CreateLogEntryRequest request) {
        LogEntry entry = new LogEntry();
        entry.setUserId(userId);
        entry.setType(request.type());
        entry.setContent(request.content());
        entry.setMood(request.mood());
        entry.setEnergy(request.energy());
        if (request.loggedAt() != null) {
            entry.setLoggedAt(request.loggedAt());
        }

        if (request.goalIds() != null && !request.goalIds().isEmpty()) {
            Set<Goal> goals = resolveGoals(userId, request.goalIds());
            entry.setGoals(goals);
        }

        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            Set<Category> categories = resolveCategories(userId, request.categoryIds());
            entry.setCategories(categories);
        }

        LogEntry saved = repository.save(entry);
        cacheEvictionService.evictDashboard(userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<LogEntryResponse> list(UUID userId, String type, UUID goalId,
                                        OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Specification<LogEntry> spec = Specification.where(hasUserId(userId));
        if (type != null) {
            spec = spec.and(hasType(type));
        }
        if (goalId != null) {
            spec = spec.and(hasGoal(goalId));
        }
        if (from != null) {
            spec = spec.and(loggedAtAfter(from));
        }
        if (to != null) {
            spec = spec.and(loggedAtBefore(to));
        }
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "loggedAt"));
        }
        return repository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LogEntryResponse get(UUID userId, UUID entryId) {
        LogEntry entry = repository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry not found"));
        return toResponse(entry);
    }

    @Transactional
    public LogEntryResponse update(UUID userId, UUID entryId, UpdateLogEntryRequest request) {
        LogEntry entry = repository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry not found"));
        entry.setContent(request.content());
        if (request.mood() != null) {
            entry.setMood(request.mood());
        }
        if (request.energy() != null) {
            entry.setEnergy(request.energy());
        }
        if (request.loggedAt() != null) {
            entry.setLoggedAt(request.loggedAt());
        }
        if (request.goalIds() != null) {
            entry.setGoals(resolveGoals(userId, request.goalIds()));
        }
        if (request.categoryIds() != null) {
            entry.setCategories(resolveCategories(userId, request.categoryIds()));
        }
        LogEntryResponse response = toResponse(repository.save(entry));
        cacheEvictionService.evictDashboard(userId);
        return response;
    }

    @Transactional
    public LogEntryResponse updateCategories(UUID userId, UUID entryId, UpdateEntryCategoriesRequest request) {
        LogEntry entry = repository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry not found"));

        Set<Category> categories = resolveCategories(userId, request.categoryIds());
        entry.setCategories(categories);
        LogEntryResponse response = toResponse(repository.save(entry));
        cacheEvictionService.evictDashboard(userId);
        return response;
    }

    @Transactional
    public void delete(UUID userId, UUID entryId) {
        LogEntry entry = repository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry not found"));
        repository.delete(entry);
        cacheEvictionService.evictDashboard(userId);
    }

    private LogEntryResponse toResponse(LogEntry entry) {
        List<LogEntryResponse.GoalInfo> goalInfos = entry.getGoals().stream()
                .map(g -> new LogEntryResponse.GoalInfo(g.getId(), g.getTitle(), g.isDefault()))
                .toList();

        List<LogEntryResponse.CategoryInfo> categoryInfos = entry.getCategories().stream()
                .map(c -> new LogEntryResponse.CategoryInfo(c.getId(), c.getName(), c.getColor()))
                .toList();

        return LogEntryResponse.from(entry, goalInfos, categoryInfos);
    }

    private Set<Goal> resolveGoals(UUID userId, List<UUID> goalIds) {
        return goalIds.stream()
                .map(id -> goalRepository.findByIdAndUserId(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + id)))
                .collect(Collectors.toSet());
    }

    private Set<Category> resolveCategories(UUID userId, List<UUID> categoryIds) {
        return categoryIds.stream()
                .map(id -> categoryRepository.findByIdAndUserId(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id)))
                .collect(Collectors.toSet());
    }

    private static Specification<LogEntry> hasUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    private static Specification<LogEntry> hasType(String type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<LogEntry> hasGoal(UUID goalId) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.join("goals").get("id"), goalId);
        };
    }

    private static Specification<LogEntry> loggedAtAfter(OffsetDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("loggedAt"), from);
    }

    private static Specification<LogEntry> loggedAtBefore(OffsetDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("loggedAt"), to);
    }
}

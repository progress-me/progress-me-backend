package com.nhavronskyi.progressmebackend.log;

import com.nhavronskyi.progressmebackend.category.Category;
import com.nhavronskyi.progressmebackend.category.CategoryRepository;
import com.nhavronskyi.progressmebackend.common.ResourceNotFoundException;
import com.nhavronskyi.progressmebackend.goal.Goal;
import com.nhavronskyi.progressmebackend.goal.GoalRepository;
import com.nhavronskyi.progressmebackend.log.dto.*;
import com.nhavronskyi.progressmebackend.value.UserValue;
import com.nhavronskyi.progressmebackend.value.UserValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogEntryService {

    private final LogEntryRepository repository;
    private final UserValueRepository valueRepository;
    private final GoalRepository goalRepository;
    private final CategoryRepository categoryRepository;

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

        if (request.tagValueIds() != null && !request.tagValueIds().isEmpty()) {
            Set<UserValue> tags = resolveValues(userId, request.tagValueIds());
            entry.setTags(tags);
        }

        LogEntry saved = repository.save(entry);
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
        Page<LogEntry> page = repository.findAll(spec, pageable);
        List<LogEntry> entries = page.getContent();
        if (entries.isEmpty()) {
            return page.map(this::toResponse);
        }

        // Batch-load all category IDs referenced by tags across all entries
        // (@BatchSize on goals/tags handles their lazy loading in batch)
        Set<UUID> allCatIds = entries.stream()
                .flatMap(e -> e.getTags().stream())
                .map(UserValue::getCategoryId)
                .collect(Collectors.toSet());
        Map<UUID, Category> catById = categoryRepository.findAllById(allCatIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        return page.map(entry -> toResponseWithCategories(entry, catById));
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
        if (request.tagValueIds() != null) {
            entry.setTags(resolveValues(userId, request.tagValueIds()));
        }
        return toResponse(repository.save(entry));
    }

    @Transactional
    public LogEntryResponse updateTags(UUID userId, UUID entryId, UpdateEntryTagsRequest request) {
        LogEntry entry = repository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry not found"));

        Set<UserValue> tags = resolveValues(userId, request.valueIds());
        entry.setTags(tags);
        return toResponse(repository.save(entry));
    }

    @Transactional
    public void delete(UUID userId, UUID entryId) {
        LogEntry entry = repository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry not found"));
        repository.delete(entry);
    }

    private LogEntryResponse toResponse(LogEntry entry) {
        Set<UUID> catIds = entry.getTags().stream()
                .map(UserValue::getCategoryId)
                .collect(Collectors.toSet());
        Map<UUID, Category> catById = categoryRepository.findAllById(catIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        return toResponseWithCategories(entry, catById);
    }

    private LogEntryResponse toResponseWithCategories(LogEntry entry, Map<UUID, Category> catById) {
        List<LogEntryResponse.GoalInfo> goalInfos = entry.getGoals().stream()
                .map(g -> new LogEntryResponse.GoalInfo(g.getId(), g.getTitle(), g.isDefault()))
                .toList();

        List<TagResponse> tags = entry.getTags().stream()
                .map(v -> {
                    Category cat = catById.get(v.getCategoryId());
                    return new TagResponse(v.getId(), v.getName(),
                            cat != null ? cat.getName() : "",
                            cat != null ? cat.getColor() : null);
                })
                .toList();

        return LogEntryResponse.from(entry, goalInfos, tags);
    }

    private Set<Goal> resolveGoals(UUID userId, List<UUID> goalIds) {
        return goalIds.stream()
                .map(id -> goalRepository.findByIdAndUserId(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + id)))
                .collect(Collectors.toSet());
    }

    private Set<UserValue> resolveValues(UUID userId, List<UUID> valueIds) {
        return valueIds.stream()
                .map(id -> valueRepository.findByIdAndUserId(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Value not found: " + id)))
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

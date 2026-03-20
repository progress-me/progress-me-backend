package com.nhavronskyi.progressmebackend.category;

import com.nhavronskyi.progressmebackend.category.dto.CategoryResponse;
import com.nhavronskyi.progressmebackend.category.dto.CreateCategoryRequest;
import com.nhavronskyi.progressmebackend.category.dto.UpdateCategoryRequest;
import com.nhavronskyi.progressmebackend.common.ConflictException;
import com.nhavronskyi.progressmebackend.common.ResourceNotFoundException;
import com.nhavronskyi.progressmebackend.value.UserValueRepository;
import com.nhavronskyi.progressmebackend.value.dto.ValueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserValueRepository valueRepository;

    @CacheEvict(value = "categories", key = "#userId")
    @Transactional
    public CategoryResponse create(UUID userId, CreateCategoryRequest request) {
        // DB has UNIQUE (user_id, name), but give a nice error message
        categoryRepository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .filter(c -> c.getName().equalsIgnoreCase(request.name()))
                .findAny()
                .ifPresent(c -> {
                    throw new ConflictException("Category '" + request.name() + "' already exists");
                });

        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.name());
        category.setDescription(request.description());
        category.setColor(request.color());
        category.setIcon(request.icon());
        category.setSortOrder(categoryRepository.countByUserId(userId));

        return CategoryResponse.from(categoryRepository.save(category), List.of());
    }

    @Cacheable(value = "categories", key = "#userId")
    @Transactional(readOnly = true)
    public List<CategoryResponse> listForUser(UUID userId) {
        List<Category> categories = categoryRepository.findByUserIdOrderBySortOrderAsc(userId);
        if (categories.isEmpty()) return List.of();

        Map<UUID, List<ValueResponse>> valuesByCategory = valueRepository
                .findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(ValueResponse::from)
                .collect(Collectors.groupingBy(ValueResponse::categoryId));

        return categories.stream()
                .map(cat -> CategoryResponse.from(cat, valuesByCategory.getOrDefault(cat.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID userId, UUID categoryId) {
        Category category = findOwned(userId, categoryId);
        return CategoryResponse.from(category, valuesForCategory(categoryId));
    }

    @CacheEvict(value = "categories", key = "#userId")
    @Transactional
    public CategoryResponse update(UUID userId, UUID categoryId, UpdateCategoryRequest request) {
        Category category = findOwned(userId, categoryId);
        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.color() != null) {
            category.setColor(request.color());
        }
        if (request.icon() != null) {
            category.setIcon(request.icon());
        }
        return CategoryResponse.from(categoryRepository.save(category), valuesForCategory(categoryId));
    }

    @CacheEvict(value = "categories", key = "#userId")
    @Transactional
    public void delete(UUID userId, UUID categoryId) {
        Category category = findOwned(userId, categoryId);
        categoryRepository.delete(category);
    }

    private Category findOwned(UUID userId, UUID categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private List<ValueResponse> valuesForCategory(UUID categoryId) {
        return valueRepository.findByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(ValueResponse::from)
                .toList();
    }
}

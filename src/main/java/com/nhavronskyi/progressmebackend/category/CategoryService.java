package com.nhavronskyi.progressmebackend.category;

import com.nhavronskyi.progressmebackend.category.dto.CategoryResponse;
import com.nhavronskyi.progressmebackend.category.dto.CreateCategoryRequest;
import com.nhavronskyi.progressmebackend.category.dto.UpdateCategoryRequest;
import com.nhavronskyi.progressmebackend.common.ConflictException;
import com.nhavronskyi.progressmebackend.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @CacheEvict(value = "categories", key = "#userId")
    @Transactional
    public CategoryResponse create(UUID userId, CreateCategoryRequest request) {
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

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Cacheable(value = "categories", key = "#userId")
    @Transactional(readOnly = true)
    public List<CategoryResponse> listForUser(UUID userId) {
        return categoryRepository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID userId, UUID categoryId) {
        return CategoryResponse.from(findOwned(userId, categoryId));
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
        return CategoryResponse.from(categoryRepository.save(category));
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
}

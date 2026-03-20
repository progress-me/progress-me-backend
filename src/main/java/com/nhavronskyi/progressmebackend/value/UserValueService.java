package com.nhavronskyi.progressmebackend.value;

import com.nhavronskyi.progressmebackend.category.CategoryRepository;
import com.nhavronskyi.progressmebackend.common.ResourceNotFoundException;
import com.nhavronskyi.progressmebackend.value.dto.CreateValueRequest;
import com.nhavronskyi.progressmebackend.value.dto.UpdateValueRequest;
import com.nhavronskyi.progressmebackend.value.dto.ValueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserValueService {

    private final UserValueRepository valueRepository;
    private final CategoryRepository categoryRepository;

    @CacheEvict(value = "categories", key = "#userId")
    @Transactional
    public ValueResponse create(UUID userId, UUID categoryId, CreateValueRequest request) {
        categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        UserValue value = new UserValue();
        value.setUserId(userId);
        value.setCategoryId(categoryId);
        value.setName(request.name());
        value.setDescription(request.description());
        value.setSortOrder(valueRepository.countByCategoryId(categoryId));

        return ValueResponse.from(valueRepository.save(value));
    }

    @Transactional(readOnly = true)
    public List<ValueResponse> listByUser(UUID userId) {
        return valueRepository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(ValueResponse::from)
                .toList();
    }

    @CacheEvict(value = "categories", key = "#userId")
    @Transactional
    public ValueResponse update(UUID userId, UUID valueId, UpdateValueRequest request) {
        UserValue value = findOwned(userId, valueId);
        if (request.name() != null) {
            value.setName(request.name());
        }
        if (request.description() != null) {
            value.setDescription(request.description());
        }
        return ValueResponse.from(valueRepository.save(value));
    }

    @CacheEvict(value = "categories", key = "#userId")
    @Transactional
    public void delete(UUID userId, UUID valueId) {
        UserValue value = findOwned(userId, valueId);
        valueRepository.delete(value);
    }

    private UserValue findOwned(UUID userId, UUID valueId) {
        return valueRepository.findByIdAndUserId(valueId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Value not found"));
    }
}

package com.nhavronskyi.progressmebackend.value;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserValueRepository extends JpaRepository<UserValue, UUID> {

    List<UserValue> findByCategoryIdOrderBySortOrderAsc(UUID categoryId);

    List<UserValue> findByUserIdOrderBySortOrderAsc(UUID userId);

    Optional<UserValue> findByIdAndUserId(UUID id, UUID userId);

    int countByCategoryId(UUID categoryId);
}

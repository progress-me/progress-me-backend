package com.nhavronskyi.progressmebackend.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserIdOrderBySortOrderAsc(UUID userId);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    int countByUserId(UUID userId);
}

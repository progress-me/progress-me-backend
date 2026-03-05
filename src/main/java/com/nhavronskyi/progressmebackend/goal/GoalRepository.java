package com.nhavronskyi.progressmebackend.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID>, JpaSpecificationExecutor<Goal> {

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndStatus(UUID userId, String status);

    List<Goal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Goal> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    Optional<Goal> findByUserIdAndIsDefaultTrue(UUID userId);

    boolean existsByUserIdAndIsDefaultTrue(UUID userId);
}

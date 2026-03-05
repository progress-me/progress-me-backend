package com.nhavronskyi.progressmebackend.insight;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InsightRepository extends JpaRepository<Insight, UUID> {

    Page<Insight> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Insight> findByIdAndUserId(UUID id, UUID userId);
}

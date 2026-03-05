package com.nhavronskyi.progressmebackend.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ai_settings")
@Getter
@Setter
@NoArgsConstructor
public class AiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String tone = "balanced";

    @Column(nullable = false)
    private Integer strictness = 5;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "focus_areas", nullable = false, columnDefinition = "jsonb")
    private List<String> focusAreas = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ignore_areas", nullable = false, columnDefinition = "jsonb")
    private List<String> ignoreAreas = new ArrayList<>();

    @Column(name = "custom_instructions", columnDefinition = "TEXT")
    private String customInstructions;

    @Column(nullable = false, length = 10)
    private String language = "en";

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    void onCreate() {
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

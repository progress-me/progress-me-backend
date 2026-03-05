package com.nhavronskyi.progressmebackend.insight;

import com.nhavronskyi.progressmebackend.log.LogEntry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "insights")
@Getter
@Setter
@NoArgsConstructor
public class Insight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "insight_entries",
            joinColumns = @JoinColumn(name = "insight_id"),
            inverseJoinColumns = @JoinColumn(name = "entry_id")
    )
    @BatchSize(size = 50)
    private Set<LogEntry> sourceEntries = new HashSet<>();
}

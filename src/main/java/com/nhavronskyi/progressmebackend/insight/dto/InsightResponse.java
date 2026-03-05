package com.nhavronskyi.progressmebackend.insight.dto;

import com.nhavronskyi.progressmebackend.insight.Insight;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InsightResponse(
        UUID id,
        String type,
        String query,
        String content,
        String contextSummary,
        String model,
        Integer tokensUsed,
        OffsetDateTime createdAt
) {
    public static InsightResponse from(Insight insight) {
        return new InsightResponse(
                insight.getId(),
                insight.getType(),
                insight.getQuery(),
                insight.getContent(),
                insight.getContextSummary(),
                insight.getModel(),
                insight.getTokensUsed(),
                insight.getCreatedAt()
        );
    }
}

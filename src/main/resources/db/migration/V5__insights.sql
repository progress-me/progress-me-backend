-- V5: Insights and Insight-Entry links
-- AI-generated analyses, stored permanently

-- ============================================
-- INSIGHTS
-- ============================================
CREATE TABLE insights
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    type            VARCHAR(30) NOT NULL, -- 'analysis', 'warning', 'prediction', 'pattern', 'weekly_summary'
    query           TEXT,                 -- what the user asked (or auto-generated prompt)
    content         TEXT        NOT NULL, -- AI response in markdown
    context_summary TEXT,                 -- brief summary of what context was used
    model           VARCHAR(50) NOT NULL, -- 'gemini-2.0-flash', etc.
    tokens_used     INTEGER,              -- for cost tracking
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_insights_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_insights_type CHECK (type IN ('analysis', 'warning', 'prediction', 'pattern', 'weekly_summary'))
);

CREATE INDEX idx_insights_user ON insights (user_id, created_at DESC);
CREATE INDEX idx_insights_type ON insights (user_id, type);

-- ============================================
-- INSIGHT ENTRIES (join table)
-- ============================================
-- Links insights to the log entries that were used as context
CREATE TABLE insight_entries
(
    insight_id UUID NOT NULL,
    entry_id   UUID NOT NULL,

    PRIMARY KEY (insight_id, entry_id),
    CONSTRAINT fk_ie_insight FOREIGN KEY (insight_id) REFERENCES insights (id) ON DELETE CASCADE,
    CONSTRAINT fk_ie_entry FOREIGN KEY (entry_id) REFERENCES log_entries (id) ON DELETE CASCADE
);

CREATE INDEX idx_insight_entries_entry ON insight_entries (entry_id);

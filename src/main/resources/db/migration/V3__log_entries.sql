-- V3: Log Entries and Tags
-- The heart of the app: every progress update, journal entry, and quick note

-- ============================================
-- LOG ENTRIES
-- ============================================
CREATE TABLE log_entries
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    goal_id    UUID,                               -- optional link to a specific goal
    type       VARCHAR(20) NOT NULL,               -- 'journal', 'micro_note', 'check_in'
    content    TEXT        NOT NULL,               -- markdown-supported free text
    mood       INTEGER,                           -- 1-10 scale
    energy     INTEGER,                           -- 1-10 scale
    logged_at  TIMESTAMPTZ NOT NULL DEFAULT now(), -- when this actually happened
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), -- when it was recorded
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_entries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_entries_goal FOREIGN KEY (goal_id) REFERENCES goals (id) ON DELETE SET NULL,
    CONSTRAINT chk_entries_type CHECK (type IN ('journal', 'micro_note', 'check_in')),
    CONSTRAINT chk_entries_mood CHECK (mood IS NULL OR (mood >= 1 AND mood <= 10)),
    CONSTRAINT chk_entries_energy CHECK (energy IS NULL OR (energy >= 1 AND energy <= 10))
);

-- Primary query: user's timeline (most recent first)
CREATE INDEX idx_entries_timeline ON log_entries (user_id, logged_at DESC);

-- Filter by entry type
CREATE INDEX idx_entries_type ON log_entries (user_id, type);

-- Entries for a specific goal
CREATE INDEX idx_entries_goal ON log_entries (goal_id) WHERE goal_id IS NOT NULL;

-- For rule evaluation: recent entries with mood/energy
CREATE INDEX idx_entries_mood ON log_entries (user_id, logged_at DESC) WHERE mood IS NOT NULL;

-- Full-text search on entry content (simple text search for MVP, pgvector later)
CREATE INDEX idx_entries_content_search ON log_entries
    USING gin (to_tsvector('english', content));

-- ============================================
-- ENTRY TAGS
-- ============================================
-- Links log entries to values for categorization
-- Example: A gym entry tagged with "Discipline" and "Health"
CREATE TABLE entry_tags
(
    entry_id UUID NOT NULL,
    value_id UUID NOT NULL,

    PRIMARY KEY (entry_id, value_id),
    CONSTRAINT fk_et_entry FOREIGN KEY (entry_id) REFERENCES log_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_et_value FOREIGN KEY (value_id) REFERENCES user_values (id) ON DELETE CASCADE
);

CREATE INDEX idx_entry_tags_value ON entry_tags (value_id);

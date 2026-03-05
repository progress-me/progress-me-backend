-- V4: Rules and Rule Events
-- User-defined trigger rules that detect patterns and generate alerts

-- ============================================
-- RULES
-- ============================================
CREATE TABLE rules
(
    id                UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL,
    name              VARCHAR(100) NOT NULL,            -- "Skip gym 3 days"
    description       TEXT,
    condition         JSONB        NOT NULL,            -- structured condition definition
    action_type       VARCHAR(20)  NOT NULL,            -- 'warn', 'suggest', 'log', 'ai_analyze'
    action_message    TEXT,                             -- custom message when triggered
    active            BOOLEAN      NOT NULL DEFAULT true,
    cooldown_hours    INTEGER      NOT NULL DEFAULT 24, -- min hours between triggers
    last_triggered_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_rules_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_rules_action CHECK (action_type IN ('warn', 'suggest', 'log', 'ai_analyze')),
    CONSTRAINT chk_rules_cooldown CHECK (cooldown_hours >= 0)
);

-- Active rules for a user (evaluated on each log entry)
CREATE INDEX idx_rules_active ON rules (user_id) WHERE active = true;

-- ============================================
-- RULE EVENTS
-- ============================================
-- History of when rules were triggered
CREATE TABLE rule_events
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    rule_id      UUID        NOT NULL,
    entry_id     UUID, -- log entry that triggered this (nullable for scheduled rules)
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    message      TEXT, -- generated message at trigger time

    CONSTRAINT fk_re_rule FOREIGN KEY (rule_id) REFERENCES rules (id) ON DELETE CASCADE,
    CONSTRAINT fk_re_entry FOREIGN KEY (entry_id) REFERENCES log_entries (id) ON DELETE SET NULL
);

CREATE INDEX idx_rule_events_rule ON rule_events (rule_id, triggered_at DESC);
CREATE INDEX idx_rule_events_entry ON rule_events (entry_id) WHERE entry_id IS NOT NULL;

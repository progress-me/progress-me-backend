-- V1: Users, Personas, and AI Settings
-- Core identity and personalization tables

-- Enable UUID generation
CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- USERS
-- ============================================
CREATE TABLE users
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),                          -- NULL for OAuth-only users
    name          VARCHAR(100) NOT NULL,
    auth_provider VARCHAR(20)  NOT NULL DEFAULT 'local', -- 'local', 'google', 'github'
    provider_id   VARCHAR(255),                          -- external OAuth provider user ID
    avatar_url    VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_provider CHECK (auth_provider IN ('local', 'google', 'github')),
    CONSTRAINT chk_users_password CHECK (
        (auth_provider = 'local' AND password_hash IS NOT NULL)
            OR (auth_provider != 'local')
        )
);

CREATE INDEX idx_users_provider ON users (auth_provider, provider_id) WHERE provider_id IS NOT NULL;

-- ============================================
-- PERSONAS
-- ============================================
-- One-to-one: user's self-description for AI context
CREATE TABLE personas
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL,
    display_name VARCHAR(100), -- how AI should address the user
    age          INTEGER,
    bio          TEXT,         -- free-form self-description
    strengths    TEXT,
    weaknesses   TEXT,
    interests    TEXT,
    context      TEXT,         -- additional context (job, life situation, etc.)
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_personas_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_personas_user UNIQUE (user_id),
    CONSTRAINT chk_personas_age CHECK (age IS NULL OR (age >= 1 AND age <= 150))
);

-- ============================================
-- AI SETTINGS
-- ============================================
-- One-to-one: how the AI assistant behaves for this user
CREATE TABLE ai_settings
(
    id                  UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    tone                VARCHAR(30) NOT NULL DEFAULT 'balanced',  -- 'strict', 'gentle', 'analytical', 'balanced'
    strictness          INTEGER    NOT NULL DEFAULT 5,           -- 1-10 scale
    focus_areas         JSONB       NOT NULL DEFAULT '[]'::jsonb, -- ["fitness", "career"]
    ignore_areas        JSONB       NOT NULL DEFAULT '[]'::jsonb, -- ["sleep"]
    custom_instructions TEXT,                                     -- free-text additions to AI system prompt
    language            VARCHAR(10) NOT NULL DEFAULT 'en',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_ai_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_ai_settings_user UNIQUE (user_id),
    CONSTRAINT chk_ai_tone CHECK (tone IN ('strict', 'gentle', 'analytical', 'balanced')),
    CONSTRAINT chk_ai_strictness CHECK (strictness >= 1 AND strictness <= 10)
);

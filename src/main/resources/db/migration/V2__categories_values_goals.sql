-- V2: Categories, Values, and Goals
-- The user's personal framework: what they care about and what they're working toward

-- ============================================
-- CATEGORIES
-- ============================================
-- Top-level grouping for values and goals
-- Examples: "Health", "Career", "Relationships", "Personal Growth"
CREATE TABLE categories
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    color       VARCHAR(7),  -- hex color for UI: "#4A90D9"
    icon        VARCHAR(50), -- icon name or emoji
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_categories_name UNIQUE (user_id, name),
    CONSTRAINT chk_categories_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$'
)
    );

CREATE INDEX idx_categories_user ON categories (user_id, sort_order);

-- ============================================
-- USER_VALUES
-- ============================================
-- Personal values within a category
-- Named "user_values" to avoid collision with SQL reserved word "values"
-- Examples: "Discipline", "Consistency", "Work-life balance"
CREATE TABLE user_values
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    category_id UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT, -- what this value means to the user
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_values_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_values_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_values_name UNIQUE (user_id, category_id, name)
);

CREATE INDEX idx_user_values_category ON user_values (category_id, sort_order);
CREATE INDEX idx_user_values_user ON user_values (user_id);

-- ============================================
-- GOALS
-- ============================================
-- Concrete objectives linked to categories
-- Supports generic goals + optional SMART template via JSONB
CREATE TABLE goals
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    category_id   UUID,        -- optional link to a category
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'active',
    start_date    DATE,
    target_date   DATE,
    completed_at  TIMESTAMPTZ,
    template_type VARCHAR(20), -- 'smart', null, or future templates
    template_data JSONB,       -- template-specific fields
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_goals_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT chk_goals_status CHECK (status IN ('active', 'completed', 'paused', 'abandoned')),
    CONSTRAINT chk_goals_dates CHECK (target_date IS NULL OR start_date IS NULL OR target_date >= start_date)
);

CREATE INDEX idx_goals_user_status ON goals (user_id, status);
CREATE INDEX idx_goals_category ON goals (category_id) WHERE category_id IS NOT NULL;

-- ============================================
-- GOAL_VALUES (many-to-many)
-- ============================================
-- Links goals to the values they support
-- Example: "Run a marathon" supports values "Discipline" + "Health"
CREATE TABLE goal_values
(
    goal_id  UUID NOT NULL,
    value_id UUID NOT NULL,

    PRIMARY KEY (goal_id, value_id),
    CONSTRAINT fk_gv_goal FOREIGN KEY (goal_id) REFERENCES goals (id) ON DELETE CASCADE,
    CONSTRAINT fk_gv_value FOREIGN KEY (value_id) REFERENCES user_values (id) ON DELETE CASCADE
);

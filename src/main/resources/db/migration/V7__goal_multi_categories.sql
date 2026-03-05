-- V7: Support multiple categories per goal
-- Replace goals.category_id (single FK) with goal_categories join table (many-to-many)

CREATE TABLE goal_categories
(
    goal_id     UUID NOT NULL,
    category_id UUID NOT NULL,

    PRIMARY KEY (goal_id, category_id),
    CONSTRAINT fk_gc_goal FOREIGN KEY (goal_id) REFERENCES goals (id) ON DELETE CASCADE,
    CONSTRAINT fk_gc_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);

CREATE INDEX idx_goal_categories_category ON goal_categories (category_id);

-- Migrate existing category_id data
INSERT INTO goal_categories (goal_id, category_id)
SELECT id, category_id
FROM goals
WHERE category_id IS NOT NULL;

-- Drop the old single FK column
ALTER TABLE goals DROP CONSTRAINT IF EXISTS fk_goals_category;
DROP INDEX IF EXISTS idx_goals_category;
ALTER TABLE goals DROP COLUMN category_id;

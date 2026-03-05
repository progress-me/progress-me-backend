-- V6: Multi-goal entries + default goal support
-- Changes:
--   1. Add is_default column to goals
--   2. Create entry_goals join table (many-to-many: log_entries <-> goals)
--   3. Migrate existing goal_id data from log_entries into entry_goals
--   4. Drop goal_id column from log_entries

-- 1. Default goal flag
ALTER TABLE goals ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT false;
CREATE UNIQUE INDEX uq_goals_default_per_user ON goals (user_id) WHERE is_default = true;

-- 2. Entry-goals join table
CREATE TABLE entry_goals (
    entry_id UUID NOT NULL,
    goal_id  UUID NOT NULL,

    PRIMARY KEY (entry_id, goal_id),
    CONSTRAINT fk_eg_entry FOREIGN KEY (entry_id) REFERENCES log_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_eg_goal  FOREIGN KEY (goal_id)  REFERENCES goals(id) ON DELETE CASCADE
);

CREATE INDEX idx_entry_goals_goal ON entry_goals (goal_id);

-- 3. Migrate existing data
INSERT INTO entry_goals (entry_id, goal_id)
SELECT id, goal_id FROM log_entries WHERE goal_id IS NOT NULL;

-- 4. Drop old FK column
ALTER TABLE log_entries DROP CONSTRAINT IF EXISTS fk_entries_goal;
ALTER TABLE log_entries DROP COLUMN goal_id;

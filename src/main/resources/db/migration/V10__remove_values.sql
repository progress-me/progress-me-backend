-- Migrate entry_tags (value tags) → entry_categories (category tags)
CREATE TABLE entry_categories (
    entry_id UUID NOT NULL REFERENCES log_entries(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (entry_id, category_id)
);

INSERT INTO entry_categories (entry_id, category_id)
SELECT DISTINCT et.entry_id, uv.category_id
FROM entry_tags et
JOIN user_values uv ON uv.id = et.value_id;

-- Drop value-related tables
DROP TABLE IF EXISTS entry_tags;
DROP TABLE IF EXISTS goal_values;
DROP TABLE IF EXISTS user_values;

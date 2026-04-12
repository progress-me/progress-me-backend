ALTER TABLE chat_messages
    ADD COLUMN action_type VARCHAR(30),
    ADD COLUMN action_payload JSONB,
    ADD COLUMN action_status VARCHAR(20);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_messages_channel_created_at
    ON messages (channel_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_messages_channel_deleted_created_at
    ON messages (channel_id, deleted, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_messages_content_trgm
    ON messages USING gin (lower(content) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_messages_sender_username_trgm
    ON messages USING gin (lower(sender_username) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_messages_file_name_trgm
    ON messages USING gin (lower(file_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_created_at
    ON direct_messages (conversation_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_deleted_created_at
    ON direct_messages (conversation_id, deleted, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_direct_messages_content_trgm
    ON direct_messages USING gin (lower(content) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_direct_messages_sender_username_trgm
    ON direct_messages USING gin (lower(sender_username) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_direct_messages_file_name_trgm
    ON direct_messages USING gin (lower(file_name) gin_trgm_ops);

CREATE TABLE IF NOT EXISTS message_mentions (
    id VARCHAR(26) PRIMARY KEY,
    source_type VARCHAR(30) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    mentioned_user_id UUID NOT NULL,
    mention_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_message_mentions_source
ON message_mentions(source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_message_mentions_user
ON message_mentions(mentioned_user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_message_mentions_user_source
ON message_mentions(source_type, source_id, mentioned_user_id, mention_type);
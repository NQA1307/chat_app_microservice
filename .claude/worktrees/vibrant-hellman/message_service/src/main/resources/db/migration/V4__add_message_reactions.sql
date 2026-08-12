CREATE TABLE IF NOT EXISTS message_reactions (
    id UUID PRIMARY KEY,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    emoji VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reaction_user_message UNIQUE (source_type, source_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_message_reactions_source
ON message_reactions(source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_message_reactions_user
ON message_reactions(user_id);

ALTER TABLE messages
ADD COLUMN IF NOT EXISTS message_type VARCHAR(50) NOT NULL DEFAULT 'TEXT';

ALTER TABLE direct_messages
ADD COLUMN IF NOT EXISTS message_type VARCHAR(50) NOT NULL DEFAULT 'TEXT';

ALTER TABLE messages
ADD COLUMN IF NOT EXISTS metadata JSONB;

ALTER TABLE direct_messages
ADD COLUMN IF NOT EXISTS metadata JSONB;

CREATE INDEX IF NOT EXISTS idx_messages_message_type ON messages(message_type);
CREATE INDEX IF NOT EXISTS idx_direct_messages_message_type ON direct_messages(message_type);
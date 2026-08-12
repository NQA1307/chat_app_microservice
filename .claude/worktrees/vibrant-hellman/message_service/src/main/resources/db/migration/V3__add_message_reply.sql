ALTER TABLE messages
ADD COLUMN IF NOT EXISTS reply_to_message_id VARCHAR(26);

ALTER TABLE direct_messages
ADD COLUMN IF NOT EXISTS reply_to_message_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_messages_reply_to
ON messages(reply_to_message_id);

CREATE INDEX IF NOT EXISTS idx_direct_messages_reply_to
ON direct_messages(reply_to_message_id);
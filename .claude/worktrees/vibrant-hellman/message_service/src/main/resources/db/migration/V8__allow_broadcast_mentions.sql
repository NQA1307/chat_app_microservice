ALTER TABLE message_mentions
ALTER COLUMN mentioned_user_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_message_mentions_broadcast_source
ON message_mentions(source_type, source_id, mention_type)
WHERE mentioned_user_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_messages_channel_deleted_id
    ON messages (channel_id, deleted, id DESC);

CREATE INDEX IF NOT EXISTS idx_messages_unread_channel_id_sender
    ON messages (channel_id, deleted, id, sender_id);

CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_deleted_id
    ON direct_messages (conversation_id, deleted, id DESC);

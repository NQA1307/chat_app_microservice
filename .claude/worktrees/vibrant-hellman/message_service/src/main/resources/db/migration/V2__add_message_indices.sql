CREATE INDEX IF NOT EXISTS idx_messages_channel_created_at ON messages(channel_id, created_at);
CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_created_at ON direct_messages(conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_conversations_participants ON conversations(participant_id1, participant_id2);

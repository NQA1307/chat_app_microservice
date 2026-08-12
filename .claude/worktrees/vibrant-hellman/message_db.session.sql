ALTER TABLE messages ADD COLUMN IF NOT EXISTS reply_to_message_id VARCHAR(26);
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS reply_to_message_id VARCHAR(255);
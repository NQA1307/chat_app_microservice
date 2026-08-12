CREATE TABLE IF NOT EXISTS conversations (
    id BIGSERIAL PRIMARY KEY,
    participant_id1 UUID NOT NULL,
    participant_id2 UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_participants UNIQUE (participant_id1, participant_id2)
);

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(26) PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    sender_id UUID NOT NULL,
    sender_username VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    file_url TEXT,
    file_name VARCHAR(255),
    file_size BIGINT,
    content_type VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Đảm bảo an toàn khi nâng cấp bảng messages cũ
ALTER TABLE messages ADD COLUMN IF NOT EXISTS file_url TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS file_name VARCHAR(255);
ALTER TABLE messages ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS content_type VARCHAR(255);

-- Cập nhật kiểu dữ liệu cột file_url sang TEXT sau khi chắc chắn nó đã tồn tại
ALTER TABLE messages ALTER COLUMN file_url TYPE TEXT;

CREATE TABLE IF NOT EXISTS direct_messages (
    id VARCHAR(255) PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id UUID NOT NULL,
    sender_username VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    file_url TEXT,
    file_name VARCHAR(255),
    file_size BIGINT,
    content_type VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dm_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Đảm bảo an toàn khi nâng cấp bảng direct_messages cũ
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS file_url TEXT;
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS file_name VARCHAR(255);
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS content_type VARCHAR(255);

-- Cập nhật kiểu dữ liệu cột file_url sang TEXT sau khi chắc chắn nó đã tồn tại
ALTER TABLE direct_messages ALTER COLUMN file_url TYPE TEXT;

CREATE TABLE IF NOT EXISTS channel_read_states (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    last_read_message_id VARCHAR(255),
    last_read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_channel_user UNIQUE (channel_id, user_id)
);

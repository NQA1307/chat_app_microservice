CREATE TABLE IF NOT EXISTS voice_sessions (
    id UUID PRIMARY KEY,
    room_name VARCHAR(255) NOT NULL,
    server_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    started_by UUID NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS voice_session_participants (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    user_id UUID NOT NULL,
    username VARCHAR(255),
    device_id VARCHAR(100),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    CONSTRAINT fk_voice_participants_session
        FOREIGN KEY (session_id) REFERENCES voice_sessions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS voice_e2ee_key_envelopes (
    id UUID PRIMARY KEY,
    room_name VARCHAR(255) NOT NULL,
    channel_id BIGINT NOT NULL,
    sender_user_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    recipient_device_id VARCHAR(100) NOT NULL,
    key_version INTEGER NOT NULL,
    encrypted_key TEXT NOT NULL,
    iv TEXT NOT NULL,
    algorithm VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_voice_sessions_room_active
ON voice_sessions(room_name, ended_at);

CREATE INDEX IF NOT EXISTS idx_voice_sessions_channel_active
ON voice_sessions(channel_id, ended_at);

CREATE INDEX IF NOT EXISTS idx_voice_envelopes_recipient
ON voice_e2ee_key_envelopes(room_name, recipient_user_id, recipient_device_id, key_version DESC);

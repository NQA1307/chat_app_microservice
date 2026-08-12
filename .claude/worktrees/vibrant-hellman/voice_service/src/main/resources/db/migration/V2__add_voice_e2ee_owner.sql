ALTER TABLE voice_sessions
ADD COLUMN IF NOT EXISTS e2ee_key_owner_user_id UUID;

ALTER TABLE voice_sessions
ADD COLUMN IF NOT EXISTS current_key_version INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_voice_sessions_e2ee_owner
ON voice_sessions(e2ee_key_owner_user_id);
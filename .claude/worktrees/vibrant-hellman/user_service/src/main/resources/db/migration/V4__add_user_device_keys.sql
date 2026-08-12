CREATE TABLE IF NOT EXISTS user_device_keys (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    public_key TEXT NOT NULL,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'ECDH-P256',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    CONSTRAINT uq_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_user_device_keys_user_id
ON user_device_keys(user_id);

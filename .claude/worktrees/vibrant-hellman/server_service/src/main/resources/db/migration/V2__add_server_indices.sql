CREATE INDEX IF NOT EXISTS idx_server_members_server_user ON server_members(server_id, user_id);
CREATE INDEX IF NOT EXISTS idx_channels_server_id ON channels(server_id);
CREATE INDEX IF NOT EXISTS idx_server_mutes_server_user_expires ON server_mutes(server_id, user_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_server_created_at ON moderation_logs(server_id, created_at);

-- Unique index xử lý vấn đề PostgreSQL không so sánh giá trị NULL trong unique constraints
CREATE UNIQUE INDEX IF NOT EXISTS uq_server_mutes_server_scope 
ON server_mutes(server_id, user_id, type) 
WHERE channel_id IS NULL;

ALTER TABLE media_files ADD COLUMN IF NOT EXISTS check_sum VARCHAR(255);
ALTER TABLE media_files ADD COLUMN IF NOT EXISTS storage_provider VARCHAR(50);
ALTER TABLE media_files ADD COLUMN IF NOT EXISTS storage_key VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_media_owner_created_at 
ON media_files(owner_id, created_at);

CREATE INDEX IF NOT EXISTS idx_media_owner_checksum_size 
ON media_files(owner_id, check_sum, size);

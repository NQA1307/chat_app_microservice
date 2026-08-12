CREATE TABLE IF NOT EXISTS media_assets (
    id VARCHAR(255) PRIMARY KEY,
    check_sum VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_provider VARCHAR(50) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    public_url TEXT NOT NULL,
    resource_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_media_assets_checksum_size
ON media_assets(check_sum, size);

ALTER TABLE media_files
ADD COLUMN IF NOT EXISTS asset_id VARCHAR(255);

ALTER TABLE media_files
ALTER COLUMN public_url TYPE TEXT;

ALTER TABLE media_files
ALTER COLUMN storage_path TYPE TEXT;

ALTER TABLE media_files
ALTER COLUMN storage_key TYPE TEXT;

CREATE INDEX IF NOT EXISTS idx_media_files_asset_id
ON media_files(asset_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_media_files_asset'
          AND table_name = 'media_files'
    ) THEN
        ALTER TABLE media_files
        ADD CONSTRAINT fk_media_files_asset
        FOREIGN KEY (asset_id) REFERENCES media_assets(id);
    END IF;
END $$;

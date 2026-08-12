CREATE TABLE IF NOT EXISTS media_files (
    id VARCHAR(255) PRIMARY KEY,
    owner_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    public_url VARCHAR(255) NOT NULL,
    check_sum VARCHAR(255) NOT NULL,
    storage_provider VARCHAR(50) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

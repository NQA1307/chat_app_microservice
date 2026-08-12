ALTER TABLE user_device_keys ADD COLUMN IF NOT EXISTS push_token VARCHAR(255);
ALTER TABLE user_device_keys ALTER COLUMN public_key DROP NOT NULL;
ALTER TABLE user_device_keys ALTER COLUMN algorithm DROP NOT NULL;

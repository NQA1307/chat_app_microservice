CREATE TABLE notification_settings (
    user_id UUID PRIMARY KEY,
    dm_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    server_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    friend_request_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    expo_push_token VARCHAR(255)
);

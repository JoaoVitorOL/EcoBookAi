CREATE TABLE user_notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    notification_id VARCHAR(100) NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(512) NOT NULL,
    route VARCHAR(120) NOT NULL,
    request_id UUID NULL,
    material_id UUID NULL,
    payload_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uk_user_notification_user_notification_id
    ON user_notification (user_id, notification_id);

CREATE INDEX idx_user_notification_user_created_at
    ON user_notification (user_id, created_at DESC);

CREATE INDEX idx_user_notification_user_read_at
    ON user_notification (user_id, read_at);

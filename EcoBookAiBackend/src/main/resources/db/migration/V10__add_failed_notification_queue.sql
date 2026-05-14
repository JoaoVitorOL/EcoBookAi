CREATE TABLE IF NOT EXISTS failed_notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(512) NOT NULL,
    payload_data JSONB NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMP NOT NULL,
    last_attempt_at TIMESTAMP,
    delivered_at TIMESTAMP,
    permanently_failed_at TIMESTAMP,
    last_error VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_failed_notification_next_attempt
    ON failed_notification (next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_failed_notification_user_id
    ON failed_notification (user_id);

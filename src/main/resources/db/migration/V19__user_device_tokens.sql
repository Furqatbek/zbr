-- User device tokens for push notifications
CREATE TABLE IF NOT EXISTS user_device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(500) NOT NULL,
    device_type VARCHAR(20) DEFAULT 'UNKNOWN',
    device_name VARCHAR(100),
    app_version VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for efficient lookups
CREATE INDEX IF NOT EXISTS idx_device_tokens_user ON user_device_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_device_tokens_token ON user_device_tokens(device_token);
CREATE INDEX IF NOT EXISTS idx_device_tokens_active ON user_device_tokens(user_id, is_active) WHERE is_active = true;

-- Unique constraint to prevent duplicate tokens per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_device_tokens_user_token ON user_device_tokens(user_id, device_token);

COMMENT ON TABLE user_device_tokens IS 'Stores FCM/APNS device tokens for push notifications';
COMMENT ON COLUMN user_device_tokens.device_token IS 'FCM registration token or APNS device token';
COMMENT ON COLUMN user_device_tokens.device_type IS 'ANDROID, IOS, WEB, or UNKNOWN';

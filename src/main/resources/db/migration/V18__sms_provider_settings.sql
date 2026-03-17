-- SMS Provider Settings Table
-- Persists SMS provider configuration to survive application restarts

CREATE TABLE sms_provider_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for fast lookups
CREATE INDEX idx_sms_provider_settings_key ON sms_provider_settings(setting_key);

-- Trigger for updated_at
CREATE TRIGGER update_sms_provider_settings_updated_at
    BEFORE UPDATE ON sms_provider_settings
    FOR EACH ROW EXECUTE FUNCTION update_notifications_updated_at();

-- Insert default settings (these will be overwritten by admin configuration)
INSERT INTO sms_provider_settings (setting_key, setting_value, description) VALUES
    ('sms.enabled', 'true', 'Whether SMS sending is globally enabled'),
    ('sms.provider', 'ESKIZ', 'Active SMS provider (ESKIZ or DEVSMS)'),
    ('sms.fallback_enabled', 'false', 'Whether to use fallback provider on failure'),
    ('sms.fallback_provider', NULL, 'Fallback provider type'),
    ('devsms.enabled', 'false', 'Whether DevSMS provider is enabled'),
    ('devsms.base_url', 'https://devsms.uz/api', 'DevSMS API base URL'),
    ('devsms.token', NULL, 'DevSMS authentication token'),
    ('devsms.from', '4546', 'DevSMS sender ID'),
    ('devsms.callback_url', NULL, 'DevSMS callback URL for delivery reports'),
    ('eskiz.enabled', 'true', 'Whether Eskiz provider is enabled'),
    ('eskiz.base_url', 'https://notify.eskiz.uz/api', 'Eskiz API base URL'),
    ('eskiz.email', NULL, 'Eskiz account email'),
    ('eskiz.password', NULL, 'Eskiz account password (encrypted)'),
    ('eskiz.from', '4546', 'Eskiz sender ID'),
    ('eskiz.callback_url', NULL, 'Eskiz callback URL for delivery reports');

COMMENT ON TABLE sms_provider_settings IS 'Persisted SMS provider configuration settings';

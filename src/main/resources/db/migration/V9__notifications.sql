-- Notification System Tables
-- V9: Templates, preferences, and notification history

-- Drop existing tables if they exist (from previous migrations)
DO $$
BEGIN
    DROP TABLE IF EXISTS notification_batches CASCADE;
    DROP TABLE IF EXISTS notifications CASCADE;
    DROP TABLE IF EXISTS push_tokens CASCADE;
    DROP TABLE IF EXISTS notification_preferences CASCADE;
    DROP TABLE IF EXISTS notification_templates CASCADE;
    DROP TABLE IF EXISTS email_templates CASCADE;
    DROP TABLE IF EXISTS sms_templates CASCADE;
END $$;

-- Notification Templates
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    channel VARCHAR(30) NOT NULL CHECK (channel IN ('PUSH', 'EMAIL', 'SMS', 'IN_APP')),
    subject VARCHAR(500),
    body TEXT NOT NULL,
    body_html TEXT,
    variables TEXT[],
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_templates_code ON notification_templates(template_code);
CREATE INDEX idx_notification_templates_channel ON notification_templates(channel);
CREATE INDEX idx_notification_templates_active ON notification_templates(is_active);

-- User Notification Preferences
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, channel, notification_type)
);

CREATE INDEX idx_notification_prefs_user ON notification_preferences(user_id);

-- Push Notification Tokens
CREATE TABLE push_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('IOS', 'ANDROID', 'WEB')),
    device_id VARCHAR(255),
    device_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_push_tokens_user ON push_tokens(user_id);
CREATE INDEX idx_push_tokens_token ON push_tokens(token);
CREATE INDEX idx_push_tokens_active ON push_tokens(is_active);

-- Notifications (sent notifications log)
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    template_id BIGINT REFERENCES notification_templates(id),
    channel VARCHAR(30) NOT NULL,
    notification_type VARCHAR(50),
    title VARCHAR(500),
    body TEXT,
    data JSONB,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    external_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_channel ON notifications(channel);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read_at);

-- Notification Batches (for bulk sends)
CREATE TABLE notification_batches (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT REFERENCES notification_templates(id),
    name VARCHAR(255),
    description TEXT,
    target_segment VARCHAR(100),
    target_count INTEGER DEFAULT 0,
    sent_count INTEGER DEFAULT 0,
    delivered_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_batches_status ON notification_batches(status);
CREATE INDEX idx_notification_batches_scheduled ON notification_batches(scheduled_at);

-- Email Templates (extended templates for rich email)
CREATE TABLE email_templates (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    from_name VARCHAR(255),
    from_email VARCHAR(255),
    reply_to VARCHAR(255),
    body_text TEXT,
    body_html TEXT NOT NULL,
    variables TEXT[],
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_templates_code ON email_templates(template_code);
CREATE INDEX idx_email_templates_active ON email_templates(is_active);

-- SMS Templates
CREATE TABLE sms_templates (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    body VARCHAR(160) NOT NULL,
    variables TEXT[],
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sms_templates_code ON sms_templates(template_code);

-- Insert default notification templates
INSERT INTO notification_templates (template_code, name, channel, subject, body, variables) VALUES
    ('ORDER_CONFIRMED', 'Order Confirmed', 'PUSH', 'Order Confirmed', 'Your order #{orderNumber} has been confirmed!', ARRAY['orderNumber', 'restaurantName']),
    ('ORDER_PREPARING', 'Order Preparing', 'PUSH', 'Order Being Prepared', 'Your order is being prepared by {restaurantName}', ARRAY['orderNumber', 'restaurantName']),
    ('ORDER_READY', 'Order Ready', 'PUSH', 'Order Ready for Pickup', 'Your order #{orderNumber} is ready!', ARRAY['orderNumber']),
    ('ORDER_PICKED_UP', 'Order Picked Up', 'PUSH', 'Order Picked Up', 'Your order is on the way! ETA: {eta}', ARRAY['orderNumber', 'courierName', 'eta']),
    ('ORDER_DELIVERED', 'Order Delivered', 'PUSH', 'Order Delivered', 'Your order #{orderNumber} has been delivered. Enjoy!', ARRAY['orderNumber']),
    ('ORDER_CANCELLED', 'Order Cancelled', 'PUSH', 'Order Cancelled', 'Your order #{orderNumber} has been cancelled.', ARRAY['orderNumber', 'reason']),
    ('PROMOTION_NEW', 'New Promotion', 'PUSH', 'New Deal Available', '{promotionTitle} - {discount}% off!', ARRAY['promotionTitle', 'discount', 'expiresAt']),
    ('COURIER_ASSIGNED', 'Courier Assigned', 'PUSH', 'Courier on the way', '{courierName} is picking up your order', ARRAY['courierName', 'eta']);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_notifications_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_notification_templates_updated_at
    BEFORE UPDATE ON notification_templates
    FOR EACH ROW EXECUTE FUNCTION update_notifications_updated_at();

CREATE TRIGGER update_notification_prefs_updated_at
    BEFORE UPDATE ON notification_preferences
    FOR EACH ROW EXECUTE FUNCTION update_notifications_updated_at();

CREATE TRIGGER update_push_tokens_updated_at
    BEFORE UPDATE ON push_tokens
    FOR EACH ROW EXECUTE FUNCTION update_notifications_updated_at();

CREATE TRIGGER update_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION update_notifications_updated_at();

CREATE TRIGGER update_notification_batches_updated_at
    BEFORE UPDATE ON notification_batches
    FOR EACH ROW EXECUTE FUNCTION update_notifications_updated_at();

CREATE TRIGGER update_email_templates_updated_at
    BEFORE UPDATE ON email_templates
    FOR EACH ROW EXECUTE FUNCTION update_notifications_updated_at();

CREATE TRIGGER update_sms_templates_updated_at
    BEFORE UPDATE ON sms_templates
    FOR EACH ROW EXECUTE FUNCTION update_notifications_updated_at();

COMMENT ON TABLE notification_templates IS 'Notification message templates';
COMMENT ON TABLE notification_preferences IS 'User notification preferences per channel and type';
COMMENT ON TABLE push_tokens IS 'Device push notification tokens';
COMMENT ON TABLE notifications IS 'Sent notifications history';
COMMENT ON TABLE notification_batches IS 'Bulk notification campaigns';
COMMENT ON TABLE email_templates IS 'Rich HTML email templates';
COMMENT ON TABLE sms_templates IS 'SMS message templates';

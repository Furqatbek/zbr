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
END $$;

-- Notification Templates (matching NotificationTemplate entity)
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    title_template VARCHAR(255) NOT NULL,
    message_template TEXT NOT NULL,
    icon VARCHAR(100),
    action_url_template VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    default_ttl_hours INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(notification_type, role)
);

CREATE INDEX idx_notification_templates_type ON notification_templates(notification_type);
CREATE INDEX idx_notification_templates_role ON notification_templates(role);

-- Notifications (matching Notification entity)
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    role VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    notification_type VARCHAR(100) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    order_id BIGINT,
    related_entity_id BIGINT,
    related_entity_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    expires_at TIMESTAMP,
    metadata JSONB,
    action_url VARCHAR(500),
    icon VARCHAR(100),
    dismissed BOOLEAN DEFAULT FALSE,
    dismissed_at TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_role ON notifications(role);
CREATE INDEX idx_notifications_read_at ON notifications(read_at);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_category ON notifications(category);
CREATE INDEX idx_notifications_order_id ON notifications(order_id);
CREATE INDEX idx_notifications_user_role_read ON notifications(user_id, role, read_at);
CREATE INDEX idx_notifications_expires_at ON notifications(expires_at);

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

COMMENT ON TABLE notification_templates IS 'Notification message templates';
COMMENT ON TABLE notifications IS 'User notifications';

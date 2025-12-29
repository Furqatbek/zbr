-- =====================================================
-- V20: Create Notification System Tables
-- =====================================================
-- Description: Creates tables for persistent notification management
-- Features:
--   - Multi-role notifications (customer, courier, restaurant, admin, etc.)
--   - Order lifecycle notifications
--   - Read/unread status tracking
--   - Soft delete (dismiss) support
--   - TTL and expiration support
--   - Notification templates
-- =====================================================

-- Drop existing tables to handle schema changes from partial runs
DROP TABLE IF EXISTS notification_read_status CASCADE;
DROP TABLE IF EXISTS notification_preferences CASCADE;
DROP TABLE IF EXISTS notification_templates CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;

-- Drop triggers if they exist
DROP TRIGGER IF EXISTS trg_notifications_updated_at ON notifications;
DROP TRIGGER IF EXISTS trg_notification_templates_updated_at ON notification_templates;
DROP TRIGGER IF EXISTS trg_notification_preferences_updated_at ON notification_preferences;
DROP FUNCTION IF EXISTS update_notification_updated_at();

-- Create enum types for notification system
DO $$
BEGIN
    -- Notification Role enum
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_role') THEN
        CREATE TYPE notification_role AS ENUM (
            'CUSTOMER', 'COURIER', 'RESTAURANT', 'ADMIN',
            'SUPPORT', 'FINANCE', 'OPERATIONS', 'ALL'
        );
    END IF;

    -- Notification Category enum
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_category') THEN
        CREATE TYPE notification_category AS ENUM (
            'ORDER', 'FINANCE', 'SUPPORT', 'SYSTEM', 'PROMOTION',
            'ACCOUNT', 'DELIVERY', 'RESTAURANT_OPS', 'ALERT'
        );
    END IF;

    -- Notification Type enum
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_type') THEN
        CREATE TYPE notification_type AS ENUM (
            'ORDER_CREATED', 'ORDER_ACCEPTED', 'ORDER_REJECTED', 'ORDER_PREPARING',
            'ORDER_READY', 'ORDER_PICKED_UP', 'ORDER_IN_TRANSIT', 'ORDER_DELIVERED',
            'ORDER_CANCELLED', 'ORDER_COMPLETED', 'ORDER_DELAYED', 'NEW_ORDER',
            'ORDER_ASSIGNED', 'COURIER_ASSIGNED', 'COURIER_ARRIVED', 'COURIER_NEARBY',
            'DELIVERY_COMPLETED', 'DELIVERY_FAILED', 'PAYMENT_RECEIVED', 'PAYMENT_FAILED',
            'PAYMENT_PENDING', 'REFUND_REQUESTED', 'REFUND_PROCESSED', 'REFUND_FAILED',
            'PAYOUT_ISSUED', 'PAYOUT_PENDING', 'INVOICE_AVAILABLE', 'TICKET_CREATED',
            'TICKET_UPDATED', 'TICKET_ASSIGNED', 'TICKET_RESOLVED', 'TICKET_CLOSED',
            'ACCOUNT_VERIFIED', 'ACCOUNT_SUSPENDED', 'ACCOUNT_REACTIVATED',
            'PASSWORD_CHANGED', 'PROFILE_UPDATED', 'DOCUMENTS_VERIFIED',
            'DOCUMENTS_REJECTED', 'PROMO_CODE_AVAILABLE', 'PROMO_EXPIRING',
            'LOYALTY_POINTS_EARNED', 'LOYALTY_REWARD_AVAILABLE', 'SYSTEM_MAINTENANCE',
            'SYSTEM_UPDATE', 'POLICY_UPDATE', 'FEATURE_ANNOUNCEMENT', 'RATING_RECEIVED',
            'REVIEW_RECEIVED', 'REVIEW_RESPONSE', 'HIGH_DEMAND_ALERT', 'LOW_INVENTORY_ALERT',
            'PEAK_HOURS_ALERT', 'FRAUD_ALERT', 'SECURITY_ALERT', 'EMERGENCY_ALERT',
            'RESTAURANT_OFFLINE', 'RESTAURANT_ONLINE', 'MENU_UPDATE_REQUIRED',
            'HOURS_UPDATE_REQUIRED', 'PERFORMANCE_WARNING', 'PERFORMANCE_IMPROVEMENT',
            'CUSTOM'
        );
    END IF;

    -- Notification Priority enum
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_priority') THEN
        CREATE TYPE notification_priority AS ENUM (
            'LOW', 'NORMAL', 'HIGH', 'URGENT'
        );
    END IF;
END$$;

-- =====================================================
-- Notifications Table
-- =====================================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,

    -- Target information
    user_id BIGINT,
    role VARCHAR(50) NOT NULL,

    -- Content
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    short_message VARCHAR(500),

    -- Classification
    category VARCHAR(50) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',

    -- Related entities
    order_id BIGINT,
    related_entity_id BIGINT,
    related_entity_type VARCHAR(100),

    -- Action URL
    action_url VARCHAR(500),

    -- Metadata (JSONB for flexible additional data)
    metadata JSONB DEFAULT '{}',

    -- Status flags
    read_at TIMESTAMP,
    dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    dismissed_at TIMESTAMP,

    -- TTL and expiration
    expires_at TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_notification_user_or_role CHECK (
        user_id IS NOT NULL OR role = 'ALL'
    )
);

-- =====================================================
-- Notification Templates Table
-- =====================================================
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,

    -- Template identification
    template_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Template content (supports placeholders like {orderNumber})
    title_template VARCHAR(255) NOT NULL,
    message_template TEXT NOT NULL,
    short_message_template VARCHAR(500),

    -- Classification
    category VARCHAR(50) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',

    -- Action URL template
    action_url_template VARCHAR(500),

    -- Template settings
    active BOOLEAN NOT NULL DEFAULT TRUE,
    ttl_hours INTEGER,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- =====================================================
-- Notification Preferences Table (per-user preferences)
-- =====================================================
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,

    -- Per-category preferences (enabled/disabled)
    order_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    finance_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    support_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    system_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    promotion_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    account_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    delivery_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    alert_notifications BOOLEAN NOT NULL DEFAULT TRUE,

    -- Channel preferences
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    -- Quiet hours
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint per user per role
    CONSTRAINT uk_notification_preferences_user_role UNIQUE (user_id, role)
);

-- =====================================================
-- Notification Read Status Table (for broadcast notifications)
-- =====================================================
CREATE TABLE notification_read_status (
    id BIGSERIAL PRIMARY KEY,

    notification_id BIGINT NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,

    -- Status
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    dismissed_at TIMESTAMP,

    -- Unique constraint
    CONSTRAINT uk_notification_read_status UNIQUE (notification_id, user_id)
);

-- =====================================================
-- Indexes for Performance
-- =====================================================

-- Notifications table indexes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_role ON notifications(role);
CREATE INDEX idx_notifications_user_role ON notifications(user_id, role);
CREATE INDEX idx_notifications_category ON notifications(category);
CREATE INDEX idx_notifications_type ON notifications(notification_type);
CREATE INDEX idx_notifications_priority ON notifications(priority);
CREATE INDEX idx_notifications_order_id ON notifications(order_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_read_at ON notifications(read_at);
CREATE INDEX idx_notifications_dismissed ON notifications(dismissed);
CREATE INDEX idx_notifications_expires_at ON notifications(expires_at);

-- Composite indexes for common queries
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, read_at)
    WHERE read_at IS NULL AND dismissed = FALSE;
CREATE INDEX idx_notifications_user_role_unread ON notifications(user_id, role, read_at)
    WHERE read_at IS NULL AND dismissed = FALSE;
CREATE INDEX idx_notifications_user_category ON notifications(user_id, category, created_at DESC);
CREATE INDEX idx_notifications_cleanup ON notifications(created_at, read_at, dismissed)
    WHERE expires_at IS NOT NULL OR dismissed = TRUE;

-- Notification templates indexes
CREATE INDEX idx_notification_templates_code ON notification_templates(template_code);
CREATE INDEX idx_notification_templates_type ON notification_templates(notification_type);
CREATE INDEX idx_notification_templates_role ON notification_templates(role);
CREATE INDEX idx_notification_templates_active ON notification_templates(active)
    WHERE active = TRUE;

-- Notification preferences indexes
CREATE INDEX idx_notification_preferences_user ON notification_preferences(user_id);
CREATE INDEX idx_notification_preferences_user_role ON notification_preferences(user_id, role);

-- Notification read status indexes
CREATE INDEX idx_notification_read_status_notification ON notification_read_status(notification_id);
CREATE INDEX idx_notification_read_status_user ON notification_read_status(user_id);

-- =====================================================
-- Trigger for updated_at timestamp
-- =====================================================
CREATE OR REPLACE FUNCTION update_notification_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Notifications trigger
DROP TRIGGER IF EXISTS trg_notifications_updated_at ON notifications;
CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_notification_updated_at();

-- Templates trigger
DROP TRIGGER IF EXISTS trg_notification_templates_updated_at ON notification_templates;
CREATE TRIGGER trg_notification_templates_updated_at
    BEFORE UPDATE ON notification_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_notification_updated_at();

-- Preferences trigger
DROP TRIGGER IF EXISTS trg_notification_preferences_updated_at ON notification_preferences;
CREATE TRIGGER trg_notification_preferences_updated_at
    BEFORE UPDATE ON notification_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_notification_updated_at();

-- =====================================================
-- Seed default notification templates
-- =====================================================
INSERT INTO notification_templates (template_code, name, title_template, message_template, category, notification_type, role, priority, action_url_template, ttl_hours)
VALUES
    -- Customer Order Templates
    ('ORDER_CREATED_CUSTOMER', 'Order Created - Customer', 'Order #{orderNumber} Placed!',
     'Your order from {restaurantName} has been placed successfully. Total: {totalAmount}',
     'ORDER', 'ORDER_CREATED', 'CUSTOMER', 'HIGH', '/orders/{orderId}', 168),

    ('ORDER_ACCEPTED_CUSTOMER', 'Order Accepted - Customer', 'Order #{orderNumber} Confirmed!',
     '{restaurantName} has accepted your order and is preparing it now.',
     'ORDER', 'ORDER_ACCEPTED', 'CUSTOMER', 'HIGH', '/orders/{orderId}', 168),

    ('ORDER_PREPARING_CUSTOMER', 'Order Preparing - Customer', 'Your Order is Being Prepared',
     '{restaurantName} is now preparing your order #{orderNumber}.',
     'ORDER', 'ORDER_PREPARING', 'CUSTOMER', 'NORMAL', '/orders/{orderId}', 168),

    ('ORDER_READY_CUSTOMER', 'Order Ready - Customer', 'Order #{orderNumber} Ready!',
     'Your order is ready and waiting for pickup by a courier.',
     'ORDER', 'ORDER_READY', 'CUSTOMER', 'HIGH', '/orders/{orderId}', 168),

    ('ORDER_PICKED_UP_CUSTOMER', 'Order Picked Up - Customer', 'Your Order is On Its Way!',
     'A courier has picked up your order #{orderNumber} and is heading to you.',
     'ORDER', 'ORDER_PICKED_UP', 'CUSTOMER', 'HIGH', '/orders/{orderId}/track', 168),

    ('ORDER_DELIVERED_CUSTOMER', 'Order Delivered - Customer', 'Order #{orderNumber} Delivered!',
     'Your order has been delivered. Enjoy your meal! Please rate your experience.',
     'ORDER', 'ORDER_DELIVERED', 'CUSTOMER', 'HIGH', '/orders/{orderId}/rate', 168),

    ('ORDER_CANCELLED_CUSTOMER', 'Order Cancelled - Customer', 'Order #{orderNumber} Cancelled',
     'Your order has been cancelled. Reason: {cancellationReason}. A refund will be processed if applicable.',
     'ORDER', 'ORDER_CANCELLED', 'CUSTOMER', 'URGENT', '/orders/{orderId}', 168),

    -- Restaurant Templates
    ('NEW_ORDER_RESTAURANT', 'New Order - Restaurant', 'New Order #{orderNumber}!',
     'You have a new order for {totalAmount}. Items: {itemCount}. Please confirm.',
     'ORDER', 'NEW_ORDER', 'RESTAURANT', 'URGENT', '/restaurant/orders/{orderId}', 24),

    ('COURIER_ARRIVING_RESTAURANT', 'Courier Arriving - Restaurant', 'Courier Arriving Soon',
     'A courier is on the way to pick up order #{orderNumber}.',
     'DELIVERY', 'COURIER_NEARBY', 'RESTAURANT', 'HIGH', '/restaurant/orders/{orderId}', 24),

    -- Courier Templates
    ('ORDER_ASSIGNED_COURIER', 'Order Assigned - Courier', 'New Delivery Assignment!',
     'You have been assigned order #{orderNumber}. Pickup: {pickupAddress}. Delivery: {deliveryAddress}',
     'DELIVERY', 'ORDER_ASSIGNED', 'COURIER', 'URGENT', '/courier/orders/{orderId}', 24),

    ('ORDER_READY_COURIER', 'Order Ready - Courier', 'Order #{orderNumber} Ready for Pickup',
     'The order is ready for pickup at {restaurantName}.',
     'ORDER', 'ORDER_READY', 'COURIER', 'HIGH', '/courier/orders/{orderId}', 24),

    -- Payment Templates
    ('PAYMENT_RECEIVED_CUSTOMER', 'Payment Received', 'Payment Confirmed',
     'Your payment of {amount} for order #{orderNumber} has been confirmed.',
     'FINANCE', 'PAYMENT_RECEIVED', 'CUSTOMER', 'NORMAL', '/orders/{orderId}', 168),

    ('PAYMENT_FAILED_CUSTOMER', 'Payment Failed', 'Payment Failed',
     'Your payment for order #{orderNumber} failed. Reason: {failureReason}. Please try again.',
     'FINANCE', 'PAYMENT_FAILED', 'CUSTOMER', 'URGENT', '/orders/{orderId}/payment', 168),

    ('PAYOUT_ISSUED_RESTAURANT', 'Payout Issued - Restaurant', 'Payout Processed',
     'A payout of {amount} has been issued to your account for the period {periodStart} - {periodEnd}.',
     'FINANCE', 'PAYOUT_ISSUED', 'RESTAURANT', 'NORMAL', '/restaurant/payouts', 720),

    ('PAYOUT_ISSUED_COURIER', 'Payout Issued - Courier', 'Earnings Deposited',
     'Your earnings of {amount} for the period {periodStart} - {periodEnd} have been deposited.',
     'FINANCE', 'PAYOUT_ISSUED', 'COURIER', 'NORMAL', '/courier/earnings', 720),

    -- Support Templates
    ('TICKET_CREATED_USER', 'Support Ticket Created', 'Ticket #{ticketId} Created',
     'Your support ticket regarding "{subject}" has been created. We''ll get back to you soon.',
     'SUPPORT', 'TICKET_CREATED', 'CUSTOMER', 'NORMAL', '/support/tickets/{ticketId}', 720),

    ('TICKET_UPDATED_USER', 'Support Ticket Updated', 'Update on Ticket #{ticketId}',
     'There''s an update on your support ticket: {updateMessage}',
     'SUPPORT', 'TICKET_UPDATED', 'CUSTOMER', 'NORMAL', '/support/tickets/{ticketId}', 720),

    ('TICKET_RESOLVED_USER', 'Support Ticket Resolved', 'Ticket #{ticketId} Resolved',
     'Your support ticket has been resolved. Please let us know if you need further assistance.',
     'SUPPORT', 'TICKET_RESOLVED', 'CUSTOMER', 'NORMAL', '/support/tickets/{ticketId}', 720),

    -- System Templates
    ('SYSTEM_MAINTENANCE', 'System Maintenance', 'Scheduled Maintenance',
     'Scheduled maintenance will occur on {maintenanceDate}. Service may be briefly unavailable.',
     'SYSTEM', 'SYSTEM_MAINTENANCE', 'ALL', 'HIGH', NULL, 168)

ON CONFLICT (template_code) DO NOTHING;

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE notifications IS 'Persistent notifications for all platform users';
COMMENT ON TABLE notification_templates IS 'Templates for generating consistent notification messages';
COMMENT ON TABLE notification_preferences IS 'User preferences for notification categories and channels';
COMMENT ON TABLE notification_read_status IS 'Read status tracking for broadcast notifications';

COMMENT ON COLUMN notifications.user_id IS 'Target user ID (null for broadcast to role)';
COMMENT ON COLUMN notifications.role IS 'Target role for the notification';
COMMENT ON COLUMN notifications.metadata IS 'Flexible JSON storage for additional notification data';
COMMENT ON COLUMN notifications.read_at IS 'Timestamp when notification was read (null = unread)';
COMMENT ON COLUMN notifications.dismissed IS 'Soft delete flag';
COMMENT ON COLUMN notifications.expires_at IS 'Optional expiration timestamp for TTL';

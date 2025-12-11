-- V5__add_activity_logs_table.sql
-- Creates the activity_logs table for tracking user events and analytics

-- Create activity_logs table
CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    event_type VARCHAR(50) NOT NULL,
    restaurant_id BIGINT,
    order_id BIGINT,
    menu_item_id BIGINT,
    session_id VARCHAR(100),
    device_type VARCHAR(20),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    referral_source VARCHAR(100),
    metadata TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints (soft - no cascade delete to preserve history)
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
);

-- Create indexes for efficient analytics queries
-- Index for user activity queries (DAU/WAU/MAU)
CREATE INDEX IF NOT EXISTS idx_activity_user_id ON activity_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_created_at ON activity_logs(created_at);

-- Composite index for user activity by date (most important for DAU/WAU/MAU)
CREATE INDEX IF NOT EXISTS idx_activity_user_event_date ON activity_logs(user_id, event_type, created_at);

-- Index for event type queries (conversion funnel)
CREATE INDEX IF NOT EXISTS idx_activity_event_type ON activity_logs(event_type);

-- Index for restaurant-specific analytics
CREATE INDEX IF NOT EXISTS idx_activity_restaurant_id ON activity_logs(restaurant_id);

-- Index for order-related analytics
CREATE INDEX IF NOT EXISTS idx_activity_order_id ON activity_logs(order_id);

-- Index for session analytics
CREATE INDEX IF NOT EXISTS idx_activity_session_id ON activity_logs(session_id);

-- Partial index for distinct user counting (excludes nulls)
CREATE INDEX IF NOT EXISTS idx_activity_user_not_null ON activity_logs(user_id, created_at) WHERE user_id IS NOT NULL;

-- Index for conversion funnel queries (event type + user + time)
CREATE INDEX IF NOT EXISTS idx_activity_funnel ON activity_logs(event_type, user_id, created_at) WHERE user_id IS NOT NULL;

-- Add comment to table
COMMENT ON TABLE activity_logs IS 'Stores user activity events for analytics (DAU/WAU/MAU, conversion funnels, etc.)';

-- Add comments to columns
COMMENT ON COLUMN activity_logs.event_type IS 'Type of activity event (USER_LOGIN, RESTAURANT_VIEW, ADD_TO_CART, etc.)';
COMMENT ON COLUMN activity_logs.session_id IS 'Session ID for tracking user journey within a single session';
COMMENT ON COLUMN activity_logs.metadata IS 'Additional event metadata stored as JSON';
COMMENT ON COLUMN activity_logs.duration_ms IS 'Duration of activity in milliseconds (e.g., time on page)';

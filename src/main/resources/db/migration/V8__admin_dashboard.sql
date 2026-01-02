-- Admin Dashboard Tables and Materialized Views
-- V8: Dashboard metrics and aggregated analytics

-- Dashboard Widgets Configuration
CREATE TABLE dashboard_widgets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    widget_type VARCHAR(50) NOT NULL,
    config JSONB,
    is_default BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Dashboard Preferences
CREATE TABLE user_dashboard_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    widget_id BIGINT NOT NULL REFERENCES dashboard_widgets(id),
    is_visible BOOLEAN DEFAULT TRUE,
    position_x INTEGER DEFAULT 0,
    position_y INTEGER DEFAULT 0,
    width INTEGER DEFAULT 1,
    height INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_dash_prefs_user ON user_dashboard_preferences(user_id);

-- Alert Rules
CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    metric_type VARCHAR(50) NOT NULL,
    condition VARCHAR(20) NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    notification_channels TEXT[],
    is_active BOOLEAN DEFAULT TRUE,
    cooldown_minutes INTEGER DEFAULT 15,
    last_triggered_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_rules_is_active ON alert_rules(is_active);
CREATE INDEX idx_alert_rules_metric_type ON alert_rules(metric_type);

-- Alert History
CREATE TABLE alert_history (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES alert_rules(id),
    metric_value DOUBLE PRECISION NOT NULL,
    threshold_value DOUBLE PRECISION NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by BIGINT,
    acknowledged_at TIMESTAMP,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_history_rule ON alert_history(rule_id);
CREATE INDEX idx_alert_history_created_at ON alert_history(created_at);
CREATE INDEX idx_alert_history_acknowledged ON alert_history(acknowledged);

-- Daily Order Stats (Materialized View)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_daily_order_stats AS
SELECT
    DATE(o.created_at) as date,
    COUNT(*) as total_orders,
    COUNT(CASE WHEN o.status = 'DELIVERED' THEN 1 END) as completed_orders,
    COUNT(CASE WHEN o.status = 'CANCELLED' THEN 1 END) as cancelled_orders,
    COALESCE(SUM(o.total), 0) as total_revenue,
    COALESCE(AVG(o.total), 0) as avg_order_value,
    COALESCE(AVG(EXTRACT(EPOCH FROM (o.delivered_at - o.created_at))/60), 0) as avg_delivery_time_minutes
FROM orders o
GROUP BY DATE(o.created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_daily_order_stats_date ON mv_daily_order_stats(date);

-- Restaurant Performance Stats (Materialized View)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_restaurant_performance AS
SELECT
    r.id as restaurant_id,
    r.name as restaurant_name,
    r.average_rating,
    r.total_ratings,
    r.is_open,
    COUNT(o.id) as total_orders,
    COUNT(CASE WHEN o.status = 'DELIVERED' THEN 1 END) as completed_orders,
    COALESCE(SUM(o.total), 0) as total_revenue,
    COALESCE(AVG(EXTRACT(EPOCH FROM (o.ready_at - o.accepted_at))/60), 0) as avg_prep_time_minutes
FROM restaurants r
LEFT JOIN orders o ON r.id = o.restaurant_id AND o.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY r.id, r.name, r.average_rating, r.total_ratings, r.is_open;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_restaurant_perf_id ON mv_restaurant_performance(restaurant_id);

-- Courier Performance Stats (Materialized View)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_courier_performance AS
SELECT
    c.id as courier_id,
    c.user_id,
    c.average_rating,
    c.total_ratings,
    c.total_deliveries,
    c.status as current_status,
    COUNT(o.id) as orders_last_30_days,
    COALESCE(AVG(EXTRACT(EPOCH FROM (o.delivered_at - o.picked_up_at))/60), 0) as avg_delivery_time_minutes
FROM couriers c
LEFT JOIN orders o ON c.id = o.courier_id AND o.created_at >= CURRENT_DATE - INTERVAL '30 days' AND o.status = 'DELIVERED'
GROUP BY c.id, c.user_id, c.average_rating, c.total_ratings, c.total_deliveries, c.status;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_courier_perf_id ON mv_courier_performance(courier_id);

-- Hourly Order Volume (Materialized View)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_hourly_order_volume AS
SELECT
    DATE(created_at) as date,
    EXTRACT(HOUR FROM created_at)::INTEGER as hour,
    COUNT(*) as order_count,
    COALESCE(SUM(total), 0) as revenue
FROM orders
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY DATE(created_at), EXTRACT(HOUR FROM created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_hourly_volume ON mv_hourly_order_volume(date, hour);

-- Real-time Metrics Table (for fast dashboard loading)
CREATE TABLE realtime_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL UNIQUE,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_realtime_metrics_name ON realtime_metrics(metric_name);

-- Insert initial metric placeholders
INSERT INTO realtime_metrics (metric_name, metric_value, metric_type) VALUES
    ('active_orders', 0, 'COUNT'),
    ('online_restaurants', 0, 'COUNT'),
    ('available_couriers', 0, 'COUNT'),
    ('today_revenue', 0, 'CURRENCY'),
    ('today_orders', 0, 'COUNT'),
    ('avg_delivery_time', 0, 'DURATION'),
    ('pending_support_tickets', 0, 'COUNT');

-- Dashboard Snapshots for historical trending
CREATE TABLE dashboard_snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    snapshot_hour INTEGER,
    metrics JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_dash_snapshots_date_hour ON dashboard_snapshots(snapshot_date, snapshot_hour);

-- Dashboard Refresh Logs (matching DashboardRefreshLog entity)
CREATE TABLE dashboard_refresh_logs (
    id BIGSERIAL PRIMARY KEY,
    panel_name VARCHAR(50) NOT NULL,
    component VARCHAR(100),
    admin_user_id BIGINT,
    query_duration_ms INTEGER,
    duration_ms BIGINT,
    cache_hit BOOLEAN DEFAULT FALSE,
    records_returned INTEGER,
    filter_params VARCHAR(1000),
    error_message VARCHAR(500),
    successful BOOLEAN DEFAULT TRUE,
    success BOOLEAN DEFAULT TRUE,
    message VARCHAR(500),
    refreshed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_panel ON dashboard_refresh_logs(panel_name);
CREATE INDEX idx_refresh_time ON dashboard_refresh_logs(refreshed_at);
CREATE INDEX idx_refresh_user ON dashboard_refresh_logs(admin_user_id);

-- Function to refresh materialized views
CREATE OR REPLACE FUNCTION refresh_dashboard_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_order_stats;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_restaurant_performance;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_courier_performance;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_hourly_order_volume;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_dashboard_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_dashboard_widgets_updated_at
    BEFORE UPDATE ON dashboard_widgets
    FOR EACH ROW EXECUTE FUNCTION update_dashboard_updated_at();

CREATE TRIGGER update_user_dash_prefs_updated_at
    BEFORE UPDATE ON user_dashboard_preferences
    FOR EACH ROW EXECUTE FUNCTION update_dashboard_updated_at();

CREATE TRIGGER update_alert_rules_updated_at
    BEFORE UPDATE ON alert_rules
    FOR EACH ROW EXECUTE FUNCTION update_dashboard_updated_at();

COMMENT ON TABLE dashboard_widgets IS 'Dashboard widget definitions';
COMMENT ON TABLE user_dashboard_preferences IS 'User-specific dashboard configurations';
COMMENT ON TABLE alert_rules IS 'Configurable alerting rules';
COMMENT ON TABLE alert_history IS 'Historical alert occurrences';
COMMENT ON TABLE realtime_metrics IS 'Cached real-time metrics for fast dashboard loading';
COMMENT ON TABLE dashboard_snapshots IS 'Historical dashboard snapshots for trending';
COMMENT ON MATERIALIZED VIEW mv_daily_order_stats IS 'Daily aggregated order statistics';
COMMENT ON MATERIALIZED VIEW mv_restaurant_performance IS 'Restaurant performance metrics';
COMMENT ON MATERIALIZED VIEW mv_courier_performance IS 'Courier performance metrics';
COMMENT ON MATERIALIZED VIEW mv_hourly_order_volume IS 'Hourly order volume for last 7 days';

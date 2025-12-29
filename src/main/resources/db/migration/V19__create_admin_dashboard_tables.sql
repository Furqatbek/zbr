-- =====================================================
-- Admin Dashboard Tables
-- Version: V19
-- Description: Tables for admin dashboard metrics and logging
-- =====================================================

-- System Health Snapshots Table
-- Captures periodic system health metrics for monitoring
CREATE TABLE IF NOT EXISTS system_health_snapshots (
    id BIGSERIAL PRIMARY KEY,
    component VARCHAR(100) NOT NULL,
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- API Metrics
    api_latency_p50 DOUBLE PRECISION DEFAULT 0,
    api_latency_p90 DOUBLE PRECISION DEFAULT 0,
    api_latency_p99 DOUBLE PRECISION DEFAULT 0,
    api_requests_per_second DOUBLE PRECISION DEFAULT 0,
    api_error_rate DOUBLE PRECISION DEFAULT 0,

    -- Database Metrics
    db_active_connections INTEGER DEFAULT 0,
    db_idle_connections INTEGER DEFAULT 0,
    db_max_connections INTEGER DEFAULT 100,
    db_query_time_avg_ms DOUBLE PRECISION DEFAULT 0,

    -- Redis Metrics
    redis_latency_ms DOUBLE PRECISION DEFAULT 0,
    redis_memory_used_mb DOUBLE PRECISION DEFAULT 0,
    redis_connected_clients INTEGER DEFAULT 0,
    redis_hit_rate DOUBLE PRECISION DEFAULT 0,

    -- Queue Metrics
    queue_backlog INTEGER DEFAULT 0,
    queue_processing_rate DOUBLE PRECISION DEFAULT 0,

    -- System Resources
    cpu_usage_percent DOUBLE PRECISION DEFAULT 0,
    memory_usage_percent DOUBLE PRECISION DEFAULT 0,
    disk_usage_percent DOUBLE PRECISION DEFAULT 0,

    -- Status
    status VARCHAR(20) DEFAULT 'HEALTHY',
    status_message TEXT,

    CONSTRAINT chk_status CHECK (status IN ('HEALTHY', 'DEGRADED', 'CRITICAL', 'UNKNOWN'))
);

-- Index for efficient querying of recent snapshots
CREATE INDEX idx_system_health_captured_at ON system_health_snapshots(captured_at DESC);
CREATE INDEX idx_system_health_component ON system_health_snapshots(component, captured_at DESC);
CREATE INDEX idx_system_health_status ON system_health_snapshots(status, captured_at DESC);

-- Dashboard Refresh Logs Table
-- Tracks dashboard data refresh events for performance monitoring
CREATE TABLE IF NOT EXISTS dashboard_refresh_logs (
    id BIGSERIAL PRIMARY KEY,
    component VARCHAR(100) NOT NULL,
    refreshed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_ms BIGINT DEFAULT 0,
    success BOOLEAN DEFAULT TRUE,
    message TEXT,
    records_processed INTEGER DEFAULT 0,
    triggered_by VARCHAR(50) DEFAULT 'SCHEDULED'
);

-- Index for efficient querying of refresh logs
CREATE INDEX idx_dashboard_refresh_logs_refreshed_at ON dashboard_refresh_logs(refreshed_at DESC);
CREATE INDEX idx_dashboard_refresh_logs_component ON dashboard_refresh_logs(component, refreshed_at DESC);
CREATE INDEX idx_dashboard_refresh_logs_success ON dashboard_refresh_logs(success, refreshed_at DESC);

-- =====================================================
-- Materialized Views for Dashboard Performance
-- These views pre-aggregate commonly accessed metrics
-- =====================================================

-- Daily Order Summary Materialized View
-- Uses correct column names from V1 schema: delivered_at (not delivery_time)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_daily_order_summary AS
SELECT
    DATE(created_at) as order_date,
    COUNT(*) as total_orders,
    COUNT(CASE WHEN status = 'DELIVERED' THEN 1 END) as delivered_orders,
    COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_orders,
    COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected_orders,
    SUM(CASE WHEN status = 'DELIVERED' THEN total_amount ELSE 0 END) as total_gmv,
    AVG(CASE WHEN status = 'DELIVERED' THEN total_amount END) as avg_order_value,
    AVG(CASE WHEN delivered_at IS NOT NULL AND created_at IS NOT NULL
        THEN EXTRACT(EPOCH FROM (delivered_at - created_at))/60 END) as avg_delivery_time_minutes
FROM orders
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY DATE(created_at);

-- Index on materialized view
CREATE UNIQUE INDEX idx_mv_daily_order_summary_date ON mv_daily_order_summary(order_date);

-- Hourly Order Activity Materialized View
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_hourly_order_activity AS
SELECT
    DATE(created_at) as order_date,
    EXTRACT(HOUR FROM created_at) as hour_of_day,
    COUNT(*) as order_count,
    SUM(total_amount) as total_amount
FROM orders
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(created_at), EXTRACT(HOUR FROM created_at);

-- Index on hourly activity view
CREATE INDEX idx_mv_hourly_order_activity ON mv_hourly_order_activity(order_date, hour_of_day);

-- Restaurant Performance Summary Materialized View
-- Uses correct column names from V1 schema: avg_rating (not rating), status (not is_online)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_restaurant_performance_daily AS
SELECT
    r.id as restaurant_id,
    r.name as restaurant_name,
    DATE(o.created_at) as order_date,
    COUNT(o.id) as total_orders,
    COUNT(CASE WHEN o.status = 'DELIVERED' THEN 1 END) as delivered_orders,
    COUNT(CASE WHEN o.status = 'REJECTED' THEN 1 END) as rejected_orders,
    AVG(CASE WHEN o.status = 'DELIVERED' THEN o.total_amount END) as avg_order_value,
    r.avg_rating as current_rating,
    (r.status = 'ACTIVE') as is_online
FROM restaurants r
LEFT JOIN orders o ON r.id = o.restaurant_id AND o.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY r.id, r.name, DATE(o.created_at), r.avg_rating, r.status;

-- Courier Performance Summary Materialized View
-- Uses correct column names from V1 schema:
-- - Joins users table to get courier name (couriers only has user_id)
-- - avg_rating (not rating)
-- - delivered_at (not delivery_time), picked_up_at (not pickup_time)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_courier_performance_daily AS
SELECT
    c.id as courier_id,
    u.full_name as courier_name,
    DATE(o.created_at) as delivery_date,
    COUNT(o.id) as total_deliveries,
    AVG(EXTRACT(EPOCH FROM (o.delivered_at - o.picked_up_at))/60) as avg_delivery_time_minutes,
    c.avg_rating as current_rating,
    c.vehicle_type
FROM couriers c
JOIN users u ON c.user_id = u.id
LEFT JOIN orders o ON c.id = o.courier_id
    AND o.status = 'DELIVERED'
    AND o.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY c.id, u.full_name, DATE(o.created_at), c.avg_rating, c.vehicle_type;

-- =====================================================
-- Functions for Refreshing Materialized Views
-- =====================================================

-- Function to refresh all dashboard materialized views
CREATE OR REPLACE FUNCTION refresh_dashboard_materialized_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_order_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_hourly_order_activity;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_restaurant_performance_daily;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_courier_performance_daily;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- Cleanup Job: Remove old records
-- =====================================================

-- Function to clean up old dashboard data
CREATE OR REPLACE FUNCTION cleanup_old_dashboard_data()
RETURNS void AS $$
BEGIN
    -- Delete system health snapshots older than 7 days
    DELETE FROM system_health_snapshots
    WHERE captured_at < CURRENT_TIMESTAMP - INTERVAL '7 days';

    -- Delete dashboard refresh logs older than 30 days
    DELETE FROM dashboard_refresh_logs
    WHERE refreshed_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- Comments for documentation
-- =====================================================

COMMENT ON TABLE system_health_snapshots IS 'Stores periodic system health metrics for dashboard monitoring';
COMMENT ON TABLE dashboard_refresh_logs IS 'Tracks dashboard data refresh events for performance analysis';
COMMENT ON MATERIALIZED VIEW mv_daily_order_summary IS 'Pre-aggregated daily order statistics for dashboard performance';
COMMENT ON MATERIALIZED VIEW mv_hourly_order_activity IS 'Pre-aggregated hourly order activity for trend analysis';
COMMENT ON MATERIALIZED VIEW mv_restaurant_performance_daily IS 'Pre-aggregated restaurant performance metrics';
COMMENT ON MATERIALIZED VIEW mv_courier_performance_daily IS 'Pre-aggregated courier performance metrics';

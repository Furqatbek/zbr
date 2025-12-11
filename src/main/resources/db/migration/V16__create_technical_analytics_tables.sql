-- Technical Analytics Tables Migration
-- Creates tables for platform health and performance monitoring

-- HTTP Request Logs Table
-- Stores HTTP request data for API performance analysis
CREATE TABLE IF NOT EXISTS http_request_logs (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(64) UNIQUE NOT NULL,
    method VARCHAR(10) NOT NULL,
    endpoint VARCHAR(512) NOT NULL,
    status_code INTEGER NOT NULL,
    response_time_ms BIGINT NOT NULL,
    request_size_bytes BIGINT,
    response_size_bytes BIGINT,
    client_ip VARCHAR(45),
    user_agent VARCHAR(512),
    user_id BIGINT,
    is_timeout BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    request_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for HTTP request logs
CREATE INDEX IF NOT EXISTS idx_http_request_logs_timestamp ON http_request_logs(request_timestamp);
CREATE INDEX IF NOT EXISTS idx_http_request_logs_endpoint ON http_request_logs(endpoint);
CREATE INDEX IF NOT EXISTS idx_http_request_logs_status ON http_request_logs(status_code);
CREATE INDEX IF NOT EXISTS idx_http_request_logs_method ON http_request_logs(method);
CREATE INDEX IF NOT EXISTS idx_http_request_logs_user ON http_request_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_http_request_logs_endpoint_timestamp ON http_request_logs(endpoint, request_timestamp);

-- Slow Query Logs Table
-- Stores slow database queries for performance analysis
CREATE TABLE IF NOT EXISTS slow_query_logs (
    id BIGSERIAL PRIMARY KEY,
    query_hash VARCHAR(64) NOT NULL,
    query_text TEXT NOT NULL,
    query_type VARCHAR(20) NOT NULL,
    duration_ms BIGINT NOT NULL,
    rows_affected BIGINT,
    table_name VARCHAR(128),
    is_using_index BOOLEAN DEFAULT FALSE,
    execution_plan TEXT,
    database_name VARCHAR(64),
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for slow query logs
CREATE INDEX IF NOT EXISTS idx_slow_query_logs_executed_at ON slow_query_logs(executed_at);
CREATE INDEX IF NOT EXISTS idx_slow_query_logs_query_hash ON slow_query_logs(query_hash);
CREATE INDEX IF NOT EXISTS idx_slow_query_logs_table ON slow_query_logs(table_name);
CREATE INDEX IF NOT EXISTS idx_slow_query_logs_duration ON slow_query_logs(duration_ms);
CREATE INDEX IF NOT EXISTS idx_slow_query_logs_type ON slow_query_logs(query_type);

-- WebSocket Connection Logs Table
-- Stores WebSocket connection events
CREATE TABLE IF NOT EXISTS websocket_connection_logs (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    user_id BIGINT,
    user_type VARCHAR(20) NOT NULL,
    client_ip VARCHAR(45),
    device_type VARCHAR(50),
    connected_at TIMESTAMP NOT NULL,
    disconnected_at TIMESTAMP,
    is_graceful_close BOOLEAN DEFAULT TRUE,
    disconnect_reason VARCHAR(128),
    messages_sent BIGINT DEFAULT 0,
    messages_received BIGINT DEFAULT 0,
    last_activity_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for WebSocket connection logs
CREATE INDEX IF NOT EXISTS idx_ws_conn_logs_connected_at ON websocket_connection_logs(connected_at);
CREATE INDEX IF NOT EXISTS idx_ws_conn_logs_disconnected_at ON websocket_connection_logs(disconnected_at);
CREATE INDEX IF NOT EXISTS idx_ws_conn_logs_user ON websocket_connection_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_ws_conn_logs_user_type ON websocket_connection_logs(user_type);
CREATE INDEX IF NOT EXISTS idx_ws_conn_logs_active ON websocket_connection_logs(disconnected_at) WHERE disconnected_at IS NULL;

-- WebSocket Message Logs Table
-- Stores WebSocket message delivery data
CREATE TABLE IF NOT EXISTS websocket_message_logs (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(64) UNIQUE NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    payload_size_bytes INTEGER,
    published_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    is_delivered BOOLEAN DEFAULT FALSE,
    delivery_attempts INTEGER DEFAULT 1,
    failure_reason VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for WebSocket message logs
CREATE INDEX IF NOT EXISTS idx_ws_msg_logs_published_at ON websocket_message_logs(published_at);
CREATE INDEX IF NOT EXISTS idx_ws_msg_logs_session ON websocket_message_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_ws_msg_logs_type ON websocket_message_logs(message_type);
CREATE INDEX IF NOT EXISTS idx_ws_msg_logs_delivered ON websocket_message_logs(is_delivered);

-- Storage Metadata Table
-- Stores file upload and storage information
CREATE TABLE IF NOT EXISTS storage_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_key VARCHAR(512) UNIQUE NOT NULL,
    original_filename VARCHAR(256),
    content_type VARCHAR(128),
    file_size_bytes BIGINT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    bucket_name VARCHAR(128),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    uploaded_by BIGINT,
    upload_duration_ms BIGINT,
    is_upload_successful BOOLEAN DEFAULT TRUE,
    upload_error_reason VARCHAR(256),
    checksum VARCHAR(128),
    is_deleted BOOLEAN DEFAULT FALSE,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for storage metadata
CREATE INDEX IF NOT EXISTS idx_storage_metadata_uploaded_at ON storage_metadata(uploaded_at);
CREATE INDEX IF NOT EXISTS idx_storage_metadata_content_type ON storage_metadata(content_type);
CREATE INDEX IF NOT EXISTS idx_storage_metadata_storage_type ON storage_metadata(storage_type);
CREATE INDEX IF NOT EXISTS idx_storage_metadata_entity ON storage_metadata(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_storage_metadata_deleted ON storage_metadata(is_deleted);
CREATE INDEX IF NOT EXISTS idx_storage_metadata_success ON storage_metadata(is_upload_successful);

-- Message Queue Stats Table
-- Stores RabbitMQ/Kafka statistics
CREATE TABLE IF NOT EXISTS message_queue_stats (
    id BIGSERIAL PRIMARY KEY,
    queue_name VARCHAR(128) NOT NULL,
    broker_type VARCHAR(20) NOT NULL,
    queue_depth BIGINT DEFAULT 0,
    consumer_lag BIGINT DEFAULT 0,
    publish_rate DOUBLE PRECISION DEFAULT 0,
    consume_rate DOUBLE PRECISION DEFAULT 0,
    consumer_count INTEGER DEFAULT 0,
    is_dead_letter_queue BOOLEAN DEFAULT FALSE,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for message queue stats
CREATE INDEX IF NOT EXISTS idx_mq_stats_collected_at ON message_queue_stats(collected_at);
CREATE INDEX IF NOT EXISTS idx_mq_stats_queue ON message_queue_stats(queue_name);
CREATE INDEX IF NOT EXISTS idx_mq_stats_broker ON message_queue_stats(broker_type);
CREATE INDEX IF NOT EXISTS idx_mq_stats_queue_collected ON message_queue_stats(queue_name, collected_at DESC);

-- Backup Logs Table
-- Stores database backup information
CREATE TABLE IF NOT EXISTS backup_logs (
    id BIGSERIAL PRIMARY KEY,
    backup_id VARCHAR(64) UNIQUE NOT NULL,
    database_name VARCHAR(64) NOT NULL,
    backup_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    backup_size_bytes BIGINT,
    backup_path VARCHAR(512),
    is_verified BOOLEAN DEFAULT FALSE,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for backup logs
CREATE INDEX IF NOT EXISTS idx_backup_logs_started_at ON backup_logs(started_at);
CREATE INDEX IF NOT EXISTS idx_backup_logs_status ON backup_logs(status);
CREATE INDEX IF NOT EXISTS idx_backup_logs_database ON backup_logs(database_name);
CREATE INDEX IF NOT EXISTS idx_backup_logs_type ON backup_logs(backup_type);

-- System Metric Snapshots Table
-- Stores periodic system metric snapshots
CREATE TABLE IF NOT EXISTS system_metric_snapshots (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(128) NOT NULL,
    metric_type VARCHAR(20) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(20),
    tags JSONB,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for system metric snapshots
CREATE INDEX IF NOT EXISTS idx_sys_metric_collected_at ON system_metric_snapshots(collected_at);
CREATE INDEX IF NOT EXISTS idx_sys_metric_name ON system_metric_snapshots(metric_name);
CREATE INDEX IF NOT EXISTS idx_sys_metric_type ON system_metric_snapshots(metric_type);
CREATE INDEX IF NOT EXISTS idx_sys_metric_name_collected ON system_metric_snapshots(metric_name, collected_at DESC);

-- Add comments for documentation
COMMENT ON TABLE http_request_logs IS 'Stores HTTP request data for API performance analysis';
COMMENT ON TABLE slow_query_logs IS 'Stores slow database queries detected above threshold';
COMMENT ON TABLE websocket_connection_logs IS 'Stores WebSocket connection lifecycle events';
COMMENT ON TABLE websocket_message_logs IS 'Stores WebSocket message delivery tracking';
COMMENT ON TABLE storage_metadata IS 'Stores file upload and storage metadata';
COMMENT ON TABLE message_queue_stats IS 'Stores periodic RabbitMQ/Kafka statistics snapshots';
COMMENT ON TABLE backup_logs IS 'Stores database backup execution logs';
COMMENT ON TABLE system_metric_snapshots IS 'Stores periodic system metric snapshots for historical analysis';

-- Create retention policy function for old data cleanup
CREATE OR REPLACE FUNCTION cleanup_old_technical_logs(retention_days INTEGER DEFAULT 30)
RETURNS void AS $$
DECLARE
    cutoff_date TIMESTAMP;
BEGIN
    cutoff_date := NOW() - (retention_days || ' days')::INTERVAL;

    -- Clean up HTTP request logs
    DELETE FROM http_request_logs WHERE request_timestamp < cutoff_date;

    -- Clean up slow query logs
    DELETE FROM slow_query_logs WHERE executed_at < cutoff_date;

    -- Clean up WebSocket connection logs (only completed connections)
    DELETE FROM websocket_connection_logs WHERE disconnected_at < cutoff_date;

    -- Clean up WebSocket message logs
    DELETE FROM websocket_message_logs WHERE published_at < cutoff_date;

    -- Clean up message queue stats (keep shorter history)
    DELETE FROM message_queue_stats WHERE collected_at < (NOW() - '7 days'::INTERVAL);

    -- Clean up system metric snapshots (keep shorter history)
    DELETE FROM system_metric_snapshots WHERE collected_at < (NOW() - '7 days'::INTERVAL);

    -- Keep storage metadata for deleted files only for retention period
    DELETE FROM storage_metadata WHERE is_deleted = TRUE AND deleted_at < cutoff_date;

    RAISE NOTICE 'Cleaned up technical logs older than %', cutoff_date;
END;
$$ LANGUAGE plpgsql;

-- Create index statistics view for database performance analysis
CREATE OR REPLACE VIEW vw_index_usage_stats AS
SELECT
    schemaname,
    tablename,
    indexrelname AS index_name,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Create table statistics view for database performance analysis
CREATE OR REPLACE VIEW vw_table_stats AS
SELECT
    schemaname,
    relname AS table_name,
    seq_scan AS sequential_scans,
    seq_tup_read AS seq_tuples_read,
    idx_scan AS index_scans,
    idx_tup_fetch AS idx_tuples_fetched,
    n_tup_ins AS inserts,
    n_tup_upd AS updates,
    n_tup_del AS deletes,
    n_live_tup AS live_tuples,
    n_dead_tup AS dead_tuples,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC;

-- Grant permissions (adjust role names as needed)
-- GRANT SELECT, INSERT, DELETE ON http_request_logs TO app_user;
-- GRANT SELECT, INSERT, DELETE ON slow_query_logs TO app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON websocket_connection_logs TO app_user;
-- GRANT SELECT, INSERT, DELETE ON websocket_message_logs TO app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON storage_metadata TO app_user;
-- GRANT SELECT, INSERT, DELETE ON message_queue_stats TO app_user;
-- GRANT SELECT, INSERT ON backup_logs TO app_user;
-- GRANT SELECT, INSERT, DELETE ON system_metric_snapshots TO app_user;

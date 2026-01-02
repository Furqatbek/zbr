-- Technical Analytics Tables
-- V5: Platform health and performance monitoring (matching JPA entities exactly)

-- HTTP Request Logs (matching HttpRequestLog entity)
CREATE TABLE http_request_logs (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(50),
    method VARCHAR(10) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    status_code INTEGER NOT NULL,
    response_time_ms BIGINT NOT NULL,
    request_size_bytes BIGINT,
    response_size_bytes BIGINT,
    user_id BIGINT,
    user_agent VARCHAR(500),
    client_ip VARCHAR(45),
    error_message VARCHAR(2000),
    is_timeout BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hrl_timestamp ON http_request_logs(timestamp);
CREATE INDEX idx_hrl_status_code ON http_request_logs(status_code);
CREATE INDEX idx_hrl_endpoint ON http_request_logs(endpoint);
CREATE INDEX idx_hrl_method ON http_request_logs(method);
CREATE INDEX idx_hrl_response_time ON http_request_logs(response_time_ms);

-- Slow Query Logs (matching SlowQueryLog entity)
CREATE TABLE slow_query_logs (
    id BIGSERIAL PRIMARY KEY,
    query_hash VARCHAR(64),
    query_text TEXT NOT NULL,
    duration_ms BIGINT NOT NULL,
    rows_affected BIGINT,
    rows_examined BIGINT,
    database_name VARCHAR(100),
    table_name VARCHAR(100),
    query_type VARCHAR(20),
    is_using_index BOOLEAN,
    index_used VARCHAR(100),
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sql_timestamp ON slow_query_logs(timestamp);
CREATE INDEX idx_sql_duration ON slow_query_logs(duration_ms);
CREATE INDEX idx_sql_query_hash ON slow_query_logs(query_hash);

-- WebSocket Connection Logs (matching WebSocketConnectionLog entity)
CREATE TABLE websocket_connection_logs (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    user_type VARCHAR(20) NOT NULL,
    connected_at TIMESTAMP NOT NULL,
    disconnected_at TIMESTAMP,
    disconnect_reason VARCHAR(100),
    is_graceful_disconnect BOOLEAN,
    client_ip VARCHAR(45),
    user_agent VARCHAR(500),
    messages_sent BIGINT DEFAULT 0,
    messages_received BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wscl_user_type ON websocket_connection_logs(user_type);
CREATE INDEX idx_wscl_connected_at ON websocket_connection_logs(connected_at);
CREATE INDEX idx_wscl_disconnected_at ON websocket_connection_logs(disconnected_at);
CREATE INDEX idx_wscl_session_id ON websocket_connection_logs(session_id);

-- WebSocket Message Logs (matching WebSocketMessageLog entity)
CREATE TABLE websocket_message_logs (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    destination VARCHAR(200),
    payload_size_bytes BIGINT,
    published_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    is_delivered BOOLEAN DEFAULT FALSE,
    delivery_attempts INTEGER DEFAULT 0,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wsml_session_id ON websocket_message_logs(session_id);
CREATE INDEX idx_wsml_message_type ON websocket_message_logs(message_type);
CREATE INDEX idx_wsml_published_at ON websocket_message_logs(published_at);
CREATE INDEX idx_wsml_delivered_at ON websocket_message_logs(delivered_at);

-- Storage Metadata (matching StorageMetadata entity)
CREATE TABLE storage_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    file_size_bytes BIGINT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    bucket_name VARCHAR(100),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    uploaded_by BIGINT,
    upload_duration_ms BIGINT,
    is_upload_successful BOOLEAN DEFAULT TRUE,
    error_message VARCHAR(500),
    uploaded_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sm_storage_type ON storage_metadata(storage_type);
CREATE INDEX idx_sm_content_type ON storage_metadata(content_type);
CREATE INDEX idx_sm_uploaded_at ON storage_metadata(uploaded_at);
CREATE INDEX idx_sm_entity_type ON storage_metadata(entity_type);

-- Message Queue Stats (matching MessageQueueStats entity)
CREATE TABLE message_queue_stats (
    id BIGSERIAL PRIMARY KEY,
    queue_name VARCHAR(100) NOT NULL,
    broker_type VARCHAR(20) NOT NULL,
    queue_depth BIGINT,
    messages_published BIGINT,
    messages_consumed BIGINT,
    messages_acknowledged BIGINT,
    messages_rejected BIGINT,
    consumer_count INTEGER,
    publish_rate DOUBLE PRECISION,
    consume_rate DOUBLE PRECISION,
    avg_processing_time_ms BIGINT,
    consumer_lag BIGINT,
    partition_count INTEGER,
    is_healthy BOOLEAN DEFAULT TRUE,
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mqs_queue_name ON message_queue_stats(queue_name);
CREATE INDEX idx_mqs_broker_type ON message_queue_stats(broker_type);
CREATE INDEX idx_mqs_recorded_at ON message_queue_stats(recorded_at);

-- Backup Logs (matching BackupLog entity)
CREATE TABLE backup_logs (
    id BIGSERIAL PRIMARY KEY,
    backup_id VARCHAR(50) NOT NULL,
    backup_type VARCHAR(20) NOT NULL,
    database_name VARCHAR(100),
    backup_size_bytes BIGINT,
    backup_location VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    duration_seconds BIGINT,
    error_message VARCHAR(1000),
    tables_backed_up INTEGER,
    rows_backed_up BIGINT,
    is_compressed BOOLEAN DEFAULT TRUE,
    is_encrypted BOOLEAN DEFAULT FALSE,
    retention_days INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bl_backup_type ON backup_logs(backup_type);
CREATE INDEX idx_bl_status ON backup_logs(status);
CREATE INDEX idx_bl_started_at ON backup_logs(started_at);

-- System Metric Snapshots (matching SystemMetricSnapshot entity)
CREATE TABLE system_metric_snapshots (
    id BIGSERIAL PRIMARY KEY,
    metric_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(20),
    tags VARCHAR(500),
    host VARCHAR(100),
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sms_metric_type ON system_metric_snapshots(metric_type);
CREATE INDEX idx_sms_recorded_at ON system_metric_snapshots(recorded_at);

-- Cleanup function for old technical logs
CREATE OR REPLACE FUNCTION cleanup_old_technical_logs(retention_days INTEGER DEFAULT 30)
RETURNS void AS $$
DECLARE
    cutoff_date TIMESTAMP;
BEGIN
    cutoff_date := NOW() - (retention_days || ' days')::INTERVAL;
    DELETE FROM http_request_logs WHERE timestamp < cutoff_date;
    DELETE FROM slow_query_logs WHERE timestamp < cutoff_date;
    DELETE FROM websocket_connection_logs WHERE disconnected_at < cutoff_date;
    DELETE FROM websocket_message_logs WHERE published_at < cutoff_date;
    DELETE FROM message_queue_stats WHERE recorded_at < (NOW() - '7 days'::INTERVAL);
    DELETE FROM system_metric_snapshots WHERE recorded_at < (NOW() - '7 days'::INTERVAL);
    DELETE FROM storage_metadata WHERE deleted_at IS NOT NULL AND deleted_at < cutoff_date;
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE http_request_logs IS 'HTTP request data for API performance analysis';
COMMENT ON TABLE slow_query_logs IS 'Slow database queries detected above threshold';
COMMENT ON TABLE websocket_connection_logs IS 'WebSocket connection lifecycle events';
COMMENT ON TABLE websocket_message_logs IS 'WebSocket message delivery tracking';
COMMENT ON TABLE storage_metadata IS 'File upload and storage metadata';
COMMENT ON TABLE message_queue_stats IS 'Periodic RabbitMQ/Kafka statistics';
COMMENT ON TABLE backup_logs IS 'Database backup execution logs';
COMMENT ON TABLE system_metric_snapshots IS 'Periodic system metric snapshots';

-- Fix backup_logs table to match BackupLog entity
-- V10: Add missing columns

-- Drop and recreate backup_logs with correct schema
DROP TABLE IF EXISTS backup_logs CASCADE;

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

COMMENT ON TABLE backup_logs IS 'Stores database backup execution logs';

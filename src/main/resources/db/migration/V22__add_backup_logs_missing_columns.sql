-- =====================================================
-- V22: Add missing columns to backup_logs table
-- =====================================================
-- Description: Adds columns required by BackupLog entity that were
-- not included in the original V16 migration

-- Add backup_location column (entity expects this, migration has backup_path)
ALTER TABLE backup_logs ADD COLUMN IF NOT EXISTS backup_location VARCHAR(500);

-- Copy data from backup_path to backup_location if backup_path exists
UPDATE backup_logs SET backup_location = backup_path WHERE backup_location IS NULL AND backup_path IS NOT NULL;

-- Add duration_seconds column
ALTER TABLE backup_logs ADD COLUMN IF NOT EXISTS duration_seconds BIGINT;

-- Add tables_backed_up column
ALTER TABLE backup_logs ADD COLUMN IF NOT EXISTS tables_backed_up INTEGER;

-- Add rows_backed_up column
ALTER TABLE backup_logs ADD COLUMN IF NOT EXISTS rows_backed_up BIGINT;

-- Add is_compressed column
ALTER TABLE backup_logs ADD COLUMN IF NOT EXISTS is_compressed BOOLEAN DEFAULT TRUE;

-- Add is_encrypted column
ALTER TABLE backup_logs ADD COLUMN IF NOT EXISTS is_encrypted BOOLEAN DEFAULT FALSE;

-- Add retention_days column
ALTER TABLE backup_logs ADD COLUMN IF NOT EXISTS retention_days INTEGER;

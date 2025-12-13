-- =====================================================
-- V21: Add missing columns to auth_logs table
-- =====================================================
-- Description: Adds columns required by AuthLog entity that were
-- not included in the original V18 migration

-- Add username column
ALTER TABLE auth_logs ADD COLUMN IF NOT EXISTS username VARCHAR(255);

-- Add device_fingerprint column
ALTER TABLE auth_logs ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(255);

-- Add geo_country column (entity uses this instead of country_code)
ALTER TABLE auth_logs ADD COLUMN IF NOT EXISTS geo_country VARCHAR(2);

-- Add geo_city column
ALTER TABLE auth_logs ADD COLUMN IF NOT EXISTS geo_city VARCHAR(100);

-- Add session_id column
ALTER TABLE auth_logs ADD COLUMN IF NOT EXISTS session_id VARCHAR(255);

-- Expand status column to accommodate all AuthLog.AuthStatus enum values
-- (SUCCESS, FAILED, LOCKED, RATE_LIMITED, TOKEN_EXPIRED, TOKEN_INVALID, MFA_REQUIRED, MFA_FAILED, SESSION_EXPIRED, SUSPICIOUS)
ALTER TABLE auth_logs ALTER COLUMN status TYPE VARCHAR(30);

-- Expand failure_reason column
ALTER TABLE auth_logs ALTER COLUMN failure_reason TYPE VARCHAR(255);

-- Make auth_type nullable (entity allows null)
ALTER TABLE auth_logs ALTER COLUMN auth_type DROP NOT NULL;

-- Drop restrictive CHECK constraints that conflict with entity enum values
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_auth_type') THEN
        ALTER TABLE auth_logs DROP CONSTRAINT chk_auth_type;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_auth_status') THEN
        ALTER TABLE auth_logs DROP CONSTRAINT chk_auth_status;
    END IF;
END $$;

-- Change user_agent from TEXT to VARCHAR(500) to match entity
ALTER TABLE auth_logs ALTER COLUMN user_agent TYPE VARCHAR(500);

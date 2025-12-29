-- V4: Add OTP table and update users table for phone-based authentication

-- Drop existing table to handle schema changes from partial runs
DROP TABLE IF EXISTS otp_codes CASCADE;

-- Create OTP codes table
CREATE TABLE otp_codes (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    purpose VARCHAR(20) NOT NULL DEFAULT 'LOGIN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for OTP codes
CREATE INDEX idx_otp_phone ON otp_codes(phone);
CREATE INDEX idx_otp_phone_code ON otp_codes(phone, code);
CREATE INDEX idx_otp_expires_at ON otp_codes(expires_at);

-- Alter users table to make email and password nullable for phone-only users
-- Use DO block to handle already-nullable columns gracefully
DO $$
BEGIN
    -- Make email nullable if not already
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'email' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
    END IF;

    -- Make password_hash nullable if not already
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'password_hash' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
    END IF;
END $$;

-- Make phone unique (drop and recreate to be idempotent)
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_key;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'users_phone_key'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT users_phone_key UNIQUE (phone);
    END IF;
END $$;

-- Add role column to users table (single role) - already exists in V1, skip if present
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20);

-- Add address fields for consumers
ALTER TABLE users ADD COLUMN IF NOT EXISTS address VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Add comment for documentation
COMMENT ON TABLE otp_codes IS 'Stores OTP codes for phone-based authentication';
COMMENT ON COLUMN otp_codes.purpose IS 'Purpose: LOGIN, SIGNUP, PASSWORD_RESET, PHONE_VERIFICATION';

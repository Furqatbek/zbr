-- =====================================================
-- V21: Add Missing Entity Columns
-- =====================================================
-- Description: Adds columns that are required by JPA entities but may be
-- missing from tables created by earlier migrations.
-- This migration ensures schema compatibility with Hibernate validation.
-- =====================================================

-- =====================================================
-- Auth Logs Table - Missing Columns
-- =====================================================

-- Add device_fingerprint column (required by AuthLog entity)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'auth_logs' AND column_name = 'device_fingerprint'
    ) THEN
        ALTER TABLE auth_logs ADD COLUMN device_fingerprint VARCHAR(255);
        RAISE NOTICE 'Added device_fingerprint column to auth_logs table';
    END IF;
END $$;

-- Add username column (required by AuthLog entity)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'auth_logs' AND column_name = 'username'
    ) THEN
        ALTER TABLE auth_logs ADD COLUMN username VARCHAR(255);
        RAISE NOTICE 'Added username column to auth_logs table';
    END IF;
END $$;

-- Add geo_country column (required by AuthLog entity)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'auth_logs' AND column_name = 'geo_country'
    ) THEN
        ALTER TABLE auth_logs ADD COLUMN geo_country VARCHAR(2);
        RAISE NOTICE 'Added geo_country column to auth_logs table';
    END IF;
END $$;

-- Add geo_city column (required by AuthLog entity)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'auth_logs' AND column_name = 'geo_city'
    ) THEN
        ALTER TABLE auth_logs ADD COLUMN geo_city VARCHAR(100);
        RAISE NOTICE 'Added geo_city column to auth_logs table';
    END IF;
END $$;

-- Add session_id column (required by AuthLog entity)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'auth_logs' AND column_name = 'session_id'
    ) THEN
        ALTER TABLE auth_logs ADD COLUMN session_id VARCHAR(255);
        RAISE NOTICE 'Added session_id column to auth_logs table';
    END IF;
END $$;

-- =====================================================
-- Restaurants Table - Missing Columns
-- =====================================================

-- Add is_online column (used by dashboard materialized views)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'restaurants' AND column_name = 'is_online'
    ) THEN
        ALTER TABLE restaurants ADD COLUMN is_online BOOLEAN DEFAULT FALSE;
        RAISE NOTICE 'Added is_online column to restaurants table';
    END IF;
END $$;

-- Add rating column if only avg_rating exists (for entity compatibility)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'restaurants' AND column_name = 'rating'
    ) THEN
        ALTER TABLE restaurants ADD COLUMN rating DECIMAL(3, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added rating column to restaurants table';
    END IF;
END $$;

-- =====================================================
-- Couriers Table - Missing Columns
-- =====================================================

-- Add name column (used by dashboard materialized views)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'couriers' AND column_name = 'name'
    ) THEN
        ALTER TABLE couriers ADD COLUMN name VARCHAR(255);
        RAISE NOTICE 'Added name column to couriers table';
    END IF;
END $$;

-- Add rating column if only avg_rating exists (for entity compatibility)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'couriers' AND column_name = 'rating'
    ) THEN
        ALTER TABLE couriers ADD COLUMN rating DECIMAL(3, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added rating column to couriers table';
    END IF;
END $$;

-- =====================================================
-- Orders Table - Missing Columns
-- =====================================================

-- Add total_amount column if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'total_amount'
    ) THEN
        ALTER TABLE orders ADD COLUMN total_amount DECIMAL(10, 2) DEFAULT 0;
        RAISE NOTICE 'Added total_amount column to orders table';
    END IF;
END $$;

-- Add delivered_at column if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'delivered_at'
    ) THEN
        ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP;
        RAISE NOTICE 'Added delivered_at column to orders table';
    END IF;
END $$;

-- Add picked_up_at column if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'picked_up_at'
    ) THEN
        ALTER TABLE orders ADD COLUMN picked_up_at TIMESTAMP;
        RAISE NOTICE 'Added picked_up_at column to orders table';
    END IF;
END $$;

-- =====================================================
-- Referral Events Table - Missing Columns
-- =====================================================

-- Add is_suspicious column (used by repository queries)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'referral_events' AND column_name = 'is_suspicious'
    ) THEN
        ALTER TABLE referral_events ADD COLUMN is_suspicious BOOLEAN DEFAULT FALSE;
        RAISE NOTICE 'Added is_suspicious column to referral_events table';
    END IF;
END $$;

-- =====================================================
-- Documentation
-- =====================================================
COMMENT ON TABLE auth_logs IS 'Authentication logs with device_fingerprint for fraud detection';

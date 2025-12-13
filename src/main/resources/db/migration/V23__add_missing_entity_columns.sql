-- =====================================================
-- V23: Add missing columns to match JPA entities
-- =====================================================
-- Description: Adds columns required by JPA entities that were
-- not included in the original migrations

-- =====================================================
-- Orders table - missing columns from Order entity
-- =====================================================
ALTER TABLE orders ADD COLUMN IF NOT EXISTS consumer_rating INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS consumer_review TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

-- =====================================================
-- Menu Items table - missing columns from MenuItem entity
-- =====================================================
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS price_with_margin DECIMAL(10,2);
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS original_price DECIMAL(10,2);
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS allergens VARCHAR(500);
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

-- =====================================================
-- Users table - missing columns from User entity
-- =====================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS address VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- =====================================================
-- Couriers table - missing columns from Courier entity
-- =====================================================
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS vehicle_number VARCHAR(50);
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS acceptance_rate DOUBLE PRECISION DEFAULT 0;
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS total_earnings DECIMAL(12,2) DEFAULT 0;
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS max_concurrent_orders INTEGER DEFAULT 3;
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS current_order_count INTEGER DEFAULT 0;
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS preferred_radius_km DOUBLE PRECISION DEFAULT 5.0;
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS documents_submitted BOOLEAN DEFAULT FALSE;
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- =====================================================
-- Notification Templates table - missing columns
-- =====================================================
ALTER TABLE notification_templates ADD COLUMN IF NOT EXISTS icon VARCHAR(100);

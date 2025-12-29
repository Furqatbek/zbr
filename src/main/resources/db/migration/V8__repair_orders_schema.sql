-- =====================================================
-- V8: Schema Repair Migration for Orders Table
-- =====================================================
-- Description: Ensures the orders table has all required columns
-- that might be missing due to schema evolution.
-- This migration is safe to run on both fresh and existing databases.
-- =====================================================

-- Add total_amount column if it doesn't exist
-- This column is required by V19 materialized views
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'total_amount'
    ) THEN
        ALTER TABLE orders ADD COLUMN total_amount DECIMAL(10, 2);
        -- Update existing rows: calculate total_amount from other fields if they exist
        UPDATE orders SET total_amount = COALESCE(subtotal, 0) + COALESCE(tax_amount, 0) + COALESCE(delivery_fee, 0) + COALESCE(tip_amount, 0) - COALESCE(discount_amount, 0)
        WHERE total_amount IS NULL;
        -- Make it NOT NULL after populating
        ALTER TABLE orders ALTER COLUMN total_amount SET NOT NULL;
        RAISE NOTICE 'Added total_amount column to orders table';
    END IF;
END $$;

-- Ensure subtotal column exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'subtotal'
    ) THEN
        ALTER TABLE orders ADD COLUMN subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0;
        RAISE NOTICE 'Added subtotal column to orders table';
    END IF;
END $$;

-- Ensure tax_amount column exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'tax_amount'
    ) THEN
        ALTER TABLE orders ADD COLUMN tax_amount DECIMAL(10, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added tax_amount column to orders table';
    END IF;
END $$;

-- Ensure delivery_fee column exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'delivery_fee'
    ) THEN
        ALTER TABLE orders ADD COLUMN delivery_fee DECIMAL(10, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added delivery_fee column to orders table';
    END IF;
END $$;

-- Ensure tip_amount column exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'tip_amount'
    ) THEN
        ALTER TABLE orders ADD COLUMN tip_amount DECIMAL(10, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added tip_amount column to orders table';
    END IF;
END $$;

-- Ensure discount_amount column exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'discount_amount'
    ) THEN
        ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(10, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added discount_amount column to orders table';
    END IF;
END $$;

-- Ensure delivered_at column exists (used by V19 materialized views)
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

-- Ensure picked_up_at column exists (used by V19 materialized views)
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

-- Ensure restaurants table has avg_rating column (used by V19 materialized views)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'restaurants' AND column_name = 'avg_rating'
    ) THEN
        ALTER TABLE restaurants ADD COLUMN avg_rating DECIMAL(3, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added avg_rating column to restaurants table';
    END IF;
END $$;

-- Ensure couriers table has avg_rating column (used by V19 materialized views)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'couriers' AND column_name = 'avg_rating'
    ) THEN
        ALTER TABLE couriers ADD COLUMN avg_rating DECIMAL(3, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added avg_rating column to couriers table';
    END IF;
END $$;

-- Ensure couriers table has vehicle_type column (used by V19 materialized views)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'couriers' AND column_name = 'vehicle_type'
    ) THEN
        ALTER TABLE couriers ADD COLUMN vehicle_type VARCHAR(50);
        RAISE NOTICE 'Added vehicle_type column to couriers table';
    END IF;
END $$;

-- Add comment for documentation
COMMENT ON TABLE orders IS 'Orders table with schema consistency ensured by V8 migration';

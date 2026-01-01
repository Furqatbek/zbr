-- =====================================================
-- V8: Schema Repair Migration for Orders Table
-- =====================================================
-- Description: Ensures the orders table has all required columns
-- that might be missing due to schema evolution.
-- This migration is safe to run on both fresh and existing databases.
-- =====================================================

-- Add total_amount column if it doesn't exist (alias for 'total' column)
-- This column is required by V19 materialized views
DO $$
BEGIN
    -- First check if 'total' column exists (from Order entity)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'total'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'total_amount'
    ) THEN
        -- Create total_amount as a generated column based on total
        ALTER TABLE orders ADD COLUMN total_amount DECIMAL(10, 2) GENERATED ALWAYS AS (total) STORED;
        RAISE NOTICE 'Added total_amount as generated column from total';
    ELSIF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'total_amount'
    ) THEN
        -- No total column exists, create total_amount with default
        ALTER TABLE orders ADD COLUMN total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0;
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

-- Ensure tax column exists (Order entity uses 'tax', not 'tax_amount')
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'tax'
    ) THEN
        ALTER TABLE orders ADD COLUMN tax DECIMAL(10, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added tax column to orders table';
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

-- Ensure discount column exists (Order entity uses 'discount', not 'discount_amount')
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'discount'
    ) THEN
        ALTER TABLE orders ADD COLUMN discount DECIMAL(10, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added discount column to orders table';
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

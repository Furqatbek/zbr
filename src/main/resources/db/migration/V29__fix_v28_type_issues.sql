-- =====================================================
-- V29: Fix V28 type issues and continue schema fixes
-- =====================================================

-- Fix table_id type - V1 has table_number as VARCHAR, entity expects INTEGER
-- First drop the column if it exists with wrong type, then recreate
ALTER TABLE orders DROP COLUMN IF EXISTS table_id;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_id INTEGER;

-- Copy with cast
UPDATE orders SET table_id = CAST(table_number AS INTEGER)
WHERE table_id IS NULL AND table_number IS NOT NULL AND table_number ~ '^[0-9]+$';

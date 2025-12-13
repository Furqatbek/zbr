-- =====================================================
-- V26: Fix couriers table schema to match Courier entity
-- =====================================================

-- Add missing columns with correct names and types

-- average_rating (entity name) - V1 has avg_rating
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS average_rating DECIMAL(3,2) DEFAULT 0;

-- current_lat (entity name) - V1 has current_latitude
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS current_lat DECIMAL(10,7);

-- current_lng (entity name) - V1 has current_longitude
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS current_lng DECIMAL(10,7);

-- location_updated_at (entity name) - V1 has last_location_update
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS location_updated_at TIMESTAMP;

-- license_number (entity name) - V1 has license_plate
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS license_number VARCHAR(50);

-- Copy data from old column names to new ones
UPDATE couriers SET average_rating = avg_rating WHERE average_rating IS NULL AND avg_rating IS NOT NULL;
UPDATE couriers SET current_lat = current_latitude WHERE current_lat IS NULL AND current_latitude IS NOT NULL;
UPDATE couriers SET current_lng = current_longitude WHERE current_lng IS NULL AND current_longitude IS NOT NULL;
UPDATE couriers SET location_updated_at = last_location_update WHERE location_updated_at IS NULL AND last_location_update IS NOT NULL;
UPDATE couriers SET license_number = license_plate WHERE license_number IS NULL AND license_plate IS NOT NULL;

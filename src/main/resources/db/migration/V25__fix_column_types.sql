-- =====================================================
-- V25: Fix column types to match JPA entity definitions
-- =====================================================

-- Fix couriers.acceptance_rate: DOUBLE PRECISION -> DECIMAL(5,2)
ALTER TABLE couriers ALTER COLUMN acceptance_rate TYPE DECIMAL(5,2);

-- Fix couriers.preferred_radius_km: DOUBLE PRECISION -> INTEGER
ALTER TABLE couriers ALTER COLUMN preferred_radius_km TYPE INTEGER USING preferred_radius_km::INTEGER;

-- Fix a Hibernate schema-validation failure: V34 created courier_delivery_rating
-- as SMALLINT, but the Order.courierDeliveryRating entity field is a Java Integer,
-- which Hibernate maps to SQL INTEGER. Under ddl-auto=validate (prod) the
-- SMALLINT vs INTEGER mismatch aborts EntityManagerFactory startup.
--
-- Widen to INTEGER (no data loss; the 1..5 CHECK constraint is preserved).
-- V34 is left untouched (already applied — editing it would break Flyway's
-- checksum); a fresh install runs V34 then this, ending at INTEGER.
ALTER TABLE orders
    ALTER COLUMN courier_delivery_rating TYPE INTEGER
    USING courier_delivery_rating::integer;

-- V27: Add operating hours to seed data restaurant
-- Sets 24-hour operating hours for demo restaurant to ensure it's always available for testing/demo

UPDATE restaurants
SET opens_at = '00:00',
    closes_at = '23:59'
WHERE slug = 'pizza-palace'
  AND opens_at IS NULL
  AND closes_at IS NULL;

-- Report orphaned rows for each foreign key that V32 added as NOT VALID.
-- Read-only. Any constraint with orphan_rows > 0 has child rows pointing at a
-- parent that no longer exists; fix those before running validate_fks.sql
-- (that FK's VALIDATE will fail until the orphans are removed or repointed).
--
--   psql "$DATABASE_URL" -f scripts/db/check_fk_orphans.sql

SELECT 'fk_restaurants_owner' AS constraint_name, count(*) AS orphan_rows
  FROM restaurants c
 WHERE c.owner_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM users p WHERE p.id = c.owner_id)
UNION ALL
SELECT 'fk_menu_categories_restaurant', count(*)
  FROM menu_categories c
 WHERE c.restaurant_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM restaurants p WHERE p.id = c.restaurant_id)
UNION ALL
SELECT 'fk_menu_items_category', count(*)
  FROM menu_items c
 WHERE c.category_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM menu_categories p WHERE p.id = c.category_id)
UNION ALL
SELECT 'fk_couriers_user', count(*)
  FROM couriers c
 WHERE c.user_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM users p WHERE p.id = c.user_id)
UNION ALL
SELECT 'fk_orders_consumer', count(*)
  FROM orders c
 WHERE c.consumer_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM users p WHERE p.id = c.consumer_id)
UNION ALL
SELECT 'fk_orders_restaurant', count(*)
  FROM orders c
 WHERE c.restaurant_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM restaurants p WHERE p.id = c.restaurant_id)
UNION ALL
SELECT 'fk_orders_courier', count(*)
  FROM orders c
 WHERE c.courier_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM couriers p WHERE p.id = c.courier_id)
UNION ALL
SELECT 'fk_order_items_order', count(*)
  FROM order_items c
 WHERE c.order_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM orders p WHERE p.id = c.order_id)
UNION ALL
SELECT 'fk_payments_order', count(*)
  FROM payments c
 WHERE c.order_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM orders p WHERE p.id = c.order_id)
ORDER BY orphan_rows DESC, constraint_name;

-- Data-integrity hardening: foreign keys on the core V1 tables, a unique
-- constraint on commission per order, and NOT NULL on money columns.
--
-- Foreign keys are added NOT VALID so the migration cannot fail on any legacy
-- drift in an existing database; they are enforced for all new/modified rows
-- going forward. (Flyway is disabled in the H2 test profile, so this Postgres
-- syntax only runs on dev/docker/prod.)

-- ---------------------------------------------------------------------------
-- 1. Money columns: backfill NULLs, then forbid NULL.
-- ---------------------------------------------------------------------------
UPDATE orders SET tax = 0 WHERE tax IS NULL;
UPDATE orders SET delivery_fee = 0 WHERE delivery_fee IS NULL;
UPDATE orders SET discount = 0 WHERE discount IS NULL;
UPDATE orders SET tip_amount = 0 WHERE tip_amount IS NULL;
ALTER TABLE orders ALTER COLUMN tax SET NOT NULL;
ALTER TABLE orders ALTER COLUMN delivery_fee SET NOT NULL;
ALTER TABLE orders ALTER COLUMN discount SET NOT NULL;
ALTER TABLE orders ALTER COLUMN tip_amount SET NOT NULL;

UPDATE restaurants SET commission_rate = 15.00 WHERE commission_rate IS NULL;
ALTER TABLE restaurants ALTER COLUMN commission_rate SET DEFAULT 15.00;
ALTER TABLE restaurants ALTER COLUMN commission_rate SET NOT NULL;

-- ---------------------------------------------------------------------------
-- 2. Unique commission per order (dedupe first, then constrain).
--    The recordCommission double-insert race could leave two rows per order.
-- ---------------------------------------------------------------------------
DELETE FROM restaurant_commissions rc
USING restaurant_commissions dup
WHERE rc.order_id = dup.order_id
  AND rc.id > dup.id;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_restaurant_commissions_order'
    ) THEN
        ALTER TABLE restaurant_commissions
            ADD CONSTRAINT uq_restaurant_commissions_order UNIQUE (order_id);
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 3. Foreign keys on core tables (NOT VALID — enforced for new/modified rows).
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_restaurants_owner') THEN
        ALTER TABLE restaurants ADD CONSTRAINT fk_restaurants_owner
            FOREIGN KEY (owner_id) REFERENCES users(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_menu_categories_restaurant') THEN
        ALTER TABLE menu_categories ADD CONSTRAINT fk_menu_categories_restaurant
            FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_menu_items_category') THEN
        ALTER TABLE menu_items ADD CONSTRAINT fk_menu_items_category
            FOREIGN KEY (category_id) REFERENCES menu_categories(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_couriers_user') THEN
        ALTER TABLE couriers ADD CONSTRAINT fk_couriers_user
            FOREIGN KEY (user_id) REFERENCES users(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_consumer') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_consumer
            FOREIGN KEY (consumer_id) REFERENCES users(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_restaurant') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_restaurant
            FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_courier') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_courier
            FOREIGN KEY (courier_id) REFERENCES couriers(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_items_order') THEN
        ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order
            FOREIGN KEY (order_id) REFERENCES orders(id) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_order') THEN
        ALTER TABLE payments ADD CONSTRAINT fk_payments_order
            FOREIGN KEY (order_id) REFERENCES orders(id) NOT VALID;
    END IF;
END $$;

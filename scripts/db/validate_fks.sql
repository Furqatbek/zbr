-- Validate the foreign keys that V32 added as NOT VALID, promoting them to fully
-- enforced (they already apply to new/modified rows; VALIDATE also checks the
-- pre-existing rows and clears the "NOT VALID" flag).
--
-- Run check_fk_orphans.sql FIRST and clean any orphans. Each constraint here is
-- validated independently: if one still has violations it is skipped with a
-- WARNING and the rest continue. Safe and idempotent to re-run — VALIDATE on an
-- already-valid constraint is a no-op.
--
-- VALIDATE CONSTRAINT takes a SHARE UPDATE EXCLUSIVE lock (reads and writes to
-- the table continue), so this is safe to run on a live database.
--
--   psql "$DATABASE_URL" -f scripts/db/validate_fks.sql

DO $$
DECLARE
    fk record;
BEGIN
    FOR fk IN
        SELECT * FROM (VALUES
            ('restaurants',     'fk_restaurants_owner'),
            ('menu_categories', 'fk_menu_categories_restaurant'),
            ('menu_items',      'fk_menu_items_category'),
            ('couriers',        'fk_couriers_user'),
            ('orders',          'fk_orders_consumer'),
            ('orders',          'fk_orders_restaurant'),
            ('orders',          'fk_orders_courier'),
            ('order_items',     'fk_order_items_order'),
            ('payments',        'fk_payments_order')
        ) AS t(tbl, con)
    LOOP
        BEGIN
            EXECUTE format('ALTER TABLE %I VALIDATE CONSTRAINT %I', fk.tbl, fk.con);
            RAISE NOTICE 'validated %.%', fk.tbl, fk.con;
        EXCEPTION WHEN others THEN
            RAISE WARNING 'SKIPPED %.% — %', fk.tbl, fk.con, SQLERRM;
        END;
    END LOOP;
END $$;

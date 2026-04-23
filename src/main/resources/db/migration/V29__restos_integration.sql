-- Restos integration: external system fields on restaurants
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS external_system_url VARCHAR(500);
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS external_api_key VARCHAR(255);
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS last_menu_sync_at TIMESTAMP;

-- External ID tracking for menu categories and items (for re-sync)
ALTER TABLE menu_categories ADD COLUMN IF NOT EXISTS external_id BIGINT;
ALTER TABLE menu_categories ADD COLUMN IF NOT EXISTS external_source VARCHAR(50);

ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS external_id BIGINT;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS external_source VARCHAR(50);

-- Unique index: one external ID per source per restaurant/category
CREATE UNIQUE INDEX IF NOT EXISTS idx_menu_categories_external
    ON menu_categories(restaurant_id, external_source, external_id)
    WHERE external_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_menu_items_external
    ON menu_items(category_id, external_source, external_id)
    WHERE external_id IS NOT NULL;

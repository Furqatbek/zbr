-- =====================================================
-- V28: Comprehensive schema fixes for all entity mismatches
-- =====================================================

-- =====================================================
-- RESTAURANTS TABLE
-- =====================================================
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS cover_image_url TEXT;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS average_rating DECIMAL(3,2) DEFAULT 0;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS opens_at TIME;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS closes_at TIME;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS is_open BOOLEAN DEFAULT FALSE;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS total_ratings INTEGER DEFAULT 0;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS total_orders INTEGER DEFAULT 0;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'USD';
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS config_json TEXT;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS average_prep_time_minutes INTEGER;

-- Copy from old columns
UPDATE restaurants SET average_rating = avg_rating WHERE average_rating IS NULL AND avg_rating IS NOT NULL;
UPDATE restaurants SET cover_image_url = banner_url WHERE cover_image_url IS NULL AND banner_url IS NOT NULL;
UPDATE restaurants SET average_prep_time_minutes = avg_prep_time WHERE average_prep_time_minutes IS NULL AND avg_prep_time IS NOT NULL;

-- =====================================================
-- MENU_ITEMS TABLE
-- =====================================================
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS price DECIMAL(10,2);
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS in_stock BOOLEAN DEFAULT TRUE;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;

UPDATE menu_items SET price = base_price WHERE price IS NULL AND base_price IS NOT NULL;
UPDATE menu_items SET in_stock = is_available WHERE in_stock IS NULL AND is_available IS NOT NULL;
UPDATE menu_items SET sort_order = display_order WHERE sort_order IS NULL AND display_order IS NOT NULL;

-- =====================================================
-- MENU_CATEGORIES TABLE
-- =====================================================
ALTER TABLE menu_categories ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE menu_categories ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE menu_categories ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

UPDATE menu_categories SET sort_order = display_order WHERE sort_order IS NULL AND display_order IS NOT NULL;
UPDATE menu_categories SET active = is_active WHERE active IS NULL AND is_active IS NOT NULL;

-- =====================================================
-- ITEM_VARIANTS TABLE
-- =====================================================
ALTER TABLE item_variants ADD COLUMN IF NOT EXISTS price_delta DECIMAL(10,2);
ALTER TABLE item_variants ADD COLUMN IF NOT EXISTS in_stock BOOLEAN DEFAULT TRUE;
ALTER TABLE item_variants ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE item_variants ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE item_variants ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE item_variants SET price_delta = price_adjustment WHERE price_delta IS NULL AND price_adjustment IS NOT NULL;
UPDATE item_variants SET in_stock = is_available WHERE in_stock IS NULL AND is_available IS NOT NULL;
UPDATE item_variants SET sort_order = display_order WHERE sort_order IS NULL AND display_order IS NOT NULL;

-- =====================================================
-- ITEM_OPTIONS TABLE
-- =====================================================
ALTER TABLE item_options ADD COLUMN IF NOT EXISTS group_name VARCHAR(100);
ALTER TABLE item_options ADD COLUMN IF NOT EXISTS price_delta DECIMAL(10,2);
ALTER TABLE item_options ADD COLUMN IF NOT EXISTS in_stock BOOLEAN DEFAULT TRUE;
ALTER TABLE item_options ADD COLUMN IF NOT EXISTS required BOOLEAN DEFAULT FALSE;
ALTER TABLE item_options ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE;
ALTER TABLE item_options ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE item_options ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE item_options ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE item_options SET group_name = option_group WHERE group_name IS NULL AND option_group IS NOT NULL;
UPDATE item_options SET price_delta = price WHERE price_delta IS NULL AND price IS NOT NULL;
UPDATE item_options SET in_stock = is_available WHERE in_stock IS NULL AND is_available IS NOT NULL;
UPDATE item_options SET required = is_required WHERE required IS NULL AND is_required IS NOT NULL;

-- =====================================================
-- ORDERS TABLE
-- =====================================================
ALTER TABLE orders ADD COLUMN IF NOT EXISTS external_order_no VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS consumer_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax DECIMAL(10,2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount DECIMAL(10,2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS total DECIMAL(10,2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_id INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_instructions VARCHAR(500);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_name VARCHAR(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS estimated_prep_time_minutes INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20);

UPDATE orders SET external_order_no = order_number WHERE external_order_no IS NULL AND order_number IS NOT NULL;
UPDATE orders SET consumer_id = customer_id WHERE consumer_id IS NULL AND customer_id IS NOT NULL;
UPDATE orders SET tax = tax_amount WHERE tax IS NULL AND tax_amount IS NOT NULL;
UPDATE orders SET discount = discount_amount WHERE discount IS NULL AND discount_amount IS NOT NULL;
UPDATE orders SET total = total_amount WHERE total IS NULL AND total_amount IS NOT NULL;
UPDATE orders SET delivered_at = actual_delivery_time WHERE delivered_at IS NULL AND actual_delivery_time IS NOT NULL;
UPDATE orders SET notes = customer_notes WHERE notes IS NULL AND customer_notes IS NOT NULL;
UPDATE orders SET table_id = table_number WHERE table_id IS NULL AND table_number IS NOT NULL;

-- =====================================================
-- ORDER_ITEMS TABLE
-- =====================================================
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS variant_id BIGINT;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS variant_name VARCHAR(100);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS variant_price_delta DECIMAL(10,2);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS modifiers_json TEXT;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS modifiers_total DECIMAL(10,2);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS special_instructions VARCHAR(500);

UPDATE order_items SET modifiers_json = options WHERE modifiers_json IS NULL AND options IS NOT NULL;

-- =====================================================
-- PAYMENTS TABLE
-- =====================================================
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider_payment_id VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS error_message TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider VARCHAR(50);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS error_code VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_reason VARCHAR(500);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;

UPDATE payments SET provider_payment_id = external_payment_id WHERE provider_payment_id IS NULL AND external_payment_id IS NOT NULL;
UPDATE payments SET error_message = failure_reason WHERE error_message IS NULL AND failure_reason IS NOT NULL;

-- =====================================================
-- REFRESH_TOKENS TABLE
-- =====================================================
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_reason VARCHAR(255);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;

-- =====================================================
-- REFERRALS TABLE
-- =====================================================
ALTER TABLE referrals ADD COLUMN IF NOT EXISTS referrer_user_id BIGINT;
ALTER TABLE referrals ADD COLUMN IF NOT EXISTS reward_amount DECIMAL(10,2);
ALTER TABLE referrals ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'USD';
ALTER TABLE referrals ADD COLUMN IF NOT EXISTS referrer_rewarded BOOLEAN DEFAULT FALSE;
ALTER TABLE referrals ADD COLUMN IF NOT EXISTS referred_rewarded BOOLEAN DEFAULT FALSE;
ALTER TABLE referrals ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

UPDATE referrals SET referrer_user_id = referrer_id WHERE referrer_user_id IS NULL AND referrer_id IS NOT NULL;
UPDATE referrals SET reward_amount = referrer_reward WHERE reward_amount IS NULL AND referrer_reward IS NOT NULL;

-- =====================================================
-- SYSTEM_HEALTH_SNAPSHOTS TABLE
-- =====================================================
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS health_status VARCHAR(20);
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS api_latency_p50 INTEGER;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS api_latency_p90 INTEGER;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS api_latency_p99 INTEGER;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS db_active_connections INTEGER;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS db_idle_connections INTEGER;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS db_waiting_connections INTEGER;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS redis_latency_ms INTEGER;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS redis_memory_used_mb INTEGER;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS redis_connected_clients INTEGER;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS queue_backlog BIGINT;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS queue_processing_rate DOUBLE PRECISION;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS cpu_usage_percent DOUBLE PRECISION;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS memory_usage_percent DOUBLE PRECISION;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS disk_usage_percent DOUBLE PRECISION;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS error_rate_percent DOUBLE PRECISION;
ALTER TABLE system_health_snapshots ADD COLUMN IF NOT EXISTS requests_per_second DOUBLE PRECISION;

UPDATE system_health_snapshots SET health_status = status WHERE health_status IS NULL AND status IS NOT NULL;

-- =====================================================
-- NOTIFICATION_TEMPLATES TABLE
-- =====================================================
ALTER TABLE notification_templates ADD COLUMN IF NOT EXISTS default_ttl_hours INTEGER;

UPDATE notification_templates SET default_ttl_hours = ttl_hours WHERE default_ttl_hours IS NULL AND ttl_hours IS NOT NULL;

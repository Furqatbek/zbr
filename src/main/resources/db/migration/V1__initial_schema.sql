-- Food Delivery Platform - Initial Schema
-- Flyway Migration V1
-- All core tables matching JPA entities exactly

-- Users table (matching User entity)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    profile_image_url VARCHAR(255),
    address VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    role VARCHAR(20) NOT NULL DEFAULT 'CONSUMER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_status ON users(status);

-- User roles collection table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Refresh tokens table (matching RefreshToken entity)
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(500),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- OTP codes table (matching OtpCode entity)
CREATE TABLE otp_codes (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    purpose VARCHAR(20) NOT NULL DEFAULT 'LOGIN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_phone ON otp_codes(phone);
CREATE INDEX idx_otp_phone_code ON otp_codes(phone, code);
CREATE INDEX idx_otp_expires_at ON otp_codes(expires_at);

-- Password reset tokens table (matching PasswordResetToken entity)
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);

-- Restaurants table (matching Restaurant entity)
CREATE TABLE restaurants (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(250) UNIQUE,
    description VARCHAR(1000),
    logo_url VARCHAR(255),
    cover_image_url VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_featured BOOLEAN DEFAULT FALSE,
    accepts_delivery BOOLEAN DEFAULT TRUE,
    accepts_takeaway BOOLEAN DEFAULT TRUE,
    accepts_dine_in BOOLEAN DEFAULT FALSE,
    minimum_order DECIMAL(10, 2) DEFAULT 0,
    delivery_fee DECIMAL(10, 2) DEFAULT 0,
    delivery_radius_km INTEGER DEFAULT 10,
    average_prep_time_minutes INTEGER DEFAULT 30,
    opens_at TIME,
    closes_at TIME,
    is_open BOOLEAN DEFAULT FALSE,
    commission_rate DECIMAL(5, 2) DEFAULT 15.00,
    currency VARCHAR(10) DEFAULT 'UZS',
    average_rating DECIMAL(3, 2) DEFAULT 0,
    total_ratings INTEGER DEFAULT 0,
    total_orders INTEGER DEFAULT 0,
    config_json TEXT,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_restaurants_owner ON restaurants(owner_id);
CREATE INDEX idx_restaurants_status ON restaurants(status);
CREATE INDEX idx_restaurants_slug ON restaurants(slug);

-- Menu categories (matching MenuCategory entity)
CREATE TABLE menu_categories (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    sort_order INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_menu_categories_restaurant ON menu_categories(restaurant_id);
CREATE INDEX idx_menu_categories_active ON menu_categories(active);

-- Menu items (matching MenuItem entity)
CREATE TABLE menu_items (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES menu_categories(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10, 2) NOT NULL,
    price_with_margin DECIMAL(10, 2),
    original_price DECIMAL(10, 2),
    image_url VARCHAR(500),
    image_path VARCHAR(500),
    image_name VARCHAR(255),
    image_size BIGINT,
    image_content_type VARCHAR(100),
    in_stock BOOLEAN NOT NULL DEFAULT TRUE,
    featured BOOLEAN DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0,
    prep_time_minutes INTEGER,
    calories INTEGER,
    is_vegetarian BOOLEAN DEFAULT FALSE,
    is_vegan BOOLEAN DEFAULT FALSE,
    is_gluten_free BOOLEAN DEFAULT FALSE,
    is_spicy BOOLEAN DEFAULT FALSE,
    allergens VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_menu_items_category ON menu_items(category_id);
CREATE INDEX idx_menu_items_in_stock ON menu_items(in_stock);
CREATE INDEX idx_menu_items_featured ON menu_items(featured);

-- Item variants (matching ItemVariant entity)
CREATE TABLE item_variants (
    id BIGSERIAL PRIMARY KEY,
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    price_delta DECIMAL(10, 2) NOT NULL DEFAULT 0,
    in_stock BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_item_variants_menu_item ON item_variants(menu_item_id);

-- Item options (matching ItemOption entity)
CREATE TABLE item_options (
    id BIGSERIAL PRIMARY KEY,
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    group_name VARCHAR(100),
    name VARCHAR(100) NOT NULL,
    price_delta DECIMAL(10, 2) NOT NULL DEFAULT 0,
    is_default BOOLEAN DEFAULT FALSE,
    max_selections INTEGER DEFAULT 1,
    is_required BOOLEAN DEFAULT FALSE,
    in_stock BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_item_options_menu_item ON item_options(menu_item_id);

-- Couriers table (matching Courier entity)
CREATE TABLE couriers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    vehicle_type VARCHAR(20) NOT NULL DEFAULT 'BICYCLE',
    vehicle_number VARCHAR(50),
    license_number VARCHAR(50),
    current_lat DECIMAL(10, 7),
    current_lng DECIMAL(10, 7),
    location_updated_at TIMESTAMP,
    total_deliveries INTEGER DEFAULT 0,
    average_rating DECIMAL(3, 2) DEFAULT 0,
    total_ratings INTEGER DEFAULT 0,
    acceptance_rate DECIMAL(5, 2) DEFAULT 100.00,
    total_earnings DECIMAL(12, 2) DEFAULT 0,
    max_concurrent_orders INTEGER DEFAULT 3,
    current_order_count INTEGER DEFAULT 0,
    preferred_radius_km INTEGER DEFAULT 5,
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    documents_submitted BOOLEAN DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_couriers_user ON couriers(user_id);
CREATE INDEX idx_couriers_status ON couriers(status);
CREATE INDEX idx_couriers_location ON couriers(current_lat, current_lng);

-- Orders table (matching Order entity)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    external_order_no VARCHAR(50) NOT NULL UNIQUE,
    consumer_id BIGINT NOT NULL REFERENCES users(id),
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id),
    courier_id BIGINT REFERENCES couriers(id),
    order_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0,
    tax DECIMAL(10, 2) DEFAULT 0,
    delivery_fee DECIMAL(10, 2) DEFAULT 0,
    discount DECIMAL(10, 2) DEFAULT 0,
    tip_amount DECIMAL(10, 2) DEFAULT 0,
    total DECIMAL(10, 2) NOT NULL DEFAULT 0,
    delivery_address VARCHAR(500),
    delivery_latitude DECIMAL(10, 7),
    delivery_longitude DECIMAL(10, 7),
    delivery_instructions VARCHAR(500),
    table_id VARCHAR(255),
    customer_name VARCHAR(200),
    customer_phone VARCHAR(50),
    notes VARCHAR(1000),
    estimated_prep_time_minutes INTEGER,
    estimated_delivery_time TIMESTAMP,
    accepted_at TIMESTAMP,
    preparing_at TIMESTAMP,
    ready_at TIMESTAMP,
    picked_up_at TIMESTAMP,
    delivered_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    rejected_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    consumer_rating INTEGER,
    consumer_review VARCHAR(1000),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_consumer ON orders(consumer_id);
CREATE INDEX idx_orders_restaurant ON orders(restaurant_id);
CREATE INDEX idx_orders_courier ON orders(courier_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_external_no ON orders(external_order_no);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- Order items (matching OrderItem entity)
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
    item_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    variant_id BIGINT,
    variant_name VARCHAR(100),
    variant_price_delta DECIMAL(10, 2) DEFAULT 0,
    modifiers_json TEXT,
    modifiers_total DECIMAL(10, 2) DEFAULT 0,
    special_instructions VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_menu_item ON order_items(menu_item_id);

-- Payments table (matching Payment entity)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    provider VARCHAR(50) NOT NULL,
    provider_payment_id VARCHAR(255),
    payment_intent_id VARCHAR(255),
    payment_method VARCHAR(50),
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    raw_response TEXT,
    error_code VARCHAR(100),
    error_message VARCHAR(500),
    refund_amount DECIMAL(10, 2),
    refund_reason VARCHAR(500),
    refunded_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_provider_id ON payments(provider_payment_id);
CREATE INDEX idx_payments_status ON payments(status);

-- Referrals table (matching Referral entity)
CREATE TABLE referrals (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    referrer_user_id BIGINT NOT NULL REFERENCES users(id),
    referred_user_id BIGINT REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reward_amount DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    referrer_rewarded BOOLEAN DEFAULT FALSE,
    referred_rewarded BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP,
    completed_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_referrals_code ON referrals(code);
CREATE INDEX idx_referrals_referrer ON referrals(referrer_user_id);
CREATE INDEX idx_referrals_referred ON referrals(referred_user_id);
CREATE INDEX idx_referrals_status ON referrals(status);

-- Activity logs table
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL,
    restaurant_id BIGINT REFERENCES restaurants(id) ON DELETE SET NULL,
    order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    menu_item_id BIGINT,
    session_id VARCHAR(100),
    device_type VARCHAR(20),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    referral_source VARCHAR(100),
    metadata TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_event_type ON activity_logs(event_type);
CREATE INDEX idx_activity_created_at ON activity_logs(created_at);

-- Functions for updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at triggers
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_restaurants_updated_at BEFORE UPDATE ON restaurants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_menu_categories_updated_at BEFORE UPDATE ON menu_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_menu_items_updated_at BEFORE UPDATE ON menu_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_couriers_updated_at BEFORE UPDATE ON couriers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_referrals_updated_at BEFORE UPDATE ON referrals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE users IS 'Platform users';
COMMENT ON TABLE restaurants IS 'Restaurant profiles';
COMMENT ON TABLE menu_categories IS 'Menu categories for organizing items';
COMMENT ON TABLE menu_items IS 'Menu items/dishes';
COMMENT ON TABLE item_variants IS 'Menu item size/type variations';
COMMENT ON TABLE item_options IS 'Menu item add-ons and modifiers';
COMMENT ON TABLE couriers IS 'Delivery couriers';
COMMENT ON TABLE orders IS 'Customer orders';
COMMENT ON TABLE order_items IS 'Individual items in orders';
COMMENT ON TABLE payments IS 'Payment transactions';
COMMENT ON TABLE referrals IS 'Referral program tracking';

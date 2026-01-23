-- Promo codes table for promotional discounts
CREATE TABLE promo_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    min_order_amount DECIMAL(10, 2),
    max_discount_amount DECIMAL(10, 2),
    restaurant_id BIGINT,
    usage_limit INTEGER,
    usage_count INTEGER DEFAULT 0,
    user_usage_limit INTEGER DEFAULT 1,
    starts_at TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promo_codes_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

-- Indexes
CREATE INDEX idx_promo_codes_code ON promo_codes(code);
CREATE INDEX idx_promo_codes_restaurant_id ON promo_codes(restaurant_id);

-- Sample promo codes
INSERT INTO promo_codes (code, description, discount_type, discount_value, min_order_amount, max_discount_amount, starts_at, expires_at, usage_limit)
VALUES
    ('WELCOME10', 'Welcome discount - 10% off', 'PERCENTAGE', 10.00, 15.00, 20.00, NOW(), NOW() + INTERVAL '1 year', 1000),
    ('SAVE5', 'Save $5 on your order', 'FIXED', 5.00, 25.00, NULL, NOW(), NOW() + INTERVAL '6 months', 500);

-- Comment
COMMENT ON TABLE promo_codes IS 'Promotional discount codes';

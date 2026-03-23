-- Delivery fee settings table for dynamic configuration
CREATE TABLE IF NOT EXISTS delivery_fee_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value DECIMAL(10, 2) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default delivery fee settings
INSERT INTO delivery_fee_settings (setting_key, setting_value, description) VALUES
    ('BASE_FEE', 2.00, 'Base delivery fee applied to all orders'),
    ('PER_KM_FEE', 0.50, 'Fee charged per kilometer of delivery distance'),
    ('MIN_FEE', 2.00, 'Minimum delivery fee regardless of distance'),
    ('MAX_FEE', 15.00, 'Maximum delivery fee cap'),
    ('PEAK_HOUR_SURCHARGE', 1.50, 'Additional fee during peak hours'),
    ('PEAK_START_HOUR', 11.00, 'Lunch peak period start hour (24h format)'),
    ('PEAK_END_HOUR', 14.00, 'Lunch peak period end hour (24h format)'),
    ('EVENING_PEAK_START_HOUR', 18.00, 'Dinner peak period start hour (24h format)'),
    ('EVENING_PEAK_END_HOUR', 21.00, 'Dinner peak period end hour (24h format)')
ON CONFLICT (setting_key) DO NOTHING;

-- Create index on setting_key for fast lookups
CREATE INDEX IF NOT EXISTS idx_delivery_fee_settings_key ON delivery_fee_settings(setting_key);

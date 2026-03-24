-- Add string_value column for settings that need string values (e.g., URLs)
ALTER TABLE delivery_fee_settings ADD COLUMN IF NOT EXISTS string_value VARCHAR(500);

-- Insert default routing settings
INSERT INTO delivery_fee_settings (setting_key, setting_value, string_value, description) VALUES
    ('ROUTING_ENABLED', 0.00, NULL, 'Whether route-based distance calculation is enabled (1=enabled, 0=disabled)'),
    ('ROUTING_OSRM_BASE_URL', 0.00, 'https://router.project-osrm.org', 'Base URL of the OSRM routing server'),
    ('ROUTING_CONNECT_TIMEOUT', 3000.00, NULL, 'OSRM connection timeout in milliseconds'),
    ('ROUTING_READ_TIMEOUT', 5000.00, NULL, 'OSRM read timeout in milliseconds')
ON CONFLICT (setting_key) DO NOTHING;

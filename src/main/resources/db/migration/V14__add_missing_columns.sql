-- V14: Add missing columns and triggers
-- Fixes identified during code review

-- Add in_transit_at timestamp to orders table for tracking when order enters IN_TRANSIT status
ALTER TABLE orders ADD COLUMN IF NOT EXISTS in_transit_at TIMESTAMP;

-- Create index for in_transit_at
CREATE INDEX IF NOT EXISTS idx_orders_in_transit_at ON orders(in_transit_at);

-- Add updated_at trigger for sms_templates table (was missing)
CREATE TRIGGER IF NOT EXISTS update_sms_templates_updated_at
    BEFORE UPDATE ON sms_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comment for documentation
COMMENT ON COLUMN orders.in_transit_at IS 'Timestamp when order status changed to IN_TRANSIT';

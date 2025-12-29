-- ============================================================================
-- V6: Operations Analytics Tables
-- Creates tables for tracking operational metrics:
--   - Courier location events
--   - Courier order events (offers, accepts, rejects)
--   - Courier availability events
--   - Restaurant order events
--   - Restaurant online status
--   - Menu update history
--   - ETA history for accuracy tracking
-- ============================================================================

-- Drop existing tables to handle schema changes from partial runs
DROP TABLE IF EXISTS eta_history CASCADE;
DROP TABLE IF EXISTS menu_update_history CASCADE;
DROP TABLE IF EXISTS restaurant_online_status CASCADE;
DROP TABLE IF EXISTS restaurant_order_events CASCADE;
DROP TABLE IF EXISTS courier_availability_events CASCADE;
DROP TABLE IF EXISTS courier_order_events CASCADE;
DROP TABLE IF EXISTS courier_location_events CASCADE;

-- ============================================================================
-- Courier Location Events
-- Tracks courier GPS location updates for analytics
-- ============================================================================
CREATE TABLE courier_location_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    accuracy_meters DOUBLE PRECISION,
    speed_kmh DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    altitude_meters DOUBLE PRECISION,
    battery_level INTEGER,
    is_moving BOOLEAN DEFAULT FALSE,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cle_courier FOREIGN KEY (courier_id) REFERENCES couriers(id) ON DELETE CASCADE
);

-- Indexes for courier location events
CREATE INDEX idx_cle_courier_id ON courier_location_events(courier_id);
CREATE INDEX idx_cle_recorded_at ON courier_location_events(recorded_at);
CREATE INDEX idx_cle_courier_recorded ON courier_location_events(courier_id, recorded_at);

-- ============================================================================
-- Courier Order Events
-- Tracks courier responses to order offers (accept/reject/timeout)
-- ============================================================================
CREATE TABLE courier_order_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    response_time_seconds BIGINT,
    distance_to_restaurant_km DOUBLE PRECISION,
    distance_to_customer_km DOUBLE PRECISION,
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_coe_courier FOREIGN KEY (courier_id) REFERENCES couriers(id) ON DELETE CASCADE,
    CONSTRAINT fk_coe_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_coe_event_type CHECK (event_type IN ('OFFERED', 'ACCEPTED', 'REJECTED', 'TIMED_OUT', 'CANCELLED', 'PICKED_UP', 'DELIVERED', 'REASSIGNED'))
);

-- Indexes for courier order events
CREATE INDEX idx_coe_courier_id ON courier_order_events(courier_id);
CREATE INDEX idx_coe_order_id ON courier_order_events(order_id);
CREATE INDEX idx_coe_event_type ON courier_order_events(event_type);
CREATE INDEX idx_coe_timestamp ON courier_order_events(event_timestamp);
CREATE INDEX idx_coe_courier_timestamp ON courier_order_events(courier_id, event_timestamp);

-- ============================================================================
-- Courier Availability Events
-- Tracks courier online/offline/busy status changes
-- ============================================================================
CREATE TABLE courier_availability_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20),
    reason VARCHAR(500),
    status_changed_at TIMESTAMP NOT NULL,
    previous_status_duration_minutes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cae_courier FOREIGN KEY (courier_id) REFERENCES couriers(id) ON DELETE CASCADE,
    CONSTRAINT chk_cae_status CHECK (status IN ('AVAILABLE', 'BUSY', 'OFFLINE', 'ON_BREAK', 'RETURNING'))
);

-- Indexes for courier availability events
CREATE INDEX idx_cae_courier_id ON courier_availability_events(courier_id);
CREATE INDEX idx_cae_status_changed_at ON courier_availability_events(status_changed_at);
CREATE INDEX idx_cae_status ON courier_availability_events(status);
CREATE INDEX idx_cae_courier_changed ON courier_availability_events(courier_id, status_changed_at);

-- ============================================================================
-- Restaurant Order Events
-- Tracks restaurant order handling (receive, accept, prepare, ready)
-- ============================================================================
CREATE TABLE restaurant_order_events (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    estimated_prep_minutes INTEGER,
    actual_prep_minutes INTEGER,
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_roe_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    CONSTRAINT fk_roe_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_roe_event_type CHECK (event_type IN ('RECEIVED', 'ACCEPTED', 'REJECTED', 'PREPARING_STARTED', 'READY', 'CANCELLED', 'WENT_OFFLINE', 'PREP_TIME_EXTENDED'))
);

-- Indexes for restaurant order events
CREATE INDEX idx_roe_restaurant_id ON restaurant_order_events(restaurant_id);
CREATE INDEX idx_roe_order_id ON restaurant_order_events(order_id);
CREATE INDEX idx_roe_event_type ON restaurant_order_events(event_type);
CREATE INDEX idx_roe_timestamp ON restaurant_order_events(event_timestamp);
CREATE INDEX idx_roe_restaurant_timestamp ON restaurant_order_events(restaurant_id, event_timestamp);

-- ============================================================================
-- Restaurant Online Status
-- Tracks restaurant online/offline status changes for uptime metrics
-- ============================================================================
CREATE TABLE restaurant_online_status (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    is_online BOOLEAN NOT NULL,
    reason VARCHAR(500),
    status_changed_at TIMESTAMP NOT NULL,
    previous_status_duration_minutes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ros_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);

-- Indexes for restaurant online status
CREATE INDEX idx_ros_restaurant_id ON restaurant_online_status(restaurant_id);
CREATE INDEX idx_ros_status_changed_at ON restaurant_online_status(status_changed_at);
CREATE INDEX idx_ros_restaurant_changed ON restaurant_online_status(restaurant_id, status_changed_at);

-- ============================================================================
-- Menu Update History
-- Tracks menu changes for update frequency metrics
-- ============================================================================
CREATE TABLE menu_update_history (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    update_type VARCHAR(30) NOT NULL,
    items_added INTEGER DEFAULT 0,
    items_removed INTEGER DEFAULT 0,
    items_modified INTEGER DEFAULT 0,
    categories_changed INTEGER DEFAULT 0,
    prices_updated INTEGER DEFAULT 0,
    updated_by_user_id BIGINT,
    updated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_muh_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    CONSTRAINT fk_muh_user FOREIGN KEY (updated_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_muh_update_type CHECK (update_type IN ('ITEM_ADDED', 'ITEM_REMOVED', 'ITEM_MODIFIED', 'PRICE_CHANGED', 'AVAILABILITY_CHANGED', 'CATEGORY_CHANGED', 'BULK_UPDATE', 'MENU_IMPORTED'))
);

-- Indexes for menu update history
CREATE INDEX idx_muh_restaurant_id ON menu_update_history(restaurant_id);
CREATE INDEX idx_muh_updated_at ON menu_update_history(updated_at);
CREATE INDEX idx_muh_restaurant_updated ON menu_update_history(restaurant_id, updated_at);

-- ============================================================================
-- ETA History
-- Tracks ETA predictions vs actual delivery for accuracy metrics
-- ============================================================================
CREATE TABLE eta_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    courier_id BIGINT,

    -- ETA shown to customer
    eta_shown_minutes INTEGER NOT NULL,
    predicted_delivery_time TIMESTAMP NOT NULL,
    predicted_at TIMESTAMP NOT NULL,

    -- Actual delivery
    actual_delivered_at TIMESTAMP,
    actual_delivery_minutes INTEGER,

    -- Error calculations
    eta_error_minutes INTEGER,
    eta_error_abs_minutes INTEGER,
    was_late BOOLEAN,
    was_completed BOOLEAN DEFAULT FALSE,

    -- Phase breakdown
    eta_prep_minutes INTEGER,
    actual_prep_minutes INTEGER,
    eta_pickup_minutes INTEGER,
    actual_pickup_minutes INTEGER,
    eta_delivery_minutes INTEGER,
    actual_travel_minutes INTEGER,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_eta_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_eta_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    CONSTRAINT fk_eta_courier FOREIGN KEY (courier_id) REFERENCES couriers(id) ON DELETE SET NULL
);

-- Indexes for ETA history
CREATE INDEX idx_eta_order_id ON eta_history(order_id);
CREATE INDEX idx_eta_restaurant_id ON eta_history(restaurant_id);
CREATE INDEX idx_eta_courier_id ON eta_history(courier_id);
CREATE INDEX idx_eta_predicted_at ON eta_history(predicted_at);
CREATE INDEX idx_eta_delivered_at ON eta_history(actual_delivered_at);
CREATE INDEX idx_eta_completed ON eta_history(was_completed, predicted_at);

-- ============================================================================
-- Composite indexes for common analytics queries
-- ============================================================================

-- For order fulfillment analysis
CREATE INDEX IF NOT EXISTS idx_orders_restaurant_created ON orders(restaurant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_courier_created ON orders(courier_id, created_at);

-- Comments for documentation
COMMENT ON TABLE courier_location_events IS 'Tracks courier GPS location updates for location frequency and movement analytics';
COMMENT ON TABLE courier_order_events IS 'Tracks courier responses to order offers for acceptance rate and response time metrics';
COMMENT ON TABLE courier_availability_events IS 'Tracks courier availability status changes for utilization metrics';
COMMENT ON TABLE restaurant_order_events IS 'Tracks restaurant order handling events for preparation time and acceptance metrics';
COMMENT ON TABLE restaurant_online_status IS 'Tracks restaurant online/offline status for uptime metrics';
COMMENT ON TABLE menu_update_history IS 'Tracks menu changes for update frequency metrics';
COMMENT ON TABLE eta_history IS 'Tracks ETA predictions vs actual delivery for accuracy metrics';

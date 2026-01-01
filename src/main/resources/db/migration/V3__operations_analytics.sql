-- Operations Analytics Tables
-- V3: Courier and restaurant operational metrics

-- Courier Location Events
CREATE TABLE courier_location_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL REFERENCES couriers(id) ON DELETE CASCADE,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    accuracy_meters DOUBLE PRECISION,
    speed_kmh DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    altitude_meters DOUBLE PRECISION,
    battery_level INTEGER,
    is_moving BOOLEAN DEFAULT FALSE,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cle_courier_id ON courier_location_events(courier_id);
CREATE INDEX idx_cle_recorded_at ON courier_location_events(recorded_at);
CREATE INDEX idx_cle_courier_recorded ON courier_location_events(courier_id, recorded_at);

-- Courier Order Events
CREATE TABLE courier_order_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL REFERENCES couriers(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    event_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    response_time_seconds BIGINT,
    distance_to_restaurant_km DOUBLE PRECISION,
    distance_to_customer_km DOUBLE PRECISION,
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_coe_event_type CHECK (event_type IN ('OFFERED', 'ACCEPTED', 'REJECTED', 'TIMED_OUT', 'CANCELLED', 'PICKED_UP', 'DELIVERED', 'REASSIGNED'))
);

CREATE INDEX idx_coe_courier_id ON courier_order_events(courier_id);
CREATE INDEX idx_coe_order_id ON courier_order_events(order_id);
CREATE INDEX idx_coe_event_type ON courier_order_events(event_type);
CREATE INDEX idx_coe_timestamp ON courier_order_events(event_timestamp);

-- Courier Availability Events
CREATE TABLE courier_availability_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL REFERENCES couriers(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20),
    reason VARCHAR(500),
    status_changed_at TIMESTAMP NOT NULL,
    previous_status_duration_minutes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cae_status CHECK (status IN ('AVAILABLE', 'BUSY', 'OFFLINE', 'ON_BREAK', 'RETURNING'))
);

CREATE INDEX idx_cae_courier_id ON courier_availability_events(courier_id);
CREATE INDEX idx_cae_status_changed_at ON courier_availability_events(status_changed_at);
CREATE INDEX idx_cae_status ON courier_availability_events(status);

-- Restaurant Order Events
CREATE TABLE restaurant_order_events (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    estimated_prep_minutes INTEGER,
    actual_prep_minutes INTEGER,
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_roe_event_type CHECK (event_type IN ('RECEIVED', 'ACCEPTED', 'REJECTED', 'PREPARING_STARTED', 'READY', 'CANCELLED', 'WENT_OFFLINE', 'PREP_TIME_EXTENDED'))
);

CREATE INDEX idx_roe_restaurant_id ON restaurant_order_events(restaurant_id);
CREATE INDEX idx_roe_order_id ON restaurant_order_events(order_id);
CREATE INDEX idx_roe_event_type ON restaurant_order_events(event_type);
CREATE INDEX idx_roe_timestamp ON restaurant_order_events(event_timestamp);

-- Restaurant Online Status
CREATE TABLE restaurant_online_status (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    is_online BOOLEAN NOT NULL,
    reason VARCHAR(500),
    status_changed_at TIMESTAMP NOT NULL,
    previous_status_duration_minutes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ros_restaurant_id ON restaurant_online_status(restaurant_id);
CREATE INDEX idx_ros_status_changed_at ON restaurant_online_status(status_changed_at);

-- Menu Update History
CREATE TABLE menu_update_history (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    update_type VARCHAR(30) NOT NULL,
    items_added INTEGER DEFAULT 0,
    items_removed INTEGER DEFAULT 0,
    items_modified INTEGER DEFAULT 0,
    categories_changed INTEGER DEFAULT 0,
    prices_updated INTEGER DEFAULT 0,
    updated_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_muh_update_type CHECK (update_type IN ('ITEM_ADDED', 'ITEM_REMOVED', 'ITEM_MODIFIED', 'PRICE_CHANGED', 'AVAILABILITY_CHANGED', 'CATEGORY_CHANGED', 'BULK_UPDATE', 'MENU_IMPORTED'))
);

CREATE INDEX idx_muh_restaurant_id ON menu_update_history(restaurant_id);
CREATE INDEX idx_muh_updated_at ON menu_update_history(updated_at);

-- ETA History
CREATE TABLE eta_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    courier_id BIGINT REFERENCES couriers(id) ON DELETE SET NULL,
    eta_shown_minutes INTEGER NOT NULL,
    predicted_delivery_time TIMESTAMP NOT NULL,
    predicted_at TIMESTAMP NOT NULL,
    actual_delivered_at TIMESTAMP,
    actual_delivery_minutes INTEGER,
    eta_error_minutes INTEGER,
    eta_error_abs_minutes INTEGER,
    was_late BOOLEAN,
    was_completed BOOLEAN DEFAULT FALSE,
    eta_prep_minutes INTEGER,
    actual_prep_minutes INTEGER,
    eta_pickup_minutes INTEGER,
    actual_pickup_minutes INTEGER,
    eta_delivery_minutes INTEGER,
    actual_travel_minutes INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_eta_order_id ON eta_history(order_id);
CREATE INDEX idx_eta_restaurant_id ON eta_history(restaurant_id);
CREATE INDEX idx_eta_courier_id ON eta_history(courier_id);
CREATE INDEX idx_eta_predicted_at ON eta_history(predicted_at);

COMMENT ON TABLE courier_location_events IS 'Tracks courier GPS location updates';
COMMENT ON TABLE courier_order_events IS 'Tracks courier responses to order offers';
COMMENT ON TABLE courier_availability_events IS 'Tracks courier availability status changes';
COMMENT ON TABLE restaurant_order_events IS 'Tracks restaurant order handling events';
COMMENT ON TABLE restaurant_online_status IS 'Tracks restaurant online/offline status';
COMMENT ON TABLE menu_update_history IS 'Tracks menu changes';
COMMENT ON TABLE eta_history IS 'Tracks ETA predictions vs actual delivery';

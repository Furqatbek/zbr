-- Self-Service Support and Proof of Delivery System
-- V26: Customer self-service resolution and delivery verification

-- Self-service resolution history
CREATE TABLE self_service_resolutions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    resolution_type VARCHAR(50) NOT NULL,
    resolution_details JSONB,
    amount DECIMAL(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ssr_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ssr_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Indexes for self-service resolutions
CREATE INDEX idx_ssr_user_id ON self_service_resolutions(user_id);
CREATE INDEX idx_ssr_order_id ON self_service_resolutions(order_id);
CREATE INDEX idx_ssr_created_at ON self_service_resolutions(created_at);

-- Delivery proof table
CREATE TABLE delivery_proofs (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    courier_id BIGINT NOT NULL,
    photo_url VARCHAR(500),
    photo_hash VARCHAR(64),
    signature_url VARCHAR(500),
    delivery_latitude DECIMAL(10, 7),
    delivery_longitude DECIMAL(10, 7),
    recipient_name VARCHAR(200),
    delivery_notes VARCHAR(500),
    verified BOOLEAN DEFAULT false,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dp_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_dp_courier FOREIGN KEY (courier_id) REFERENCES couriers(id)
);

-- Indexes for delivery proofs
CREATE INDEX idx_dp_order_id ON delivery_proofs(order_id);
CREATE INDEX idx_dp_courier_id ON delivery_proofs(courier_id);
CREATE INDEX idx_dp_created_at ON delivery_proofs(created_at);

-- Self-service thresholds configuration
CREATE TABLE self_service_thresholds (
    id BIGSERIAL PRIMARY KEY,
    resolution_type VARCHAR(50) NOT NULL UNIQUE,
    max_auto_refund DECIMAL(10,2),
    max_daily_resolutions INT DEFAULT 3,
    require_photo BOOLEAN DEFAULT false,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default thresholds
INSERT INTO self_service_thresholds (resolution_type, max_auto_refund, max_daily_resolutions, require_photo) VALUES
('AUTO_REFUND_SMALL', 10.00, 3, false),
('MISSING_ITEMS', NULL, 2, true),
('CANCEL_PRE_PREP', NULL, 5, false),
('LATE_DELIVERY_CREDIT', NULL, 3, false);

-- Compensation System Tables
-- V23: Automated compensation for delivery issues, late orders, etc.

-- Compensation rules table for defining automatic compensation triggers
CREATE TABLE compensation_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    trigger_type VARCHAR(50) NOT NULL,
    condition_json JSONB NOT NULL,
    compensation_type VARCHAR(30) NOT NULL,
    compensation_value DECIMAL(10,2),
    compensation_percent DECIMAL(5,2),
    max_compensation DECIMAL(10,2),
    priority INT DEFAULT 0,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Customer credits table for tracking credit balances
CREATE TABLE customer_credits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(10,2) NOT NULL DEFAULT 0,
    lifetime_earned DECIMAL(10,2) NOT NULL DEFAULT 0,
    lifetime_used DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cc_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Compensation logs for audit trail
CREATE TABLE compensation_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    trigger_type VARCHAR(50) NOT NULL,
    compensation_type VARCHAR(30) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(500),
    rule_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cl_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_cl_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_cl_rule FOREIGN KEY (rule_id) REFERENCES compensation_rules(id)
);

-- Indexes for performance
CREATE INDEX idx_cr_trigger_type ON compensation_rules(trigger_type);
CREATE INDEX idx_cr_active ON compensation_rules(active);
CREATE INDEX idx_cc_user_id ON customer_credits(user_id);
CREATE INDEX idx_cl_user_id ON compensation_logs(user_id);
CREATE INDEX idx_cl_order_id ON compensation_logs(order_id);
CREATE INDEX idx_cl_status ON compensation_logs(status);
CREATE INDEX idx_cl_created_at ON compensation_logs(created_at);

-- Insert default compensation rules
INSERT INTO compensation_rules (name, trigger_type, condition_json, compensation_type, compensation_percent, max_compensation, priority, description) VALUES
('Late 30-45 min', 'LATE_DELIVERY', '{"minDelayMinutes": 30, "maxDelayMinutes": 45}', 'CREDIT', 10.00, 50.00, 1, 'Order delivered 30-45 minutes late'),
('Late 45-60 min', 'LATE_DELIVERY', '{"minDelayMinutes": 45, "maxDelayMinutes": 60}', 'CREDIT', 20.00, 100.00, 2, 'Order delivered 45-60 minutes late'),
('Late >60 min', 'LATE_DELIVERY', '{"minDelayMinutes": 60}', 'CREDIT_AND_FREE_DELIVERY', 30.00, 150.00, 3, 'Order delivered more than 60 minutes late'),
('Missing Items', 'MISSING_ITEMS', '{}', 'REFUND', NULL, NULL, 1, 'Refund for missing item value'),
('Cancelled After Prep', 'CANCELLED_AFTER_PREP', '{}', 'REFUND_AND_CREDIT', 10.00, 50.00, 1, 'Order cancelled after restaurant started preparation'),
('Restaurant Rejection', 'RESTAURANT_REJECTION', '{}', 'REFUND_AND_CREDIT', 15.00, 75.00, 1, 'Restaurant rejected order after customer paid');

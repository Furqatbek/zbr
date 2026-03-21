-- Courier Reassignment System
-- V24: Support for reassigning orders to different couriers

-- Add reassignment tracking columns to orders
ALTER TABLE orders ADD COLUMN reassignment_count INT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN previous_courier_id BIGINT;

-- Add foreign key for previous courier
ALTER TABLE orders ADD CONSTRAINT fk_orders_previous_courier
    FOREIGN KEY (previous_courier_id) REFERENCES couriers(id);

-- Create index for finding orders that have been reassigned
CREATE INDEX idx_orders_reassignment_count ON orders(reassignment_count);

-- Reassignment logs for audit trail
CREATE TABLE reassignment_logs (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    previous_courier_id BIGINT NOT NULL,
    new_courier_id BIGINT,
    reason VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    initiated_by VARCHAR(50) NOT NULL,
    initiated_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rl_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_rl_prev_courier FOREIGN KEY (previous_courier_id) REFERENCES couriers(id),
    CONSTRAINT fk_rl_new_courier FOREIGN KEY (new_courier_id) REFERENCES couriers(id)
);

-- Indexes for reassignment logs
CREATE INDEX idx_rl_order_id ON reassignment_logs(order_id);
CREATE INDEX idx_rl_previous_courier_id ON reassignment_logs(previous_courier_id);
CREATE INDEX idx_rl_new_courier_id ON reassignment_logs(new_courier_id);
CREATE INDEX idx_rl_created_at ON reassignment_logs(created_at);

-- Add last_active_at to track courier responsiveness
ALTER TABLE couriers ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP;

-- Create index for finding inactive couriers
CREATE INDEX IF NOT EXISTS idx_couriers_last_active_at ON couriers(last_active_at);

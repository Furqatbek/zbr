-- Delivery issues table for tracking courier-reported issues
CREATE TABLE IF NOT EXISTS delivery_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    courier_id BIGINT NOT NULL,
    issue_type VARCHAR(30) NOT NULL,
    description VARCHAR(1000),
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolution VARCHAR(500),
    resolved_at TIMESTAMP,
    resolved_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_di_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_di_courier FOREIGN KEY (courier_id) REFERENCES couriers(id)
);

-- Indexes for delivery_issues
CREATE INDEX idx_di_order_id ON delivery_issues(order_id);
CREATE INDEX idx_di_courier_id ON delivery_issues(courier_id);
CREATE INDEX idx_di_issue_type ON delivery_issues(issue_type);
CREATE INDEX idx_di_resolved ON delivery_issues(resolved);
CREATE INDEX idx_di_created_at ON delivery_issues(created_at);

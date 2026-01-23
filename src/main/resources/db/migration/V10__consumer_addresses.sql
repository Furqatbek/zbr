-- Consumer addresses table for saved delivery addresses
CREATE TABLE consumer_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(50),
    full_address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    apartment_number VARCHAR(50),
    entrance VARCHAR(50),
    instructions VARCHAR(500),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_consumer_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_consumer_addresses_user_id ON consumer_addresses(user_id);
CREATE INDEX idx_consumer_addresses_default ON consumer_addresses(user_id, is_default);

-- Comment
COMMENT ON TABLE consumer_addresses IS 'Saved delivery addresses for consumers';

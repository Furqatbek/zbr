-- Reviews table for order reviews
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    consumer_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    courier_id BIGINT,
    restaurant_rating INTEGER CHECK (restaurant_rating >= 1 AND restaurant_rating <= 5),
    courier_rating INTEGER CHECK (courier_rating >= 1 AND courier_rating <= 5),
    food_rating INTEGER CHECK (food_rating >= 1 AND food_rating <= 5),
    comment VARCHAR(1000),
    tags VARCHAR(500),
    is_visible BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_reviews_consumer FOREIGN KEY (consumer_id) REFERENCES users(id),
    CONSTRAINT fk_reviews_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_reviews_courier FOREIGN KEY (courier_id) REFERENCES couriers(id)
);

-- Indexes
CREATE INDEX idx_reviews_order_id ON reviews(order_id);
CREATE INDEX idx_reviews_consumer_id ON reviews(consumer_id);
CREATE INDEX idx_reviews_restaurant_id ON reviews(restaurant_id);
CREATE INDEX idx_reviews_courier_id ON reviews(courier_id);

-- Comment
COMMENT ON TABLE reviews IS 'Order reviews from consumers';

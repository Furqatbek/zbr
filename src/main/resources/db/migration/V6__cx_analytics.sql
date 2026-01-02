-- Customer Experience Analytics Tables
-- V6: NPS, ratings, and support tickets (matching JPA entities exactly)

-- Restaurant Ratings (matching RestaurantRating entity)
CREATE TABLE rating_restaurant (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    score INTEGER NOT NULL,
    food_quality_score INTEGER,
    portion_size_score INTEGER,
    value_for_money_score INTEGER,
    comment VARCHAR(2000),
    is_verified_purchase BOOLEAN DEFAULT TRUE,
    is_anonymous BOOLEAN DEFAULT FALSE,
    helpful_count INTEGER DEFAULT 0,
    restaurant_response VARCHAR(1000),
    restaurant_responded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_rating_restaurant_restaurant ON rating_restaurant(restaurant_id);
CREATE INDEX idx_rating_restaurant_user ON rating_restaurant(user_id);
CREATE INDEX idx_rating_restaurant_created ON rating_restaurant(created_at);
CREATE INDEX idx_rating_restaurant_score ON rating_restaurant(score);

-- Courier Ratings (matching CourierRating entity)
CREATE TABLE rating_courier (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    delivery_id BIGINT,
    score INTEGER NOT NULL,
    professionalism_score INTEGER,
    communication_score INTEGER,
    timeliness_score INTEGER,
    comment VARCHAR(2000),
    tip_amount DOUBLE PRECISION,
    is_verified_delivery BOOLEAN DEFAULT TRUE,
    is_anonymous BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_rating_courier_courier ON rating_courier(courier_id);
CREATE INDEX idx_rating_courier_user ON rating_courier(user_id);
CREATE INDEX idx_rating_courier_created ON rating_courier(created_at);
CREATE INDEX idx_rating_courier_score ON rating_courier(score);

-- App Store Ratings (matching AppStoreRating entity)
CREATE TABLE rating_app_store (
    id BIGSERIAL PRIMARY KEY,
    platform VARCHAR(20) NOT NULL,
    score INTEGER NOT NULL,
    country VARCHAR(10),
    app_version VARCHAR(20),
    review_id VARCHAR(100),
    author_name VARCHAR(100),
    title VARCHAR(500),
    comment VARCHAR(5000),
    developer_response VARCHAR(2000),
    developer_responded_at TIMESTAMP,
    helpful_count INTEGER DEFAULT 0,
    sentiment_score DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_date TIMESTAMP
);

CREATE INDEX idx_rating_app_platform ON rating_app_store(platform);
CREATE INDEX idx_rating_app_country ON rating_app_store(country);
CREATE INDEX idx_rating_app_created ON rating_app_store(created_at);
CREATE INDEX idx_rating_app_score ON rating_app_store(score);
CREATE INDEX idx_rating_app_version ON rating_app_store(app_version);

-- NPS Surveys (matching NpsSurvey entity)
CREATE TABLE nps_surveys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    score INTEGER NOT NULL,
    comment VARCHAR(2000),
    survey_channel VARCHAR(20),
    survey_trigger VARCHAR(50),
    order_id BIGINT,
    user_segment VARCHAR(50),
    app_version VARCHAR(20),
    device_type VARCHAR(50),
    is_follow_up_requested BOOLEAN DEFAULT FALSE,
    follow_up_completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_nps_user ON nps_surveys(user_id);
CREATE INDEX idx_nps_created ON nps_surveys(created_at);
CREATE INDEX idx_nps_score ON nps_surveys(score);
CREATE INDEX idx_nps_channel ON nps_surveys(survey_channel);

-- Support Tickets (matching SupportTicket entity)
CREATE TABLE support_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(20) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT,
    courier_id BIGINT,
    order_id BIGINT,
    ticket_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    subject VARCHAR(500),
    description VARCHAR(5000),
    channel VARCHAR(20),
    assigned_to BIGINT,
    assigned_at TIMESTAMP,
    first_response_at TIMESTAMP,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    resolution_notes VARCHAR(2000),
    refund_amount DOUBLE PRECISION,
    is_refunded BOOLEAN DEFAULT FALSE,
    customer_satisfaction_score INTEGER,
    sla_breach BOOLEAN DEFAULT FALSE,
    escalated BOOLEAN DEFAULT FALSE,
    escalated_at TIMESTAMP,
    reopen_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_ticket_user ON support_tickets(user_id);
CREATE INDEX idx_ticket_restaurant ON support_tickets(restaurant_id);
CREATE INDEX idx_ticket_courier ON support_tickets(courier_id);
CREATE INDEX idx_ticket_type ON support_tickets(ticket_type);
CREATE INDEX idx_ticket_status ON support_tickets(status);
CREATE INDEX idx_ticket_created ON support_tickets(created_at);
CREATE INDEX idx_ticket_priority ON support_tickets(priority);
CREATE INDEX idx_ticket_assigned ON support_tickets(assigned_to);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_cx_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_rating_restaurant_updated_at
    BEFORE UPDATE ON rating_restaurant
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

CREATE TRIGGER update_rating_courier_updated_at
    BEFORE UPDATE ON rating_courier
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

CREATE TRIGGER update_support_tickets_updated_at
    BEFORE UPDATE ON support_tickets
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

COMMENT ON TABLE rating_restaurant IS 'Customer ratings for restaurants';
COMMENT ON TABLE rating_courier IS 'Customer ratings for couriers';
COMMENT ON TABLE rating_app_store IS 'App store reviews from iOS and Android';
COMMENT ON TABLE nps_surveys IS 'Net Promoter Score surveys';
COMMENT ON TABLE support_tickets IS 'Customer support tickets';

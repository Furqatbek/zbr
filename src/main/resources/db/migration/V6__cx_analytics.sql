-- Customer Experience Analytics Tables
-- V6: NPS, ratings, and support tickets

-- Restaurant Ratings
CREATE TABLE restaurant_ratings (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    score INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    food_quality_score INTEGER CHECK (food_quality_score >= 1 AND food_quality_score <= 5),
    portion_size_score INTEGER CHECK (portion_size_score >= 1 AND portion_size_score <= 5),
    value_for_money_score INTEGER CHECK (value_for_money_score >= 1 AND value_for_money_score <= 5),
    comment TEXT,
    restaurant_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_restaurant_ratings_restaurant_id ON restaurant_ratings(restaurant_id);
CREATE INDEX idx_restaurant_ratings_user_id ON restaurant_ratings(user_id);
CREATE INDEX idx_restaurant_ratings_order_id ON restaurant_ratings(order_id);
CREATE INDEX idx_restaurant_ratings_created_at ON restaurant_ratings(created_at);
CREATE INDEX idx_restaurant_ratings_score ON restaurant_ratings(score);

-- Courier Ratings
CREATE TABLE courier_ratings (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    delivery_id BIGINT,
    score INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    professionalism_score INTEGER CHECK (professionalism_score >= 1 AND professionalism_score <= 5),
    communication_score INTEGER CHECK (communication_score >= 1 AND communication_score <= 5),
    timeliness_score INTEGER CHECK (timeliness_score >= 1 AND timeliness_score <= 5),
    comment TEXT,
    tip_amount DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_courier_ratings_courier_id ON courier_ratings(courier_id);
CREATE INDEX idx_courier_ratings_user_id ON courier_ratings(user_id);
CREATE INDEX idx_courier_ratings_order_id ON courier_ratings(order_id);
CREATE INDEX idx_courier_ratings_created_at ON courier_ratings(created_at);
CREATE INDEX idx_courier_ratings_score ON courier_ratings(score);

-- App Store Ratings
CREATE TABLE app_store_ratings (
    id BIGSERIAL PRIMARY KEY,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('IOS', 'ANDROID')),
    score INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    title VARCHAR(500),
    comment TEXT,
    review_id VARCHAR(255) UNIQUE,
    country VARCHAR(10),
    app_version VARCHAR(50),
    device_type VARCHAR(100),
    os_version VARCHAR(50),
    sentiment_score DOUBLE PRECISION,
    is_featured BOOLEAN DEFAULT FALSE,
    developer_response TEXT,
    developer_response_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_store_ratings_platform ON app_store_ratings(platform);
CREATE INDEX idx_app_store_ratings_created_at ON app_store_ratings(created_at);
CREATE INDEX idx_app_store_ratings_score ON app_store_ratings(score);

-- NPS Surveys
CREATE TABLE nps_surveys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 10),
    comment TEXT,
    survey_channel VARCHAR(30) CHECK (survey_channel IN ('IN_APP', 'EMAIL', 'SMS', 'PUSH_NOTIFICATION', 'WEB')),
    user_segment VARCHAR(50),
    order_id BIGINT,
    is_follow_up_requested BOOLEAN DEFAULT FALSE,
    follow_up_completed BOOLEAN DEFAULT FALSE,
    follow_up_completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_nps_surveys_user_id ON nps_surveys(user_id);
CREATE INDEX idx_nps_surveys_score ON nps_surveys(score);
CREATE INDEX idx_nps_surveys_created_at ON nps_surveys(created_at);
CREATE INDEX idx_nps_surveys_survey_channel ON nps_surveys(survey_channel);

-- Support Tickets
CREATE TABLE support_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    ticket_type VARCHAR(30) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    channel VARCHAR(20),
    subject VARCHAR(500) NOT NULL,
    description TEXT,
    assigned_agent_id BIGINT,
    first_response_at TIMESTAMP,
    first_response_time_minutes BIGINT,
    resolved_at TIMESTAMP,
    resolution_time_hours BIGINT,
    closed_at TIMESTAMP,
    sla_due_at TIMESTAMP,
    is_sla_breached BOOLEAN DEFAULT FALSE,
    is_escalated BOOLEAN DEFAULT FALSE,
    escalated_at TIMESTAMP,
    is_reopened BOOLEAN DEFAULT FALSE,
    reopened_count INTEGER DEFAULT 0,
    csat_score INTEGER CHECK (csat_score >= 1 AND csat_score <= 5),
    csat_feedback TEXT,
    refund_issued BOOLEAN DEFAULT FALSE,
    refund_amount DECIMAL(10,2),
    tags TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_support_tickets_user_id ON support_tickets(user_id);
CREATE INDEX idx_support_tickets_order_id ON support_tickets(order_id);
CREATE INDEX idx_support_tickets_type ON support_tickets(type);
CREATE INDEX idx_support_tickets_ticket_type ON support_tickets(ticket_type);
CREATE INDEX idx_support_tickets_status ON support_tickets(status);
CREATE INDEX idx_support_tickets_priority ON support_tickets(priority);
CREATE INDEX idx_support_tickets_created_at ON support_tickets(created_at);
CREATE INDEX idx_support_tickets_resolved_at ON support_tickets(resolved_at);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_cx_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_restaurant_ratings_updated_at
    BEFORE UPDATE ON restaurant_ratings
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

CREATE TRIGGER update_courier_ratings_updated_at
    BEFORE UPDATE ON courier_ratings
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

CREATE TRIGGER update_app_store_ratings_updated_at
    BEFORE UPDATE ON app_store_ratings
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

CREATE TRIGGER update_nps_surveys_updated_at
    BEFORE UPDATE ON nps_surveys
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

CREATE TRIGGER update_support_tickets_updated_at
    BEFORE UPDATE ON support_tickets
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

COMMENT ON TABLE restaurant_ratings IS 'Customer ratings for restaurants with sub-ratings';
COMMENT ON TABLE courier_ratings IS 'Customer ratings for couriers with sub-ratings';
COMMENT ON TABLE app_store_ratings IS 'App store reviews from iOS and Android';
COMMENT ON TABLE nps_surveys IS 'Net Promoter Score surveys (score 0-10)';
COMMENT ON TABLE support_tickets IS 'Customer support tickets with SLA tracking';

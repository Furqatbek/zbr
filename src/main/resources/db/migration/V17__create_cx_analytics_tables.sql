-- Customer Experience Analytics Tables
-- V17: Create CX analytics tables for NPS, ratings, and support tickets

-- ==================== Restaurant Ratings ====================
CREATE TABLE IF NOT EXISTS restaurant_ratings (
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

-- Indexes for restaurant_ratings
CREATE INDEX IF NOT EXISTS idx_restaurant_ratings_restaurant_id ON restaurant_ratings(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_restaurant_ratings_user_id ON restaurant_ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_restaurant_ratings_order_id ON restaurant_ratings(order_id);
CREATE INDEX IF NOT EXISTS idx_restaurant_ratings_created_at ON restaurant_ratings(created_at);
CREATE INDEX IF NOT EXISTS idx_restaurant_ratings_score ON restaurant_ratings(score);
CREATE INDEX IF NOT EXISTS idx_restaurant_ratings_restaurant_created ON restaurant_ratings(restaurant_id, created_at);

-- ==================== Courier Ratings ====================
CREATE TABLE IF NOT EXISTS courier_ratings (
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

-- Indexes for courier_ratings
CREATE INDEX IF NOT EXISTS idx_courier_ratings_courier_id ON courier_ratings(courier_id);
CREATE INDEX IF NOT EXISTS idx_courier_ratings_user_id ON courier_ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_courier_ratings_order_id ON courier_ratings(order_id);
CREATE INDEX IF NOT EXISTS idx_courier_ratings_delivery_id ON courier_ratings(delivery_id);
CREATE INDEX IF NOT EXISTS idx_courier_ratings_created_at ON courier_ratings(created_at);
CREATE INDEX IF NOT EXISTS idx_courier_ratings_score ON courier_ratings(score);
CREATE INDEX IF NOT EXISTS idx_courier_ratings_courier_created ON courier_ratings(courier_id, created_at);

-- ==================== App Store Ratings ====================
CREATE TABLE IF NOT EXISTS app_store_ratings (
    id BIGSERIAL PRIMARY KEY,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('IOS', 'ANDROID')),
    score INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    title VARCHAR(500),
    comment TEXT,
    review_id VARCHAR(255),
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

-- Add unique constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_review_id') THEN
        ALTER TABLE app_store_ratings ADD CONSTRAINT unique_review_id UNIQUE (review_id);
    END IF;
END $$;

-- Indexes for app_store_ratings
CREATE INDEX IF NOT EXISTS idx_app_store_ratings_platform ON app_store_ratings(platform);
CREATE INDEX IF NOT EXISTS idx_app_store_ratings_created_at ON app_store_ratings(created_at);
CREATE INDEX IF NOT EXISTS idx_app_store_ratings_score ON app_store_ratings(score);
CREATE INDEX IF NOT EXISTS idx_app_store_ratings_country ON app_store_ratings(country);
CREATE INDEX IF NOT EXISTS idx_app_store_ratings_app_version ON app_store_ratings(app_version);
CREATE INDEX IF NOT EXISTS idx_app_store_ratings_platform_created ON app_store_ratings(platform, created_at);

-- ==================== NPS Surveys ====================
CREATE TABLE IF NOT EXISTS nps_surveys (
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

-- Indexes for nps_surveys
CREATE INDEX IF NOT EXISTS idx_nps_surveys_user_id ON nps_surveys(user_id);
CREATE INDEX IF NOT EXISTS idx_nps_surveys_score ON nps_surveys(score);
CREATE INDEX IF NOT EXISTS idx_nps_surveys_created_at ON nps_surveys(created_at);
CREATE INDEX IF NOT EXISTS idx_nps_surveys_survey_channel ON nps_surveys(survey_channel);
CREATE INDEX IF NOT EXISTS idx_nps_surveys_user_segment ON nps_surveys(user_segment);

-- ==================== Support Tickets ====================
CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    ticket_type VARCHAR(30) NOT NULL CHECK (ticket_type IN (
        'LATE_DELIVERY', 'MISSING_ITEMS', 'WRONG_ORDER', 'COLD_FOOD',
        'REFUND_REQUEST', 'PAYMENT_ISSUE', 'ACCOUNT_ISSUE', 'PROMO_CODE_ISSUE',
        'APP_BUG', 'RESTAURANT_COMPLAINT', 'COURIER_COMPLAINT', 'OTHER'
    )),
    status VARCHAR(30) NOT NULL CHECK (status IN (
        'OPEN', 'IN_PROGRESS', 'WAITING_CUSTOMER', 'RESOLVED', 'CLOSED', 'REOPENED'
    )),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    channel VARCHAR(20) CHECK (channel IN ('IN_APP', 'EMAIL', 'PHONE', 'CHAT', 'SOCIAL_MEDIA')),
    subject VARCHAR(500) NOT NULL,
    description TEXT,
    assigned_agent_id BIGINT,
    first_response_at TIMESTAMP,
    resolved_at TIMESTAMP,
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
    tags TEXT[], -- PostgreSQL array for tags
    resolution_time_hours INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for support_tickets
CREATE INDEX IF NOT EXISTS idx_support_tickets_user_id ON support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_order_id ON support_tickets(order_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_ticket_type ON support_tickets(ticket_type);
CREATE INDEX IF NOT EXISTS idx_support_tickets_status ON support_tickets(status);
CREATE INDEX IF NOT EXISTS idx_support_tickets_priority ON support_tickets(priority);
CREATE INDEX IF NOT EXISTS idx_support_tickets_channel ON support_tickets(channel);
CREATE INDEX IF NOT EXISTS idx_support_tickets_assigned_agent_id ON support_tickets(assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_created_at ON support_tickets(created_at);
CREATE INDEX IF NOT EXISTS idx_support_tickets_resolved_at ON support_tickets(resolved_at);
CREATE INDEX IF NOT EXISTS idx_support_tickets_sla_due_at ON support_tickets(sla_due_at);
CREATE INDEX IF NOT EXISTS idx_support_tickets_is_sla_breached ON support_tickets(is_sla_breached);
CREATE INDEX IF NOT EXISTS idx_support_tickets_ticket_type_created ON support_tickets(ticket_type, created_at);
CREATE INDEX IF NOT EXISTS idx_support_tickets_status_created ON support_tickets(status, created_at);

-- ==================== Trigger for updated_at timestamps ====================
CREATE OR REPLACE FUNCTION update_cx_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to all CX tables (drop first if exists)
DROP TRIGGER IF EXISTS update_restaurant_ratings_updated_at ON restaurant_ratings;
CREATE TRIGGER update_restaurant_ratings_updated_at
    BEFORE UPDATE ON restaurant_ratings
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

DROP TRIGGER IF EXISTS update_courier_ratings_updated_at ON courier_ratings;
CREATE TRIGGER update_courier_ratings_updated_at
    BEFORE UPDATE ON courier_ratings
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

DROP TRIGGER IF EXISTS update_app_store_ratings_updated_at ON app_store_ratings;
CREATE TRIGGER update_app_store_ratings_updated_at
    BEFORE UPDATE ON app_store_ratings
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

DROP TRIGGER IF EXISTS update_nps_surveys_updated_at ON nps_surveys;
CREATE TRIGGER update_nps_surveys_updated_at
    BEFORE UPDATE ON nps_surveys
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

DROP TRIGGER IF EXISTS update_support_tickets_updated_at ON support_tickets;
CREATE TRIGGER update_support_tickets_updated_at
    BEFORE UPDATE ON support_tickets
    FOR EACH ROW EXECUTE FUNCTION update_cx_updated_at();

-- ==================== Comments for documentation ====================
COMMENT ON TABLE restaurant_ratings IS 'Customer ratings for restaurants with sub-ratings';
COMMENT ON TABLE courier_ratings IS 'Customer ratings for couriers with sub-ratings';
COMMENT ON TABLE app_store_ratings IS 'App store reviews from iOS and Android platforms';
COMMENT ON TABLE nps_surveys IS 'Net Promoter Score surveys (score 0-10)';
COMMENT ON TABLE support_tickets IS 'Customer support tickets with SLA tracking';

COMMENT ON COLUMN nps_surveys.score IS 'NPS score: 0-6 detractor, 7-8 passive, 9-10 promoter';
COMMENT ON COLUMN support_tickets.ticket_type IS 'Ticket type categorization for analytics';
COMMENT ON COLUMN support_tickets.sla_due_at IS 'SLA deadline based on priority';
COMMENT ON COLUMN support_tickets.is_sla_breached IS 'True if ticket was not resolved within SLA';

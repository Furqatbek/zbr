-- Fraud Detection and Security Analytics
-- V7: Auth logs, fraud events, and risk scoring

-- Drop existing tables if they exist (for idempotent migrations)
DO $$
BEGIN
    DROP TABLE IF EXISTS chargeback_events CASCADE;
    DROP TABLE IF EXISTS account_risk_scores CASCADE;
    DROP TABLE IF EXISTS fraud_rules CASCADE;
    DROP TABLE IF EXISTS ip_reputation CASCADE;
    DROP TABLE IF EXISTS device_fingerprints CASCADE;
    DROP TABLE IF EXISTS referral_events CASCADE;
    DROP TABLE IF EXISTS fraud_events CASCADE;
    DROP TABLE IF EXISTS auth_logs CASCADE;
END $$;

-- Auth Logs (matching AuthLog entity exactly)
CREATE TABLE auth_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    auth_type VARCHAR(30),
    failure_reason VARCHAR(255),
    device_id VARCHAR(255),
    device_fingerprint VARCHAR(255),
    geo_country VARCHAR(2),
    geo_city VARCHAR(100),
    is_vpn BOOLEAN,
    is_tor BOOLEAN,
    is_proxy BOOLEAN,
    session_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_logs_user_id ON auth_logs(user_id);
CREATE INDEX idx_auth_logs_ip_address ON auth_logs(ip_address);
CREATE INDEX idx_auth_logs_status ON auth_logs(status);
CREATE INDEX idx_auth_logs_created_at ON auth_logs(created_at);
CREATE INDEX idx_auth_logs_user_status_created ON auth_logs(user_id, status, created_at);
CREATE INDEX idx_auth_logs_ip_status_created ON auth_logs(ip_address, status, created_at);

-- Fraud Events
CREATE TABLE fraud_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT,
    order_id BIGINT,
    payment_id BIGINT,
    risk_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    fraud_indicators TEXT[],
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    is_confirmed_fraud BOOLEAN DEFAULT FALSE,
    is_false_positive BOOLEAN DEFAULT FALSE,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    action_taken VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fraud_events_user_id ON fraud_events(user_id);
CREATE INDEX idx_fraud_events_order_id ON fraud_events(order_id);
CREATE INDEX idx_fraud_events_event_type ON fraud_events(event_type);
CREATE INDEX idx_fraud_events_risk_level ON fraud_events(risk_level);
CREATE INDEX idx_fraud_events_created_at ON fraud_events(created_at);
CREATE INDEX idx_fraud_events_is_confirmed ON fraud_events(is_confirmed_fraud);

-- Referral Events (with is_suspicious column)
CREATE TABLE referral_events (
    id BIGSERIAL PRIMARY KEY,
    referrer_id BIGINT NOT NULL,
    referred_id BIGINT,
    referral_code VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    is_suspicious BOOLEAN DEFAULT FALSE,
    fraud_indicators TEXT[],
    reward_amount DECIMAL(10,2),
    reward_issued BOOLEAN DEFAULT FALSE,
    reward_issued_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_referral_events_referrer_id ON referral_events(referrer_id);
CREATE INDEX idx_referral_events_referred_id ON referral_events(referred_id);
CREATE INDEX idx_referral_events_code ON referral_events(referral_code);
CREATE INDEX idx_referral_events_is_suspicious ON referral_events(is_suspicious);
CREATE INDEX idx_referral_events_created_at ON referral_events(created_at);

-- Device Fingerprints (matching DeviceFingerprint entity)
CREATE TABLE device_fingerprints (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    fingerprint_hash VARCHAR(64),
    device_type VARCHAR(50),
    device_brand VARCHAR(100),
    device_model VARCHAR(100),
    os_name VARCHAR(50),
    os_version VARCHAR(50),
    app_version VARCHAR(50),
    screen_resolution VARCHAR(20),
    timezone VARCHAR(50),
    language VARCHAR(10),
    is_emulator BOOLEAN,
    is_rooted BOOLEAN,
    is_trusted BOOLEAN,
    trust_score INTEGER,
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP,
    times_seen INTEGER
);

CREATE INDEX idx_device_fingerprint_user_id ON device_fingerprints(user_id);
CREATE INDEX idx_device_fingerprint_device_id ON device_fingerprints(device_id);
CREATE INDEX idx_device_fingerprint_fingerprint ON device_fingerprints(fingerprint_hash);
CREATE INDEX idx_device_fingerprint_first_seen ON device_fingerprints(first_seen_at);

-- IP Reputation
CREATE TABLE ip_reputation (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    reputation_score DOUBLE PRECISION DEFAULT 0,
    is_vpn BOOLEAN DEFAULT FALSE,
    is_proxy BOOLEAN DEFAULT FALSE,
    is_tor BOOLEAN DEFAULT FALSE,
    is_datacenter BOOLEAN DEFAULT FALSE,
    is_blacklisted BOOLEAN DEFAULT FALSE,
    country_code VARCHAR(10),
    city VARCHAR(100),
    isp VARCHAR(255),
    fraud_count INTEGER DEFAULT 0,
    last_fraud_at TIMESTAMP,
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ip_reputation_ip ON ip_reputation(ip_address);
CREATE INDEX idx_ip_reputation_is_blacklisted ON ip_reputation(is_blacklisted);
CREATE INDEX idx_ip_reputation_reputation ON ip_reputation(reputation_score);

-- Fraud Rules
CREATE TABLE fraud_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    rule_type VARCHAR(50) NOT NULL,
    conditions JSONB NOT NULL,
    action VARCHAR(50) NOT NULL DEFAULT 'FLAG',
    risk_score_adjustment DOUBLE PRECISION DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fraud_rules_is_active ON fraud_rules(is_active);
CREATE INDEX idx_fraud_rules_rule_type ON fraud_rules(rule_type);

-- Account Risk Scores
CREATE TABLE account_risk_scores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    overall_risk_score DOUBLE PRECISION DEFAULT 0,
    payment_risk_score DOUBLE PRECISION DEFAULT 0,
    behavior_risk_score DOUBLE PRECISION DEFAULT 0,
    identity_risk_score DOUBLE PRECISION DEFAULT 0,
    risk_level VARCHAR(20) DEFAULT 'LOW',
    risk_factors TEXT[],
    fraud_history_count INTEGER DEFAULT 0,
    last_fraud_at TIMESTAMP,
    last_calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_account_risk_user_id ON account_risk_scores(user_id);
CREATE INDEX idx_account_risk_level ON account_risk_scores(risk_level);
CREATE INDEX idx_account_risk_overall ON account_risk_scores(overall_risk_score);

-- Chargeback Events
CREATE TABLE chargeback_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    order_id BIGINT,
    user_id BIGINT,
    chargeback_id VARCHAR(255),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    reason_code VARCHAR(50),
    reason_description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    is_fraud BOOLEAN DEFAULT FALSE,
    evidence_submitted BOOLEAN DEFAULT FALSE,
    evidence_submitted_at TIMESTAMP,
    dispute_won BOOLEAN,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chargeback_payment_id ON chargeback_events(payment_id);
CREATE INDEX idx_chargeback_order_id ON chargeback_events(order_id);
CREATE INDEX idx_chargeback_user_id ON chargeback_events(user_id);
CREATE INDEX idx_chargeback_status ON chargeback_events(status);
CREATE INDEX idx_chargeback_created_at ON chargeback_events(created_at);

-- Fraud Order History (matching FraudOrderHistory entity)
CREATE TABLE fraud_order_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT,
    courier_id BIGINT,
    amount DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2),
    user_paid_amount DECIMAL(10, 2),
    delivery_address VARCHAR(500),
    delivery_address_hash VARCHAR(64),
    delivery_lat DOUBLE PRECISION,
    delivery_lng DOUBLE PRECISION,
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    status VARCHAR(30) NOT NULL,
    is_refunded BOOLEAN,
    refund_amount DECIMAL(10, 2),
    refund_reason VARCHAR(255),
    is_promo_order BOOLEAN,
    promo_code VARCHAR(50),
    user_account_age_hours INTEGER,
    is_first_order BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_fraud_order_user_id ON fraud_order_history(user_id);
CREATE INDEX idx_fraud_order_device_id ON fraud_order_history(device_id);
CREATE INDEX idx_fraud_order_created_at ON fraud_order_history(created_at);
CREATE INDEX idx_fraud_order_address_hash ON fraud_order_history(delivery_address_hash);
CREATE INDEX idx_fraud_order_status ON fraud_order_history(status);
CREATE INDEX idx_fraud_order_user_created ON fraud_order_history(user_id, created_at);

-- Payment Attempts (matching PaymentAttempt entity)
CREATE TABLE payment_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(30),
    failure_reason VARCHAR(255),
    failure_code VARCHAR(50),
    card_last_four VARCHAR(4),
    card_bin VARCHAR(6),
    payment_token_hash VARCHAR(64),
    ip_address VARCHAR(45),
    device_id VARCHAR(255),
    gateway_response VARCHAR(500),
    is_3ds_verified BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_attempts_user_id ON payment_attempts(user_id);
CREATE INDEX idx_payment_attempts_order_id ON payment_attempts(order_id);
CREATE INDEX idx_payment_attempts_status ON payment_attempts(status);
CREATE INDEX idx_payment_attempts_created_at ON payment_attempts(created_at);
CREATE INDEX idx_payment_attempts_user_status_created ON payment_attempts(user_id, status, created_at);

-- Promo Usage Logs (matching PromoUsageLog entity)
CREATE TABLE promo_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    promo_code VARCHAR(50) NOT NULL,
    order_id BIGINT,
    promo_type VARCHAR(30),
    discount_value DECIMAL(10, 2) NOT NULL,
    order_total DECIMAL(10, 2),
    user_paid_amount DECIMAL(10, 2),
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    is_first_order BOOLEAN,
    is_referral_promo BOOLEAN,
    status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_promo_usage_user_id ON promo_usage_logs(user_id);
CREATE INDEX idx_promo_usage_promo_code ON promo_usage_logs(promo_code);
CREATE INDEX idx_promo_usage_order_id ON promo_usage_logs(order_id);
CREATE INDEX idx_promo_usage_created_at ON promo_usage_logs(created_at);
CREATE INDEX idx_promo_usage_device_id ON promo_usage_logs(device_id);

-- Security Flags (matching SecurityFlag entity)
CREATE TABLE security_flags (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flag_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    evidence VARCHAR(1000),
    risk_score INTEGER,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    ip_address VARCHAR(45),
    device_id VARCHAR(255),
    auto_detected BOOLEAN,
    detection_rule VARCHAR(100),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    review_notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_security_flag_user_id ON security_flags(user_id);
CREATE INDEX idx_security_flag_type ON security_flags(flag_type);
CREATE INDEX idx_security_flag_severity ON security_flags(severity);
CREATE INDEX idx_security_flag_status ON security_flags(status);
CREATE INDEX idx_security_flag_created_at ON security_flags(created_at);
CREATE INDEX idx_security_flag_user_type ON security_flags(user_id, flag_type);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_fraud_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_fraud_events_updated_at
    BEFORE UPDATE ON fraud_events
    FOR EACH ROW EXECUTE FUNCTION update_fraud_updated_at();

CREATE TRIGGER update_ip_reputation_updated_at
    BEFORE UPDATE ON ip_reputation
    FOR EACH ROW EXECUTE FUNCTION update_fraud_updated_at();

CREATE TRIGGER update_fraud_rules_updated_at
    BEFORE UPDATE ON fraud_rules
    FOR EACH ROW EXECUTE FUNCTION update_fraud_updated_at();

CREATE TRIGGER update_account_risk_updated_at
    BEFORE UPDATE ON account_risk_scores
    FOR EACH ROW EXECUTE FUNCTION update_fraud_updated_at();

CREATE TRIGGER update_chargeback_updated_at
    BEFORE UPDATE ON chargeback_events
    FOR EACH ROW EXECUTE FUNCTION update_fraud_updated_at();

COMMENT ON TABLE auth_logs IS 'Authentication events with device fingerprinting';
COMMENT ON TABLE fraud_events IS 'Detected fraud events with risk scoring';
COMMENT ON TABLE referral_events IS 'Referral program events with fraud detection';
COMMENT ON TABLE device_fingerprints IS 'Device fingerprint tracking for fraud detection';
COMMENT ON TABLE ip_reputation IS 'IP address reputation and risk data';
COMMENT ON TABLE fraud_rules IS 'Configurable fraud detection rules';
COMMENT ON TABLE account_risk_scores IS 'User account risk scoring';
COMMENT ON TABLE chargeback_events IS 'Payment chargeback tracking';

-- Fraud Analytics Tables Migration
-- Version: 18
-- Description: Creates tables for fraud detection and security analytics

-- Drop existing tables to handle schema changes from partial runs
DROP TABLE IF EXISTS security_flags CASCADE;
DROP TABLE IF EXISTS device_fingerprints CASCADE;
DROP TABLE IF EXISTS fraud_order_history CASCADE;
DROP TABLE IF EXISTS promo_usage_logs CASCADE;
DROP TABLE IF EXISTS referral_events CASCADE;
DROP TABLE IF EXISTS auth_logs CASCADE;
DROP TABLE IF EXISTS payment_attempts CASCADE;

-- Drop function if exists
DROP FUNCTION IF EXISTS calculate_z_score(DECIMAL, DECIMAL, DECIMAL);

-- ============================================
-- Payment Attempts Table
-- Tracks all payment attempts for fraud analysis
-- ============================================
CREATE TABLE payment_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_method VARCHAR(50) NOT NULL,
    card_bin VARCHAR(6),
    card_last_four VARCHAR(4),
    payment_token_hash VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(100),
    error_code VARCHAR(50),
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    geo_location VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,

    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'DECLINED', 'TIMEOUT', 'CANCELLED'))
);

-- Indexes for payment_attempts
CREATE INDEX idx_payment_attempts_user_id ON payment_attempts(user_id);
CREATE INDEX idx_payment_attempts_status ON payment_attempts(status);
CREATE INDEX idx_payment_attempts_created_at ON payment_attempts(created_at);
CREATE INDEX idx_payment_attempts_user_status ON payment_attempts(user_id, status);
CREATE INDEX idx_payment_attempts_card_bin ON payment_attempts(card_bin);
CREATE INDEX idx_payment_attempts_token_hash ON payment_attempts(payment_token_hash);
CREATE INDEX idx_payment_attempts_device_id ON payment_attempts(device_id);

-- ============================================
-- Authentication Logs Table
-- Tracks login attempts and security events
-- ============================================
CREATE TABLE auth_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    auth_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    device_id VARCHAR(255),
    geo_location VARCHAR(100),
    country_code VARCHAR(2),
    is_vpn BOOLEAN DEFAULT FALSE,
    is_tor BOOLEAN DEFAULT FALSE,
    is_proxy BOOLEAN DEFAULT FALSE,
    is_datacenter BOOLEAN DEFAULT FALSE,
    failure_reason VARCHAR(100),
    request_path VARCHAR(255),
    rate_limited BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_auth_type CHECK (auth_type IN ('LOGIN', 'LOGOUT', 'TOKEN_REFRESH', 'PASSWORD_RESET', 'MFA_VERIFY', 'API_KEY', 'OAUTH')),
    CONSTRAINT chk_auth_status CHECK (status IN ('SUCCESS', 'FAILED', 'BLOCKED', 'RATE_LIMITED', 'MFA_REQUIRED'))
);

-- Indexes for auth_logs
CREATE INDEX idx_auth_logs_user_id ON auth_logs(user_id);
CREATE INDEX idx_auth_logs_ip_address ON auth_logs(ip_address);
CREATE INDEX idx_auth_logs_status ON auth_logs(status);
CREATE INDEX idx_auth_logs_created_at ON auth_logs(created_at);
CREATE INDEX idx_auth_logs_user_status ON auth_logs(user_id, status);
CREATE INDEX idx_auth_logs_ip_status ON auth_logs(ip_address, status);
CREATE INDEX idx_auth_logs_vpn_tor ON auth_logs(is_vpn, is_tor, is_proxy);

-- ============================================
-- Referral Events Table
-- Tracks referral program usage for fraud detection
-- ============================================
CREATE TABLE referral_events (
    id BIGSERIAL PRIMARY KEY,
    referrer_id BIGINT NOT NULL,
    referred_user_id BIGINT NOT NULL,
    referral_code VARCHAR(50) NOT NULL,
    signup_device_id VARCHAR(255),
    signup_ip_address VARCHAR(45),
    reward_earned DECIMAL(10, 2) DEFAULT 0,
    reward_type VARCHAR(30),
    is_fraudulent BOOLEAN DEFAULT FALSE,
    fraud_reason VARCHAR(100),
    is_converted BOOLEAN DEFAULT FALSE,
    conversion_order_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    converted_at TIMESTAMP,

    CONSTRAINT chk_reward_type CHECK (reward_type IN ('CREDIT', 'DISCOUNT', 'FREE_DELIVERY', 'CASHBACK', 'POINTS'))
);

-- Indexes for referral_events
CREATE INDEX idx_referral_events_referrer ON referral_events(referrer_id);
CREATE INDEX idx_referral_events_referred ON referral_events(referred_user_id);
CREATE INDEX idx_referral_events_code ON referral_events(referral_code);
CREATE INDEX idx_referral_events_device ON referral_events(signup_device_id);
CREATE INDEX idx_referral_events_ip ON referral_events(signup_ip_address);
CREATE INDEX idx_referral_events_created_at ON referral_events(created_at);
CREATE INDEX idx_referral_events_fraudulent ON referral_events(is_fraudulent);

-- ============================================
-- Promo Usage Logs Table
-- Tracks promotional code usage
-- ============================================
CREATE TABLE promo_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    promo_code VARCHAR(50) NOT NULL,
    promo_type VARCHAR(30) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    discount_percentage DECIMAL(5, 2),
    order_subtotal DECIMAL(12, 2),
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    is_first_order BOOLEAN DEFAULT FALSE,
    is_abuse_suspected BOOLEAN DEFAULT FALSE,
    abuse_reason VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_promo_type CHECK (promo_type IN ('REFERRAL', 'WELCOME', 'SEASONAL', 'LOYALTY', 'PARTNER', 'FLASH_SALE', 'FREE_DELIVERY', 'PERCENTAGE', 'FIXED'))
);

-- Indexes for promo_usage_logs
CREATE INDEX idx_promo_usage_user ON promo_usage_logs(user_id);
CREATE INDEX idx_promo_usage_code ON promo_usage_logs(promo_code);
CREATE INDEX idx_promo_usage_type ON promo_usage_logs(promo_type);
CREATE INDEX idx_promo_usage_device ON promo_usage_logs(device_id);
CREATE INDEX idx_promo_usage_ip ON promo_usage_logs(ip_address);
CREATE INDEX idx_promo_usage_created ON promo_usage_logs(created_at);
CREATE INDEX idx_promo_usage_abuse ON promo_usage_logs(is_abuse_suspected);

-- ============================================
-- Fraud Order History Table
-- Enriched order data for fraud analysis
-- ============================================
CREATE TABLE fraud_order_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    order_total DECIMAL(12, 2) NOT NULL,
    delivery_address_hash VARCHAR(64),
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    has_promo BOOLEAN DEFAULT FALSE,
    promo_discount DECIMAL(10, 2) DEFAULT 0,
    is_refunded BOOLEAN DEFAULT FALSE,
    refund_amount DECIMAL(10, 2) DEFAULT 0,
    refund_reason VARCHAR(100),
    account_age_at_order INTEGER,
    orders_count_at_time INTEGER,
    order_status VARCHAR(30),
    is_flagged BOOLEAN DEFAULT FALSE,
    flag_reason VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,

    CONSTRAINT chk_order_status CHECK (order_status IN ('PLACED', 'CONFIRMED', 'PREPARING', 'READY', 'PICKED_UP', 'DELIVERED', 'CANCELLED', 'REFUNDED'))
);

-- Indexes for fraud_order_history
CREATE INDEX idx_fraud_order_user ON fraud_order_history(user_id);
CREATE INDEX idx_fraud_order_restaurant ON fraud_order_history(restaurant_id);
CREATE INDEX idx_fraud_order_address_hash ON fraud_order_history(delivery_address_hash);
CREATE INDEX idx_fraud_order_device ON fraud_order_history(device_id);
CREATE INDEX idx_fraud_order_refunded ON fraud_order_history(is_refunded);
CREATE INDEX idx_fraud_order_created ON fraud_order_history(created_at);
CREATE INDEX idx_fraud_order_user_created ON fraud_order_history(user_id, created_at);
CREATE INDEX idx_fraud_order_flagged ON fraud_order_history(is_flagged);

-- ============================================
-- Device Fingerprints Table
-- Tracks device information for fraud detection
-- ============================================
CREATE TABLE device_fingerprints (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    device_type VARCHAR(30),
    os_name VARCHAR(50),
    os_version VARCHAR(30),
    browser_name VARCHAR(50),
    browser_version VARCHAR(30),
    screen_resolution VARCHAR(20),
    timezone VARCHAR(50),
    language VARCHAR(10),
    is_emulator BOOLEAN DEFAULT FALSE,
    is_rooted BOOLEAN DEFAULT FALSE,
    trust_score INTEGER DEFAULT 100,
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_device_type CHECK (device_type IN ('MOBILE_ANDROID', 'MOBILE_IOS', 'TABLET_ANDROID', 'TABLET_IOS', 'DESKTOP', 'WEB', 'UNKNOWN')),
    CONSTRAINT chk_trust_score CHECK (trust_score >= 0 AND trust_score <= 100)
);

-- Indexes for device_fingerprints
CREATE INDEX idx_device_fingerprints_device ON device_fingerprints(device_id);
CREATE INDEX idx_device_fingerprints_user ON device_fingerprints(user_id);
CREATE INDEX idx_device_fingerprints_type ON device_fingerprints(device_type);
CREATE INDEX idx_device_fingerprints_emulator ON device_fingerprints(is_emulator);
CREATE INDEX idx_device_fingerprints_rooted ON device_fingerprints(is_rooted);
CREATE INDEX idx_device_fingerprints_trust ON device_fingerprints(trust_score);
CREATE INDEX idx_device_fingerprints_first_seen ON device_fingerprints(first_seen_at);
CREATE UNIQUE INDEX idx_device_fingerprints_device_user ON device_fingerprints(device_id, user_id);

-- ============================================
-- Security Flags Table
-- Tracks security-related flags and alerts
-- ============================================
CREATE TABLE security_flags (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    entity_type VARCHAR(30) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    flag_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    details JSONB,
    source VARCHAR(50),
    resolved_by BIGINT,
    resolved_at TIMESTAMP,
    resolution_notes TEXT,
    flagged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,

    CONSTRAINT chk_entity_type CHECK (entity_type IN ('USER', 'DEVICE', 'IP_ADDRESS', 'PAYMENT_METHOD', 'ORDER', 'RESTAURANT', 'COURIER')),
    CONSTRAINT chk_flag_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_flag_status CHECK (status IN ('ACTIVE', 'RESOLVED', 'DISMISSED', 'EXPIRED', 'ESCALATED'))
);

-- Indexes for security_flags
CREATE INDEX idx_security_flags_user ON security_flags(user_id);
CREATE INDEX idx_security_flags_entity ON security_flags(entity_type, entity_id);
CREATE INDEX idx_security_flags_type ON security_flags(flag_type);
CREATE INDEX idx_security_flags_severity ON security_flags(severity);
CREATE INDEX idx_security_flags_status ON security_flags(status);
CREATE INDEX idx_security_flags_flagged_at ON security_flags(flagged_at);

-- ============================================
-- Comments for documentation
-- ============================================
COMMENT ON TABLE payment_attempts IS 'Tracks all payment attempts for fraud detection and analysis';
COMMENT ON TABLE auth_logs IS 'Stores authentication events including login attempts, failures, and security indicators';
COMMENT ON TABLE referral_events IS 'Tracks referral program events for fraud detection (device/IP reuse, circular referrals)';
COMMENT ON TABLE promo_usage_logs IS 'Records promotional code usage for abuse detection';
COMMENT ON TABLE fraud_order_history IS 'Enriched order history with fraud-relevant data points';
COMMENT ON TABLE device_fingerprints IS 'Stores device fingerprint data for multi-account and fraud detection';
COMMENT ON TABLE security_flags IS 'Central repository for all security-related flags and alerts';

-- ============================================
-- Create aggregate functions for fraud analysis
-- ============================================

-- Function to calculate z-score for anomaly detection
CREATE OR REPLACE FUNCTION calculate_z_score(value DECIMAL, mean DECIMAL, stddev DECIMAL)
RETURNS DECIMAL AS $$
BEGIN
    IF stddev = 0 THEN
        RETURN 0;
    END IF;
    RETURN (value - mean) / stddev;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION calculate_z_score IS 'Calculates z-score for anomaly detection in fraud analysis';

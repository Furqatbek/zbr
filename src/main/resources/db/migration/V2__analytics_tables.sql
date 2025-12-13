-- =====================================================
-- V2: Analytics and Dashboard Tables
-- =====================================================

-- =====================================================
-- FINANCIAL ANALYTICS
-- =====================================================

-- Courier bonuses
CREATE TABLE courier_bonuses (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    bonus_type VARCHAR(30) NOT NULL,
    bonus_amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(500),
    campaign_id BIGINT,
    target_achieved VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    bonus_date TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cb_courier_id ON courier_bonuses(courier_id);
CREATE INDEX idx_cb_bonus_date ON courier_bonuses(bonus_date);
CREATE INDEX idx_cb_bonus_type ON courier_bonuses(bonus_type);

-- Courier payments
CREATE TABLE courier_payments (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    base_payment DECIMAL(10,2) NOT NULL,
    distance_bonus DECIMAL(10,2) DEFAULT 0,
    tip_amount DECIMAL(10,2) DEFAULT 0,
    peak_bonus DECIMAL(10,2) DEFAULT 0,
    total_payment DECIMAL(10,2) NOT NULL,
    payment_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_date TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    payment_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cp_courier_id ON courier_payments(courier_id);
CREATE INDEX idx_cp_order_id ON courier_payments(order_id);
CREATE INDEX idx_cp_payment_date ON courier_payments(payment_date);
CREATE INDEX idx_cp_status ON courier_payments(payment_status);

-- Gift card usages
CREATE TABLE gift_card_usages (
    id BIGSERIAL PRIMARY KEY,
    gift_card_id BIGINT NOT NULL,
    gift_card_code VARCHAR(50) NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    redeemed_amount DECIMAL(10,2) NOT NULL,
    balance_before DECIMAL(10,2) NOT NULL,
    balance_after DECIMAL(10,2) NOT NULL,
    used_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gcu_order_id ON gift_card_usages(order_id);
CREATE INDEX idx_gcu_gift_card_id ON gift_card_usages(gift_card_id);
CREATE INDEX idx_gcu_user_id ON gift_card_usages(user_id);

-- Payout disputes
CREATE TABLE payout_disputes (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    payout_id BIGINT,
    order_id BIGINT,
    dispute_type VARCHAR(30) NOT NULL,
    disputed_amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolution VARCHAR(500),
    resolved_amount DECIMAL(10,2),
    resolved_at TIMESTAMP,
    resolved_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pd_entity_id ON payout_disputes(entity_id);
CREATE INDEX idx_pd_entity_type ON payout_disputes(entity_type);
CREATE INDEX idx_pd_status ON payout_disputes(status);

-- Promotion usages
CREATE TABLE promotion_usages (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    promotion_id BIGINT,
    user_id BIGINT NOT NULL,
    promo_code VARCHAR(50),
    promotion_type VARCHAR(30) NOT NULL,
    original_order_value DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL,
    funded_by VARCHAR(20) NOT NULL DEFAULT 'PLATFORM',
    platform_cost DECIMAL(10,2) DEFAULT 0,
    restaurant_cost DECIMAL(10,2) DEFAULT 0,
    used_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pu_order_id ON promotion_usages(order_id);
CREATE INDEX idx_pu_promotion_id ON promotion_usages(promotion_id);
CREATE INDEX idx_pu_user_id ON promotion_usages(user_id);

-- Referral rewards
CREATE TABLE referral_rewards (
    id BIGSERIAL PRIMARY KEY,
    referral_id BIGINT NOT NULL,
    referrer_id BIGINT NOT NULL,
    referred_user_id BIGINT NOT NULL,
    reward_type VARCHAR(20) NOT NULL,
    reward_amount DECIMAL(10,2) NOT NULL,
    triggering_order_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    awarded_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rr_referrer_id ON referral_rewards(referrer_id);
CREATE INDEX idx_rr_referred_id ON referral_rewards(referred_user_id);
CREATE INDEX idx_rr_awarded_at ON referral_rewards(awarded_at);

-- Restaurant commissions
CREATE TABLE restaurant_commissions (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_subtotal DECIMAL(12,2) NOT NULL,
    commission_rate DECIMAL(5,2) NOT NULL,
    fixed_commission DECIMAL(10,2) DEFAULT 0,
    commission_amount DECIMAL(10,2) NOT NULL,
    commission_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    earned_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rc_restaurant_id ON restaurant_commissions(restaurant_id);
CREATE INDEX idx_rc_order_id ON restaurant_commissions(order_id);
CREATE INDEX idx_rc_earned_at ON restaurant_commissions(earned_at);

-- Restaurant payouts
CREATE TABLE restaurant_payouts (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    gross_amount DECIMAL(12,2) NOT NULL,
    commission_deducted DECIMAL(10,2) NOT NULL,
    delivery_subsidies DECIMAL(10,2) DEFAULT 0,
    promotion_costs DECIMAL(10,2) DEFAULT 0,
    adjustments DECIMAL(10,2) DEFAULT 0,
    fees DECIMAL(10,2) DEFAULT 0,
    net_payout DECIMAL(12,2) NOT NULL,
    order_count INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payout_date TIMESTAMP,
    paid_at TIMESTAMP,
    payment_reference VARCHAR(100),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rp_restaurant_id ON restaurant_payouts(restaurant_id);
CREATE INDEX idx_rp_payout_date ON restaurant_payouts(payout_date);
CREATE INDEX idx_rp_status ON restaurant_payouts(status);

-- =====================================================
-- FRAUD ANALYTICS
-- =====================================================

-- Auth logs
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

-- Device fingerprints
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
    trust_score INT,
    first_seen_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP,
    times_seen INT DEFAULT 1
);

CREATE INDEX idx_device_fingerprint_user_id ON device_fingerprints(user_id);
CREATE INDEX idx_device_fingerprint_device_id ON device_fingerprints(device_id);
CREATE INDEX idx_device_fingerprint_hash ON device_fingerprints(fingerprint_hash);

-- Fraud order history
CREATE TABLE fraud_order_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT,
    courier_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2),
    user_paid_amount DECIMAL(10,2),
    delivery_address VARCHAR(500),
    delivery_address_hash VARCHAR(64),
    delivery_lat DOUBLE PRECISION,
    delivery_lng DOUBLE PRECISION,
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    status VARCHAR(30) NOT NULL,
    is_refunded BOOLEAN,
    refund_amount DECIMAL(10,2),
    refund_reason VARCHAR(255),
    is_promo_order BOOLEAN,
    promo_code VARCHAR(50),
    user_account_age_hours INT,
    is_first_order BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_fraud_order_user_id ON fraud_order_history(user_id);
CREATE INDEX idx_fraud_order_device_id ON fraud_order_history(device_id);
CREATE INDEX idx_fraud_order_created_at ON fraud_order_history(created_at);

-- Payment attempts
CREATE TABLE payment_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
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

-- Promo usage logs
CREATE TABLE promo_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    promo_code VARCHAR(50) NOT NULL,
    order_id BIGINT,
    promo_type VARCHAR(30),
    discount_value DECIMAL(10,2) NOT NULL,
    order_total DECIMAL(10,2),
    user_paid_amount DECIMAL(10,2),
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

-- Referral events
CREATE TABLE referral_events (
    id BIGSERIAL PRIMARY KEY,
    referrer_id BIGINT NOT NULL,
    referred_user_id BIGINT NOT NULL,
    referral_code VARCHAR(50),
    device_id VARCHAR(255),
    device_fingerprint VARCHAR(255),
    ip_address VARCHAR(45),
    status VARCHAR(20) NOT NULL,
    referrer_bonus DECIMAL(10,2),
    referred_bonus DECIMAL(10,2),
    phone_number_hash VARCHAR(64),
    email_domain VARCHAR(255),
    first_order_completed BOOLEAN,
    first_order_id BIGINT,
    is_suspicious BOOLEAN,
    fraud_flags VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_referral_events_referrer_id ON referral_events(referrer_id);
CREATE INDEX idx_referral_events_referred_user_id ON referral_events(referred_user_id);
CREATE INDEX idx_referral_events_device_id ON referral_events(device_id);
CREATE INDEX idx_referral_events_created_at ON referral_events(created_at);

-- Security flags
CREATE TABLE security_flags (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flag_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    evidence VARCHAR(1000),
    risk_score INT,
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

-- =====================================================
-- CUSTOMER EXPERIENCE (CX)
-- =====================================================

-- App store ratings
CREATE TABLE rating_app_store (
    id BIGSERIAL PRIMARY KEY,
    platform VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    country VARCHAR(10),
    app_version VARCHAR(20),
    review_id VARCHAR(100),
    author_name VARCHAR(100),
    title VARCHAR(500),
    comment VARCHAR(5000),
    developer_response VARCHAR(2000),
    developer_responded_at TIMESTAMP,
    helpful_count INT DEFAULT 0,
    sentiment_score DOUBLE PRECISION,
    review_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rating_app_platform ON rating_app_store(platform);
CREATE INDEX idx_rating_app_country ON rating_app_store(country);
CREATE INDEX idx_rating_app_created ON rating_app_store(created_at);

-- NPS surveys
CREATE TABLE nps_surveys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    score INT NOT NULL,
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

-- Support tickets
CREATE TABLE support_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(20) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT,
    courier_id BIGINT,
    order_id BIGINT,
    ticket_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
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
    customer_satisfaction_score INT,
    sla_breach BOOLEAN DEFAULT FALSE,
    escalated BOOLEAN DEFAULT FALSE,
    escalated_at TIMESTAMP,
    reopen_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_user ON support_tickets(user_id);
CREATE INDEX idx_ticket_restaurant ON support_tickets(restaurant_id);
CREATE INDEX idx_ticket_type ON support_tickets(ticket_type);
CREATE INDEX idx_ticket_status ON support_tickets(status);
CREATE INDEX idx_ticket_created ON support_tickets(created_at);

-- =====================================================
-- OPERATIONS ANALYTICS
-- =====================================================

-- Courier availability events
CREATE TABLE courier_availability_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20),
    reason VARCHAR(500),
    status_changed_at TIMESTAMP NOT NULL,
    previous_status_duration_minutes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cae_courier_id ON courier_availability_events(courier_id);
CREATE INDEX idx_cae_status_changed_at ON courier_availability_events(status_changed_at);

-- Courier location events
CREATE TABLE courier_location_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    accuracy_meters DOUBLE PRECISION,
    speed_kmh DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    altitude_meters DOUBLE PRECISION,
    battery_level INT,
    is_moving BOOLEAN DEFAULT FALSE,
    recorded_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_cle_courier_id ON courier_location_events(courier_id);
CREATE INDEX idx_cle_recorded_at ON courier_location_events(recorded_at);

-- Courier order events
CREATE TABLE courier_order_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    response_time_seconds BIGINT,
    distance_to_restaurant_km DOUBLE PRECISION,
    distance_to_customer_km DOUBLE PRECISION,
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coe_courier_id ON courier_order_events(courier_id);
CREATE INDEX idx_coe_order_id ON courier_order_events(order_id);
CREATE INDEX idx_coe_event_type ON courier_order_events(event_type);

-- ETA history
CREATE TABLE eta_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    courier_id BIGINT,
    eta_shown_minutes INT NOT NULL,
    predicted_delivery_time TIMESTAMP NOT NULL,
    predicted_at TIMESTAMP NOT NULL,
    actual_delivered_at TIMESTAMP,
    actual_delivery_minutes INT,
    eta_error_minutes INT,
    eta_error_abs_minutes INT,
    was_late BOOLEAN,
    was_completed BOOLEAN DEFAULT FALSE,
    eta_prep_minutes INT,
    actual_prep_minutes INT,
    eta_pickup_minutes INT,
    actual_pickup_minutes INT,
    eta_delivery_minutes INT,
    actual_travel_minutes INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_eta_order_id ON eta_history(order_id);
CREATE INDEX idx_eta_restaurant_id ON eta_history(restaurant_id);
CREATE INDEX idx_eta_predicted_at ON eta_history(predicted_at);

-- Menu update history
CREATE TABLE menu_update_history (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    update_type VARCHAR(30) NOT NULL,
    items_added INT DEFAULT 0,
    items_removed INT DEFAULT 0,
    items_modified INT DEFAULT 0,
    categories_changed INT DEFAULT 0,
    prices_updated INT DEFAULT 0,
    updated_by_user_id BIGINT,
    updated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_muh_restaurant_id ON menu_update_history(restaurant_id);
CREATE INDEX idx_muh_updated_at ON menu_update_history(updated_at);

-- Restaurant online status
CREATE TABLE restaurant_online_status (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    is_online BOOLEAN NOT NULL,
    reason VARCHAR(500),
    status_changed_at TIMESTAMP NOT NULL,
    previous_status_duration_minutes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ros_restaurant_id ON restaurant_online_status(restaurant_id);
CREATE INDEX idx_ros_status_changed_at ON restaurant_online_status(status_changed_at);

-- Restaurant order events
CREATE TABLE restaurant_order_events (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    estimated_prep_minutes INT,
    actual_prep_minutes INT,
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_roe_restaurant_id ON restaurant_order_events(restaurant_id);
CREATE INDEX idx_roe_order_id ON restaurant_order_events(order_id);
CREATE INDEX idx_roe_event_type ON restaurant_order_events(event_type);

-- =====================================================
-- TECHNICAL ANALYTICS
-- =====================================================

-- Backup logs
CREATE TABLE backup_logs (
    id BIGSERIAL PRIMARY KEY,
    backup_id VARCHAR(50) NOT NULL,
    backup_type VARCHAR(20) NOT NULL,
    database_name VARCHAR(100),
    backup_size_bytes BIGINT,
    backup_location VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    duration_seconds BIGINT,
    error_message VARCHAR(1000),
    tables_backed_up INT,
    rows_backed_up BIGINT,
    is_compressed BOOLEAN DEFAULT TRUE,
    is_encrypted BOOLEAN DEFAULT FALSE,
    retention_days INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bl_backup_type ON backup_logs(backup_type);
CREATE INDEX idx_bl_status ON backup_logs(status);
CREATE INDEX idx_bl_started_at ON backup_logs(started_at);

-- HTTP request logs
CREATE TABLE http_request_logs (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(50),
    method VARCHAR(10) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    status_code INT NOT NULL,
    response_time_ms BIGINT NOT NULL,
    request_size_bytes BIGINT,
    response_size_bytes BIGINT,
    user_id BIGINT,
    user_agent VARCHAR(500),
    client_ip VARCHAR(45),
    error_message VARCHAR(2000),
    is_timeout BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hrl_timestamp ON http_request_logs(timestamp);
CREATE INDEX idx_hrl_status_code ON http_request_logs(status_code);
CREATE INDEX idx_hrl_endpoint ON http_request_logs(endpoint);

-- Message queue stats
CREATE TABLE message_queue_stats (
    id BIGSERIAL PRIMARY KEY,
    queue_name VARCHAR(100) NOT NULL,
    broker_type VARCHAR(20) NOT NULL,
    queue_depth BIGINT,
    messages_published BIGINT,
    messages_consumed BIGINT,
    messages_acknowledged BIGINT,
    messages_rejected BIGINT,
    consumer_count INT,
    publish_rate DOUBLE PRECISION,
    consume_rate DOUBLE PRECISION,
    avg_processing_time_ms BIGINT,
    consumer_lag BIGINT,
    partition_count INT,
    is_healthy BOOLEAN DEFAULT TRUE,
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mqs_queue_name ON message_queue_stats(queue_name);
CREATE INDEX idx_mqs_recorded_at ON message_queue_stats(recorded_at);

-- Slow query logs
CREATE TABLE slow_query_logs (
    id BIGSERIAL PRIMARY KEY,
    query_hash VARCHAR(64),
    query_text TEXT NOT NULL,
    duration_ms BIGINT NOT NULL,
    rows_affected BIGINT,
    rows_examined BIGINT,
    database_name VARCHAR(100),
    table_name VARCHAR(100),
    query_type VARCHAR(20),
    is_using_index BOOLEAN,
    index_used VARCHAR(100),
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sql_timestamp ON slow_query_logs(timestamp);
CREATE INDEX idx_sql_duration ON slow_query_logs(duration_ms);
CREATE INDEX idx_sql_query_hash ON slow_query_logs(query_hash);

-- Storage metadata
CREATE TABLE storage_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    file_size_bytes BIGINT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    bucket_name VARCHAR(100),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    uploaded_by BIGINT,
    upload_duration_ms BIGINT,
    is_upload_successful BOOLEAN DEFAULT TRUE,
    error_message VARCHAR(500),
    uploaded_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sm_storage_type ON storage_metadata(storage_type);
CREATE INDEX idx_sm_uploaded_at ON storage_metadata(uploaded_at);

-- System metric snapshots
CREATE TABLE system_metric_snapshots (
    id BIGSERIAL PRIMARY KEY,
    metric_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(20),
    tags VARCHAR(500),
    host VARCHAR(100),
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sms_metric_type ON system_metric_snapshots(metric_type);
CREATE INDEX idx_sms_recorded_at ON system_metric_snapshots(recorded_at);

-- WebSocket connection logs
CREATE TABLE websocket_connection_logs (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    user_type VARCHAR(20) NOT NULL,
    connected_at TIMESTAMP NOT NULL,
    disconnected_at TIMESTAMP,
    disconnect_reason VARCHAR(100),
    is_graceful_disconnect BOOLEAN,
    client_ip VARCHAR(45),
    user_agent VARCHAR(500),
    messages_sent BIGINT DEFAULT 0,
    messages_received BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wscl_user_type ON websocket_connection_logs(user_type);
CREATE INDEX idx_wscl_connected_at ON websocket_connection_logs(connected_at);
CREATE INDEX idx_wscl_session_id ON websocket_connection_logs(session_id);

-- WebSocket message logs
CREATE TABLE websocket_message_logs (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    destination VARCHAR(200),
    payload_size_bytes BIGINT,
    published_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    is_delivered BOOLEAN DEFAULT FALSE,
    delivery_attempts INT DEFAULT 0,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wsml_session_id ON websocket_message_logs(session_id);
CREATE INDEX idx_wsml_message_type ON websocket_message_logs(message_type);
CREATE INDEX idx_wsml_published_at ON websocket_message_logs(published_at);

-- =====================================================
-- ADMIN DASHBOARD
-- =====================================================

-- Dashboard refresh logs
CREATE TABLE dashboard_refresh_logs (
    id BIGSERIAL PRIMARY KEY,
    panel_name VARCHAR(50) NOT NULL,
    component VARCHAR(50),
    admin_user_id BIGINT,
    query_duration_ms INT,
    duration_ms BIGINT,
    cache_hit BOOLEAN DEFAULT FALSE,
    records_returned INT,
    records_processed INT,
    filter_params VARCHAR(1000),
    error_message VARCHAR(500),
    message VARCHAR(500),
    successful BOOLEAN DEFAULT TRUE,
    success BOOLEAN,
    triggered_by VARCHAR(50) DEFAULT 'SCHEDULED',
    refreshed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_panel ON dashboard_refresh_logs(panel_name);
CREATE INDEX idx_refresh_time ON dashboard_refresh_logs(refreshed_at DESC);
CREATE INDEX idx_refresh_user ON dashboard_refresh_logs(admin_user_id);

-- System health snapshots
CREATE TABLE system_health_snapshots (
    id BIGSERIAL PRIMARY KEY,
    component VARCHAR(50) NOT NULL,
    api_latency_p50 INT,
    api_latency_p90 INT,
    api_latency_p99 INT,
    db_active_connections INT,
    db_idle_connections INT,
    db_waiting_connections INT,
    redis_latency_ms INT,
    redis_memory_used_mb INT,
    redis_connected_clients INT,
    queue_backlog BIGINT,
    queue_processing_rate DOUBLE PRECISION,
    cpu_usage_percent DOUBLE PRECISION,
    memory_usage_percent DOUBLE PRECISION,
    disk_usage_percent DOUBLE PRECISION,
    error_rate_percent DOUBLE PRECISION,
    requests_per_second DOUBLE PRECISION,
    health_status VARCHAR(20) DEFAULT 'HEALTHY',
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_health_snapshot_time ON system_health_snapshots(captured_at);
CREATE INDEX idx_health_snapshot_component ON system_health_snapshots(component);

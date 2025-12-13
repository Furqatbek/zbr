-- =====================================================
-- V24: Add remaining missing columns to match JPA entities
-- =====================================================

-- =====================================================
-- FINANCIAL PACKAGE
-- =====================================================

-- courier_bonuses
ALTER TABLE courier_bonuses ADD COLUMN IF NOT EXISTS bonus_date TIMESTAMP;
ALTER TABLE courier_bonuses ADD COLUMN IF NOT EXISTS campaign_id BIGINT;
ALTER TABLE courier_bonuses ADD COLUMN IF NOT EXISTS target_achieved VARCHAR(200);
-- Copy earned_at to bonus_date for existing records
UPDATE courier_bonuses SET bonus_date = earned_at WHERE bonus_date IS NULL;

-- courier_payments
ALTER TABLE courier_payments ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(100);

-- payout_disputes
ALTER TABLE payout_disputes ADD COLUMN IF NOT EXISTS order_id BIGINT;
ALTER TABLE payout_disputes ADD COLUMN IF NOT EXISTS resolved_by VARCHAR(100);
ALTER TABLE payout_disputes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE payout_disputes ADD COLUMN IF NOT EXISTS resolution TEXT;

-- restaurant_payouts
ALTER TABLE restaurant_payouts ADD COLUMN IF NOT EXISTS payout_date TIMESTAMP;
ALTER TABLE restaurant_payouts ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(100);
ALTER TABLE restaurant_payouts ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE restaurant_payouts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- =====================================================
-- TECHNICAL PACKAGE
-- =====================================================

-- slow_query_logs
ALTER TABLE slow_query_logs ADD COLUMN IF NOT EXISTS rows_examined BIGINT;
ALTER TABLE slow_query_logs ADD COLUMN IF NOT EXISTS index_used VARCHAR(100);
ALTER TABLE slow_query_logs ADD COLUMN IF NOT EXISTS timestamp TIMESTAMP;

-- websocket_connection_logs
ALTER TABLE websocket_connection_logs ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);
ALTER TABLE websocket_connection_logs ADD COLUMN IF NOT EXISTS is_graceful_disconnect BOOLEAN;

-- websocket_message_logs
ALTER TABLE websocket_message_logs ADD COLUMN IF NOT EXISTS destination VARCHAR(255);
ALTER TABLE websocket_message_logs ADD COLUMN IF NOT EXISTS error_message TEXT;

-- message_queue_stats
ALTER TABLE message_queue_stats ADD COLUMN IF NOT EXISTS messages_published BIGINT DEFAULT 0;
ALTER TABLE message_queue_stats ADD COLUMN IF NOT EXISTS messages_consumed BIGINT DEFAULT 0;
ALTER TABLE message_queue_stats ADD COLUMN IF NOT EXISTS messages_acknowledged BIGINT DEFAULT 0;
ALTER TABLE message_queue_stats ADD COLUMN IF NOT EXISTS messages_rejected BIGINT DEFAULT 0;
ALTER TABLE message_queue_stats ADD COLUMN IF NOT EXISTS avg_processing_time_ms DOUBLE PRECISION;
ALTER TABLE message_queue_stats ADD COLUMN IF NOT EXISTS partition_count INTEGER;
ALTER TABLE message_queue_stats ADD COLUMN IF NOT EXISTS is_healthy BOOLEAN DEFAULT TRUE;
ALTER TABLE message_queue_stats ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP;

-- system_metric_snapshots
ALTER TABLE system_metric_snapshots ADD COLUMN IF NOT EXISTS host VARCHAR(100);
ALTER TABLE system_metric_snapshots ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP;

-- =====================================================
-- CX PACKAGE
-- =====================================================

-- nps_surveys
ALTER TABLE nps_surveys ADD COLUMN IF NOT EXISTS survey_trigger VARCHAR(50);
ALTER TABLE nps_surveys ADD COLUMN IF NOT EXISTS app_version VARCHAR(20);
ALTER TABLE nps_surveys ADD COLUMN IF NOT EXISTS device_type VARCHAR(30);

-- support_tickets
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS restaurant_id BIGINT;
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS courier_id BIGINT;
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP;
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS ticket_type VARCHAR(50);
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS assigned_to BIGINT;
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS customer_satisfaction_score INTEGER;
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS sla_breach BOOLEAN DEFAULT FALSE;
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS escalated BOOLEAN DEFAULT FALSE;
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS reopen_count INTEGER DEFAULT 0;

-- =====================================================
-- FRAUD PACKAGE
-- =====================================================

-- payment_attempts
ALTER TABLE payment_attempts ADD COLUMN IF NOT EXISTS gateway_response TEXT;
ALTER TABLE payment_attempts ADD COLUMN IF NOT EXISTS is_3ds_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE payment_attempts ADD COLUMN IF NOT EXISTS failure_code VARCHAR(50);

-- referral_events
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(255);
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS referrer_bonus DECIMAL(10,2);
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS referred_bonus DECIMAL(10,2);
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS phone_number_hash VARCHAR(255);
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS email_domain VARCHAR(100);
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS first_order_completed BOOLEAN DEFAULT FALSE;
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS first_order_id BIGINT;
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS fraud_flags VARCHAR(500);
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS status VARCHAR(30);
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS device_id VARCHAR(255);
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS is_suspicious BOOLEAN DEFAULT FALSE;
ALTER TABLE referral_events ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

-- promo_usage_logs
ALTER TABLE promo_usage_logs ADD COLUMN IF NOT EXISTS user_paid_amount DECIMAL(10,2);
ALTER TABLE promo_usage_logs ADD COLUMN IF NOT EXISTS is_referral_promo BOOLEAN DEFAULT FALSE;
ALTER TABLE promo_usage_logs ADD COLUMN IF NOT EXISTS status VARCHAR(30);
ALTER TABLE promo_usage_logs ADD COLUMN IF NOT EXISTS discount_value DECIMAL(10,2);
ALTER TABLE promo_usage_logs ADD COLUMN IF NOT EXISTS order_total DECIMAL(10,2);

-- fraud_order_history
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS delivery_address VARCHAR(500);
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS delivery_lat DOUBLE PRECISION;
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS delivery_lng DOUBLE PRECISION;
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS user_paid_amount DECIMAL(10,2);
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS is_promo_order BOOLEAN DEFAULT FALSE;
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS promo_code VARCHAR(50);
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS is_first_order BOOLEAN DEFAULT FALSE;
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS amount DECIMAL(10,2);
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS user_account_age_hours INTEGER;
ALTER TABLE fraud_order_history ADD COLUMN IF NOT EXISTS status VARCHAR(30);

-- device_fingerprints
ALTER TABLE device_fingerprints ADD COLUMN IF NOT EXISTS fingerprint_hash VARCHAR(255);
ALTER TABLE device_fingerprints ADD COLUMN IF NOT EXISTS device_brand VARCHAR(100);
ALTER TABLE device_fingerprints ADD COLUMN IF NOT EXISTS app_version VARCHAR(30);
ALTER TABLE device_fingerprints ADD COLUMN IF NOT EXISTS is_trusted BOOLEAN DEFAULT FALSE;
ALTER TABLE device_fingerprints ADD COLUMN IF NOT EXISTS times_seen INTEGER DEFAULT 1;

-- security_flags
ALTER TABLE security_flags ADD COLUMN IF NOT EXISTS evidence TEXT;
ALTER TABLE security_flags ADD COLUMN IF NOT EXISTS risk_score DOUBLE PRECISION;
ALTER TABLE security_flags ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);
ALTER TABLE security_flags ADD COLUMN IF NOT EXISTS device_id VARCHAR(255);
ALTER TABLE security_flags ADD COLUMN IF NOT EXISTS auto_detected BOOLEAN DEFAULT FALSE;
ALTER TABLE security_flags ADD COLUMN IF NOT EXISTS detection_rule VARCHAR(100);
ALTER TABLE security_flags ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(100);
ALTER TABLE security_flags ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE security_flags ADD COLUMN IF NOT EXISTS review_notes TEXT;

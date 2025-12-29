-- Financial Analytics Tables Migration
-- Version: V7
-- Description: Add tables for financial analytics module

-- Drop existing tables to handle schema changes from partial runs
DROP TABLE IF EXISTS referral_rewards CASCADE;
DROP TABLE IF EXISTS gift_card_usages CASCADE;
DROP TABLE IF EXISTS payout_disputes CASCADE;
DROP TABLE IF EXISTS restaurant_payouts CASCADE;
DROP TABLE IF EXISTS promotion_usages CASCADE;
DROP TABLE IF EXISTS courier_bonuses CASCADE;
DROP TABLE IF EXISTS courier_payments CASCADE;
DROP TABLE IF EXISTS restaurant_commissions CASCADE;

-- =============================================
-- Restaurant Commissions Table
-- Tracks commission earned per order
-- =============================================
CREATE TABLE restaurant_commissions (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_subtotal DECIMAL(10, 2) NOT NULL,
    commission_rate DECIMAL(5, 4),
    fixed_commission DECIMAL(10, 2),
    commission_amount DECIMAL(10, 2) NOT NULL,
    commission_type VARCHAR(20) NOT NULL,
    earned_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_commission_type CHECK (commission_type IN ('PERCENTAGE', 'FIXED', 'HYBRID', 'TIERED', 'PROMOTIONAL'))
);

CREATE INDEX idx_rc_restaurant_id ON restaurant_commissions(restaurant_id);
CREATE INDEX idx_rc_order_id ON restaurant_commissions(order_id);
CREATE INDEX idx_rc_earned_at ON restaurant_commissions(earned_at);
CREATE INDEX idx_rc_commission_type ON restaurant_commissions(commission_type);

-- =============================================
-- Courier Payments Table
-- Tracks payments made to couriers for deliveries
-- =============================================
CREATE TABLE courier_payments (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    delivery_id BIGINT,
    base_payment DECIMAL(10, 2) NOT NULL,
    distance_bonus DECIMAL(10, 2) DEFAULT 0,
    tip_amount DECIMAL(10, 2) DEFAULT 0,
    peak_bonus DECIMAL(10, 2) DEFAULT 0,
    total_payment DECIMAL(10, 2) NOT NULL,
    delivery_distance_km DECIMAL(8, 2),
    payment_type VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_date TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payment_type CHECK (payment_type IN ('PER_DELIVERY', 'HOURLY', 'BATCH', 'ADJUSTMENT')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_cp_courier_id ON courier_payments(courier_id);
CREATE INDEX idx_cp_order_id ON courier_payments(order_id);
CREATE INDEX idx_cp_payment_date ON courier_payments(payment_date);
CREATE INDEX idx_cp_payment_status ON courier_payments(payment_status);

-- =============================================
-- Courier Bonuses Table
-- Tracks incentives and bonuses for couriers
-- =============================================
CREATE TABLE courier_bonuses (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    bonus_type VARCHAR(30) NOT NULL,
    bonus_amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(255),
    qualifying_period_start TIMESTAMP,
    qualifying_period_end TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    earned_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_bonus_type CHECK (bonus_type IN ('SIGNUP_BONUS', 'COMPLETION_BONUS', 'STREAK_BONUS', 'PEAK_HOUR_BONUS', 'REFERRAL_BONUS', 'PERFORMANCE_BONUS', 'QUEST_BONUS', 'RETENTION_BONUS')),
    CONSTRAINT chk_bonus_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_cb_courier_id ON courier_bonuses(courier_id);
CREATE INDEX idx_cb_bonus_type ON courier_bonuses(bonus_type);
CREATE INDEX idx_cb_earned_at ON courier_bonuses(earned_at);
CREATE INDEX idx_cb_status ON courier_bonuses(status);

-- =============================================
-- Promotion Usages Table
-- Tracks usage of promotions and discounts
-- =============================================
CREATE TABLE promotion_usages (
    id BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    promotion_code VARCHAR(50),
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT,
    promotion_type VARCHAR(30) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    original_order_value DECIMAL(10, 2) NOT NULL,
    final_order_value DECIMAL(10, 2) NOT NULL,
    funded_by VARCHAR(20) NOT NULL,
    platform_cost DECIMAL(10, 2) DEFAULT 0,
    restaurant_cost DECIMAL(10, 2) DEFAULT 0,
    applied_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_promotion_type CHECK (promotion_type IN ('PERCENTAGE_DISCOUNT', 'FIXED_DISCOUNT', 'FREE_DELIVERY', 'BOGO', 'CASHBACK', 'BUNDLE_DISCOUNT', 'FIRST_ORDER', 'LOYALTY_REWARD')),
    CONSTRAINT chk_funded_by CHECK (funded_by IN ('PLATFORM', 'RESTAURANT', 'SHARED'))
);

CREATE INDEX idx_pu_promotion_id ON promotion_usages(promotion_id);
CREATE INDEX idx_pu_order_id ON promotion_usages(order_id);
CREATE INDEX idx_pu_user_id ON promotion_usages(user_id);
CREATE INDEX idx_pu_restaurant_id ON promotion_usages(restaurant_id);
CREATE INDEX idx_pu_applied_at ON promotion_usages(applied_at);
CREATE INDEX idx_pu_promotion_type ON promotion_usages(promotion_type);

-- =============================================
-- Restaurant Payouts Table
-- Tracks payouts to restaurants
-- =============================================
CREATE TABLE restaurant_payouts (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    payout_reference VARCHAR(50) UNIQUE NOT NULL,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    gross_amount DECIMAL(12, 2) NOT NULL,
    commission_deducted DECIMAL(10, 2) NOT NULL,
    delivery_subsidies DECIMAL(10, 2) DEFAULT 0,
    promotion_costs DECIMAL(10, 2) DEFAULT 0,
    adjustments DECIMAL(10, 2) DEFAULT 0,
    fees DECIMAL(10, 2) DEFAULT 0,
    net_payout DECIMAL(12, 2) NOT NULL,
    order_count INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    bank_account_last_four VARCHAR(4),
    payment_method VARCHAR(30),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payout_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_rp_restaurant_id ON restaurant_payouts(restaurant_id);
CREATE INDEX idx_rp_period_start ON restaurant_payouts(period_start);
CREATE INDEX idx_rp_period_end ON restaurant_payouts(period_end);
CREATE INDEX idx_rp_status ON restaurant_payouts(status);

-- =============================================
-- Payout Disputes Table
-- Tracks disputes raised on payouts
-- =============================================
CREATE TABLE payout_disputes (
    id BIGSERIAL PRIMARY KEY,
    payout_id BIGINT NOT NULL,
    entity_id BIGINT NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    dispute_type VARCHAR(30) NOT NULL,
    disputed_amount DECIMAL(10, 2) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution_notes TEXT,
    resolved_amount DECIMAL(10, 2),
    raised_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_entity_type CHECK (entity_type IN ('RESTAURANT', 'COURIER')),
    CONSTRAINT chk_dispute_type CHECK (dispute_type IN ('MISSING_ORDER', 'INCORRECT_COMMISSION', 'INCORRECT_DELIVERY_FEE', 'UNAUTHORIZED_DEDUCTION', 'REFUND_DISPUTE', 'LATE_PAYMENT', 'TECHNICAL_ERROR', 'OTHER')),
    CONSTRAINT chk_dispute_status CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED_FOR_CLAIMANT', 'RESOLVED_FOR_PLATFORM', 'PARTIALLY_RESOLVED', 'CLOSED', 'ESCALATED'))
);

CREATE INDEX idx_pd_payout_id ON payout_disputes(payout_id);
CREATE INDEX idx_pd_entity_id ON payout_disputes(entity_id);
CREATE INDEX idx_pd_entity_type ON payout_disputes(entity_type);
CREATE INDEX idx_pd_status ON payout_disputes(status);
CREATE INDEX idx_pd_raised_at ON payout_disputes(raised_at);

-- =============================================
-- Gift Card Usages Table
-- Tracks gift card redemptions
-- =============================================
CREATE TABLE gift_card_usages (
    id BIGSERIAL PRIMARY KEY,
    gift_card_id BIGINT NOT NULL,
    gift_card_code VARCHAR(50) NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    redeemed_amount DECIMAL(10, 2) NOT NULL,
    balance_before DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL,
    used_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gcu_gift_card_id ON gift_card_usages(gift_card_id);
CREATE INDEX idx_gcu_order_id ON gift_card_usages(order_id);
CREATE INDEX idx_gcu_user_id ON gift_card_usages(user_id);
CREATE INDEX idx_gcu_used_at ON gift_card_usages(used_at);

-- =============================================
-- Referral Rewards Table
-- Tracks referral program rewards
-- =============================================
CREATE TABLE referral_rewards (
    id BIGSERIAL PRIMARY KEY,
    referral_id BIGINT NOT NULL,
    referrer_id BIGINT NOT NULL,
    referred_user_id BIGINT NOT NULL,
    reward_type VARCHAR(20) NOT NULL,
    reward_amount DECIMAL(10, 2) NOT NULL,
    triggering_order_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    awarded_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_reward_type CHECK (reward_type IN ('REFERRER', 'REFERRED')),
    CONSTRAINT chk_reward_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_rr_referrer_id ON referral_rewards(referrer_id);
CREATE INDEX idx_rr_referred_id ON referral_rewards(referred_user_id);
CREATE INDEX idx_rr_awarded_at ON referral_rewards(awarded_at);
CREATE INDEX idx_rr_status ON referral_rewards(status);

-- =============================================
-- Add Comments for Documentation
-- =============================================
COMMENT ON TABLE restaurant_commissions IS 'Tracks commission earned by platform from restaurant orders';
COMMENT ON TABLE courier_payments IS 'Tracks payments made to couriers for deliveries';
COMMENT ON TABLE courier_bonuses IS 'Tracks incentives and bonuses awarded to couriers';
COMMENT ON TABLE promotion_usages IS 'Tracks usage of promotions and discounts with cost attribution';
COMMENT ON TABLE restaurant_payouts IS 'Tracks periodic payouts to restaurants';
COMMENT ON TABLE payout_disputes IS 'Tracks disputes raised on restaurant and courier payouts';
COMMENT ON TABLE gift_card_usages IS 'Tracks gift card redemptions and balance changes';
COMMENT ON TABLE referral_rewards IS 'Tracks referral program rewards for referrers and referred users';

COMMENT ON COLUMN restaurant_commissions.commission_type IS 'Type: PERCENTAGE, FIXED, HYBRID, TIERED, PROMOTIONAL';
COMMENT ON COLUMN courier_payments.payment_type IS 'Type: PER_DELIVERY, HOURLY, BATCH, ADJUSTMENT';
COMMENT ON COLUMN promotion_usages.funded_by IS 'Who funds the promotion: PLATFORM, RESTAURANT, SHARED';
COMMENT ON COLUMN restaurant_payouts.net_payout IS 'Net = Gross - Commission - Subsidies - Promo Costs + Adjustments - Fees';

-- Financial Analytics Tables
-- V4: Financial metrics and payment tracking

-- Drop existing tables if they exist (for idempotent migrations)
DO $$
BEGIN
    DROP TABLE IF EXISTS referral_rewards CASCADE;
    DROP TABLE IF EXISTS gift_card_usages CASCADE;
    DROP TABLE IF EXISTS payout_disputes CASCADE;
    DROP TABLE IF EXISTS restaurant_payouts CASCADE;
    DROP TABLE IF EXISTS promotion_usages CASCADE;
    DROP TABLE IF EXISTS courier_bonuses CASCADE;
    DROP TABLE IF EXISTS courier_payments CASCADE;
    DROP TABLE IF EXISTS restaurant_commissions CASCADE;
END $$;

-- Restaurant Commissions
CREATE TABLE restaurant_commissions (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_subtotal DECIMAL(12, 2) NOT NULL,
    commission_rate DECIMAL(5, 2) NOT NULL,
    fixed_commission DECIMAL(10, 2) DEFAULT 0,
    commission_amount DECIMAL(10, 2) NOT NULL,
    commission_type VARCHAR(20) NOT NULL,
    earned_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rc_restaurant_id ON restaurant_commissions(restaurant_id);
CREATE INDEX idx_rc_order_id ON restaurant_commissions(order_id);
CREATE INDEX idx_rc_earned_at ON restaurant_commissions(earned_at);
CREATE INDEX idx_rc_restaurant_earned ON restaurant_commissions(restaurant_id, earned_at);

-- Courier Payments
CREATE TABLE courier_payments (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    base_payment DECIMAL(10, 2) NOT NULL,
    distance_bonus DECIMAL(10, 2) DEFAULT 0,
    tip_amount DECIMAL(10, 2) DEFAULT 0,
    peak_bonus DECIMAL(10, 2) DEFAULT 0,
    total_payment DECIMAL(10, 2) NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
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
CREATE INDEX idx_cp_courier_date ON courier_payments(courier_id, payment_date);

-- Courier Bonuses
CREATE TABLE courier_bonuses (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    bonus_type VARCHAR(30) NOT NULL,
    bonus_amount DECIMAL(10, 2) NOT NULL,
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
CREATE INDEX idx_cb_courier_date ON courier_bonuses(courier_id, bonus_date);

-- Promotion Usages
CREATE TABLE promotion_usages (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    promotion_id BIGINT,
    user_id BIGINT NOT NULL,
    promo_code VARCHAR(50),
    promotion_type VARCHAR(30) NOT NULL,
    original_order_value DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    funded_by VARCHAR(20) NOT NULL,
    platform_cost DECIMAL(10, 2) DEFAULT 0,
    restaurant_cost DECIMAL(10, 2) DEFAULT 0,
    used_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pu_order_id ON promotion_usages(order_id);
CREATE INDEX idx_pu_promotion_id ON promotion_usages(promotion_id);
CREATE INDEX idx_pu_user_id ON promotion_usages(user_id);
CREATE INDEX idx_pu_used_at ON promotion_usages(used_at);
CREATE INDEX idx_pu_promo_type ON promotion_usages(promotion_type);

-- Restaurant Payouts
CREATE TABLE restaurant_payouts (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    gross_amount DECIMAL(12, 2) NOT NULL,
    commission_deducted DECIMAL(10, 2) NOT NULL,
    delivery_subsidies DECIMAL(10, 2) DEFAULT 0,
    promotion_costs DECIMAL(10, 2) DEFAULT 0,
    adjustments DECIMAL(10, 2) DEFAULT 0,
    fees DECIMAL(10, 2) DEFAULT 0,
    net_payout DECIMAL(12, 2) NOT NULL,
    order_count INTEGER NOT NULL,
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
CREATE INDEX idx_rp_period ON restaurant_payouts(period_start, period_end);
CREATE INDEX idx_rp_restaurant_date ON restaurant_payouts(restaurant_id, payout_date);

-- Payout Disputes
CREATE TABLE payout_disputes (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    payout_id BIGINT,
    order_id BIGINT,
    dispute_type VARCHAR(30) NOT NULL,
    disputed_amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolution VARCHAR(500),
    resolved_amount DECIMAL(10, 2),
    resolved_at TIMESTAMP,
    resolved_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pd_entity_id ON payout_disputes(entity_id);
CREATE INDEX idx_pd_entity_type ON payout_disputes(entity_type);
CREATE INDEX idx_pd_status ON payout_disputes(status);
CREATE INDEX idx_pd_created_at ON payout_disputes(created_at);

-- Gift Card Usages
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

CREATE INDEX idx_gcu_order_id ON gift_card_usages(order_id);
CREATE INDEX idx_gcu_gift_card_id ON gift_card_usages(gift_card_id);
CREATE INDEX idx_gcu_user_id ON gift_card_usages(user_id);
CREATE INDEX idx_gcu_used_at ON gift_card_usages(used_at);

-- Referral Rewards
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rr_referrer_id ON referral_rewards(referrer_id);
CREATE INDEX idx_rr_referred_id ON referral_rewards(referred_user_id);
CREATE INDEX idx_rr_awarded_at ON referral_rewards(awarded_at);
CREATE INDEX idx_rr_status ON referral_rewards(status);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_financial_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_restaurant_payouts_updated_at
    BEFORE UPDATE ON restaurant_payouts
    FOR EACH ROW EXECUTE FUNCTION update_financial_updated_at();

CREATE TRIGGER update_payout_disputes_updated_at
    BEFORE UPDATE ON payout_disputes
    FOR EACH ROW EXECUTE FUNCTION update_financial_updated_at();

COMMENT ON TABLE restaurant_commissions IS 'Tracks commission earned by platform from restaurant orders';
COMMENT ON TABLE courier_payments IS 'Tracks payments made to couriers for deliveries';
COMMENT ON TABLE courier_bonuses IS 'Tracks incentives and bonuses awarded to couriers';
COMMENT ON TABLE promotion_usages IS 'Tracks usage of promotions and discounts';
COMMENT ON TABLE restaurant_payouts IS 'Tracks periodic payouts to restaurants';
COMMENT ON TABLE payout_disputes IS 'Tracks disputes raised on payouts';
COMMENT ON TABLE gift_card_usages IS 'Tracks gift card redemptions';
COMMENT ON TABLE referral_rewards IS 'Tracks referral rewards issued';

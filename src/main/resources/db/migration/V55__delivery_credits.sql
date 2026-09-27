-- Free delivery: one for every new customer, and one for each side of a
-- referral.
--
-- A credit rather than a flag on the user, because three questions have to stay
-- answerable: has this person already had theirs, which order spent it, and
-- what did it cost us. A boolean answers none of them after the fact.
--
-- The credit discounts the delivery fee; it does NOT zero it. The courier is
-- paid deliveryFee + tip, so zeroing the fee would take the free delivery out
-- of the courier's pocket instead of the platform's. The order keeps its fee
-- and carries an equal discount, which is also what makes the invoice read
-- honestly: "delivery 8 000, discount 8 000".
CREATE TABLE delivery_credits (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT      NOT NULL REFERENCES users (id),

    -- WELCOME          — every new customer, once, ever
    -- REFERRAL_REWARD  — to the referrer, once per successful referral
    reason             VARCHAR(30) NOT NULL,

    source_referral_id BIGINT      REFERENCES referrals (id),

    -- Set when spent.
    order_id           BIGINT      REFERENCES orders (id),
    amount             DECIMAL(12, 2),

    granted_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at            TIMESTAMP,
    expires_at         TIMESTAMP
);

-- "Not twice to one person" is a rule about money, so the database holds it
-- rather than a service method that a second code path can forget to call.
CREATE UNIQUE INDEX uq_delivery_credits_welcome
    ON delivery_credits (user_id)
    WHERE reason = 'WELCOME';

-- One reward per referral, however many times completion is attempted — and it
-- will be attempted more than once, because the trigger is an order reaching
-- DELIVERED and events get redelivered.
CREATE UNIQUE INDEX uq_delivery_credits_referral
    ON delivery_credits (source_referral_id)
    WHERE source_referral_id IS NOT NULL;

-- The lookup on every delivery checkout: does this customer have one going
-- spare.
CREATE INDEX idx_delivery_credits_unused
    ON delivery_credits (user_id)
    WHERE used_at IS NULL;

-- Nothing is granted to existing accounts here. Every customer who registers
-- from this deploy on gets a welcome credit; the people already registered do
-- not, because handing out free deliveries to an existing list is a decision
-- about money and not one a migration should make quietly.
--
-- To grant it to everyone who has registered but never ordered:
--
--   INSERT INTO delivery_credits (user_id, reason)
--   SELECT u.id, 'WELCOME' FROM users u
--   WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.consumer_id = u.id)
--     AND NOT EXISTS (SELECT 1 FROM delivery_credits c
--                     WHERE c.user_id = u.id AND c.reason = 'WELCOME');

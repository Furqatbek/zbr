-- Make a promo code actually reduce what the customer pays.
--
-- CreateOrderRequest has carried a discountCode since promo codes were added.
-- OrderService never read it. Order.discount exists and is subtracted in
-- calculateTotals(), and nothing ever set it. So the flow was:
-- POST /orders/validate-promo told the customer the code was good and what they
-- would save, the app sent the code with the order, the field was accepted and
-- ignored, and the customer was charged full price. A discount that the UI
-- promises and the invoice omits is the worst shape this can take.
--
-- Two things were missing to fix it properly.

-- 1. Per-user enforcement. promo_codes.user_usage_limit has been on the table
--    since V12 and could never be enforced: usage_count is global, so there was
--    nowhere to record WHO used a code. A "first order free" campaign without
--    this is a code that every customer can use forever.
CREATE TABLE promo_code_usages (
    id              BIGSERIAL PRIMARY KEY,
    promo_code_id   BIGINT       NOT NULL REFERENCES promo_codes (id),
    user_id         BIGINT       NOT NULL REFERENCES users (id),
    order_id        BIGINT       NOT NULL REFERENCES orders (id),
    discount_amount DECIMAL(12, 2) NOT NULL,
    used_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Deliberately not a unique constraint on (promo_code_id, user_id):
-- user_usage_limit may legitimately be more than one. The limit is counted.
CREATE INDEX idx_promo_usages_code_user ON promo_code_usages (promo_code_id, user_id);
CREATE INDEX idx_promo_usages_order ON promo_code_usages (order_id);

-- 2. The code on the order itself. Without it a discounted total cannot be
--    explained after the fact — a refund, a dispute or a finance report sees an
--    amount that does not follow from the items.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS promo_code VARCHAR(50);

-- The two codes V12 seeded are template leftovers denominated in dollars:
-- SAVE5 takes 5.00 off an order over 25.00, and WELCOME10 gives 10% capped at
-- 20.00. In so'm those are meaningless — a 20 so'm cap on a 45 000 so'm order —
-- and until now it did not matter because no code was ever applied. From this
-- migration on they would be, so retire them rather than let a customer find
-- one. Deactivated, not deleted: they are evidence of what shipped.
UPDATE promo_codes
SET is_active = FALSE,
    description = description || ' (retired: seeded in USD, not valid in so''m)'
WHERE code IN ('WELCOME10', 'SAVE5')
  AND is_active = TRUE;

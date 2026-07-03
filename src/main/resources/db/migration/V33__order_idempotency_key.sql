-- Idempotency key for order creation: a retried or double-submitted "create
-- order" request (flaky network, double-tap) carries the same Idempotency-Key
-- header and resolves to the SAME order instead of creating a duplicate — which,
-- in a cash flow, would mean a duplicate delivery.
--
-- The column is nullable (legacy rows and keyless requests stay NULL); a PARTIAL
-- unique index enforces uniqueness only for non-null keys, so it also serves as
-- the concurrency backstop: two simultaneous requests with the same key race on
-- the index and exactly one order can be inserted.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(80);

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_idempotency_key
    ON orders (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

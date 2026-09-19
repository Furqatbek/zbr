-- Pushing orders OUT to a partner's POS, so the ticket prints in their kitchen.
--
-- The inbound half (V42) is them calling us. This is us calling them, which
-- needs three things V42 had no reason to store: where to call, what credential
-- they issued US, and whether a given venue has actually been switched on.

ALTER TABLE partners ADD COLUMN IF NOT EXISTS outbound_base_url   VARCHAR(500);
ALTER TABLE partners ADD COLUMN IF NOT EXISTS outbound_api_key    VARCHAR(500);
-- Which header their API expects the credential in. Restos take X-Partner-Key
-- or Authorization: Bearer; another partner will want something else, and
-- hard-coding one means a code change per integration.
ALTER TABLE partners ADD COLUMN IF NOT EXISTS outbound_auth_header VARCHAR(50);

-- Off by default, and per venue. Switching a restaurant's orders over to print
-- on someone else's till is a decision about one kitchen, not about a partner —
-- and the failure mode of getting it wrong is an order nobody cooks.
ALTER TABLE partner_venue_grants
    ADD COLUMN IF NOT EXISTS push_orders BOOLEAN NOT NULL DEFAULT FALSE;

-- One row per order we tried to push. Its own table rather than columns on
-- orders: this is integration bookkeeping with its own retry count and error
-- text, it only applies to a minority of orders, and it must not widen the row
-- every order read in the platform touches.
CREATE TABLE IF NOT EXISTS partner_order_pushes (
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT       NOT NULL REFERENCES orders(id),
    partner_id        BIGINT       NOT NULL REFERENCES partners(id),
    -- Our own order reference, sent as their idempotency key. Stored so a
    -- support question can be answered without joining back to orders.
    external_order_no VARCHAR(50)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    attempts          INT          NOT NULL DEFAULT 0,
    -- What the partner called the order on their side, when they tell us.
    partner_order_id  VARCHAR(100),
    last_error        VARCHAR(1000),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    delivered_at      TIMESTAMP
);

-- One push per order per partner. The unique index is the real guard against a
-- duplicate ticket: a redelivered message, a retry after a timeout and a manual
-- replay all collapse onto the same row, and a double print means a
-- double-cooked order rather than a cosmetic bug.
CREATE UNIQUE INDEX IF NOT EXISTS idx_partner_order_pushes_order
    ON partner_order_pushes(order_id, partner_id);

-- Finding what still needs sending, or what failed and needs a human.
CREATE INDEX IF NOT EXISTS idx_partner_order_pushes_status
    ON partner_order_pushes(status, created_at);

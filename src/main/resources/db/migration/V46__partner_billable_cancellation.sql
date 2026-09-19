-- A cancellation the partner refused because their kitchen had already started.
--
-- We cancel and refund on our side; they answer 422 and keep the ticket open,
-- because their cutoff says the venue is owed for food it has already made.
-- That refusal is not an integration fault and must not be retried — it is a
-- fact about money, and the only place it can be recorded is here.
--
-- Recorded now, before the commercial question is settled, so that when the
-- answer arrives it can be applied to orders that already happened rather than
-- only to future ones. A status code whose meaning changes later is worse than
-- a column that was empty for a while.
ALTER TABLE partner_order_pushes
    ADD COLUMN IF NOT EXISTS venue_owed_at TIMESTAMP;
ALTER TABLE partner_order_pushes
    ADD COLUMN IF NOT EXISTS venue_owed_reason VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_partner_order_pushes_venue_owed
    ON partner_order_pushes(venue_owed_at)
    WHERE venue_owed_at IS NOT NULL;

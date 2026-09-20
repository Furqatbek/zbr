-- What the food on a refused cancellation was worth.
--
-- We recorded THAT a venue was owed for a ticket and not what it came to, so
-- the two companies' month-end lists could be compared by row count and by
-- nothing else. Equal totals hide two offsetting errors; matching ticket by
-- ticket needs a number on each row.
--
-- Deliberately the goods at the partner's published prices and nothing else:
-- no delivery fee, because nobody drove it and nobody earned it; no service fee
-- of ours; no tip. It is expectedTotal MINUS the delivery fee, which is a
-- distinction one word wide and different on every delivery order.
ALTER TABLE partner_order_pushes
    ADD COLUMN IF NOT EXISTS venue_owed_amount NUMERIC(10, 2);

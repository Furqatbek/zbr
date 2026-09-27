-- A cancellation reason you can count.
--
-- The customer app sends `reason` as the localised display string it showed the
-- customer, so three people picking the same reason send three different values:
-- "Wrong delivery address", "Неверный адрес доставки", "Noto'g'ri yetkazib
-- berish manzili". The column therefore holds free text in three languages and
-- cannot be grouped.
--
-- The cost is not tidiness. "Wrong address" is the most operationally useful
-- signal in the whole cancellation flow — it means the address step failed —
-- and today it is undetectable without matching strings in three languages.
-- Restaurant staff also read the customer's language rather than their own.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancellation_reason_code VARCHAR(40);

-- Deliberately a VARCHAR and not an enum type.
--
-- The app team held back from sending a code speculatively because a value we
-- did not recognise might break cancelling outright — a reasonable fear, and a
-- closed enum would have made it real: an unknown value fails Jackson binding
-- and answers 400 at the moment a customer is trying to cancel. A short
-- constrained string means a new reason in a shipped app is recorded rather
-- than refused, and the vocabulary can be narrowed later once it has settled.
--
-- Recommended values, matching the app's own keys:
--   WRONG_ADDRESS, ORDERED_BY_MISTAKE, TOO_SLOW, CHANGED_MIND, OTHER
--
-- Nothing is backfilled. The existing rows hold display strings in three
-- languages and guessing a code from them would invent data that looks
-- authoritative. They stay uncoded, and reports should say so rather than
-- treating the gap as zero.
CREATE INDEX IF NOT EXISTS idx_orders_cancellation_reason_code
    ON orders (cancellation_reason_code)
    WHERE cancellation_reason_code IS NOT NULL;

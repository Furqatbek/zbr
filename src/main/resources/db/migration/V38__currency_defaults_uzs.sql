-- Currency defaults: USD -> UZS
--
-- The platform operates in Uzbekistan and restaurants were always created with
-- currency 'UZS' (V1 line 123), but payments, referrals and chargeback_events
-- were declared DEFAULT 'USD'. Combined with a hardcoded "USD" in PaymentService
-- that meant every payment row carried the wrong currency label while holding a
-- som amount — the number was right, the unit was a lie.
--
-- Amounts are NOT converted: nothing here is a currency conversion, and no rate
-- is applied. The stored figures were always som; only the label changes. This
-- is safe precisely because it runs before the first real order — if this
-- migration ever needs backporting to a database with genuine USD rows, it must
-- be rewritten to discriminate by row rather than blanket-update.

ALTER TABLE payments          ALTER COLUMN currency SET DEFAULT 'UZS';
ALTER TABLE referrals         ALTER COLUMN currency SET DEFAULT 'UZS';
ALTER TABLE chargeback_events ALTER COLUMN currency SET DEFAULT 'UZS';

-- Relabel existing rows (seed/demo data at this point in the project's life).
UPDATE payments          SET currency = 'UZS' WHERE currency = 'USD';
UPDATE referrals         SET currency = 'UZS' WHERE currency = 'USD';
UPDATE chargeback_events SET currency = 'UZS' WHERE currency = 'USD';

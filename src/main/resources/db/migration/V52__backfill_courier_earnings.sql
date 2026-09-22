-- Rebuild couriers.total_earnings from the deliveries that produced it.
--
-- The column was incremented by a flat 5.00 per completed delivery — dollar
-- shaped scaffolding on a platform that trades in so'm, where five is less than
-- a bus fare. It is shown to couriers as lifetime earnings and as
-- average-per-delivery, beside a weekly figure computed properly from orders,
-- so one screen carried two incompatible answers to "what am I paid".
--
-- Recomputed on the same definition every earnings query uses: the delivery fee
-- plus any tip, for orders this courier delivered. DELIVERED **and** COMPLETED,
-- because a delivered order becomes completed an hour later and excluding it
-- was its own bug.
--
-- REFUNDED is left out here as it is in the queries. Whether a courier keeps
-- the fee on a refunded order is a policy question nobody has answered, and a
-- backfill is not the place to answer it.
UPDATE couriers c
SET total_earnings = COALESCE((
        SELECT SUM(COALESCE(o.delivery_fee, 0) + COALESCE(o.tip_amount, 0))
        FROM orders o
        WHERE o.courier_id = c.id
          AND o.status IN ('DELIVERED', 'COMPLETED')
          AND o.delivered_at IS NOT NULL
    ), 0);

-- total_deliveries is recomputed the same way. It was incremented in the same
-- place, so a courier whose order was reassigned away still carried the count.
UPDATE couriers c
SET total_deliveries = COALESCE((
        SELECT COUNT(*)
        FROM orders o
        WHERE o.courier_id = c.id
          AND o.status IN ('DELIVERED', 'COMPLETED')
          AND o.delivered_at IS NOT NULL
    ), 0);

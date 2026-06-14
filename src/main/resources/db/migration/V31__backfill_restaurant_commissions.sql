-- Backfill commission records for existing delivered/completed orders that
-- don't yet have one. Commission service was added after these orders existed,
-- so the restaurant financial report showed zero despite real order history.

INSERT INTO restaurant_commissions
    (restaurant_id, order_id, order_subtotal, commission_rate, fixed_commission,
     commission_amount, commission_type, earned_at, created_at)
SELECT
    o.restaurant_id,
    o.id,
    o.subtotal,
    COALESCE(r.commission_rate, 15.00) AS commission_rate,
    0.00 AS fixed_commission,
    ROUND(o.subtotal * COALESCE(r.commission_rate, 15.00) / 100.0, 2) AS commission_amount,
    'PERCENTAGE' AS commission_type,
    COALESCE(o.delivered_at, o.completed_at, o.updated_at, o.created_at) AS earned_at,
    CURRENT_TIMESTAMP AS created_at
FROM orders o
JOIN restaurants r ON r.id = o.restaurant_id
WHERE o.status IN ('DELIVERED', 'COMPLETED')
  AND NOT EXISTS (
      SELECT 1 FROM restaurant_commissions rc WHERE rc.order_id = o.id
  );

-- Courier's rating of a completed delivery (the courier app force-shows a rating
-- screen after each delivery). This is the courier -> order/customer direction and
-- is distinct from the consumer-authored `reviews` table (consumer -> restaurant/
-- courier/food). Stored inline on the order, 1:1, mirroring consumer_rating.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS courier_delivery_rating SMALLINT
    CHECK (courier_delivery_rating >= 1 AND courier_delivery_rating <= 5);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS courier_delivery_comment VARCHAR(1000);

-- Add restaurant_id column to notifications for restaurant-specific notifications
ALTER TABLE notifications ADD COLUMN restaurant_id BIGINT;

-- Add index for restaurant_id
CREATE INDEX idx_notifications_restaurant_id ON notifications(restaurant_id);

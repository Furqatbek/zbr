-- =====================================================
-- V3: Seed Data
-- =====================================================

-- Insert default admin user
INSERT INTO users (email, password_hash, first_name, last_name, role, status, email_verified, created_at)
VALUES ('admin@fooddelivery.com', '$2a$10$dummyhashedpassword', 'Admin', 'User', 'ADMIN', 'ACTIVE', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Insert default notification templates
INSERT INTO notification_templates (notification_type, role, title_template, message_template, icon, active, default_ttl_hours, created_at)
VALUES
    ('ORDER_CREATED', 'CUSTOMER', 'Order Placed!', 'Your order #{orderId} has been placed successfully.', 'order', TRUE, 168, CURRENT_TIMESTAMP),
    ('ORDER_ACCEPTED', 'CUSTOMER', 'Order Confirmed!', 'Your order #{orderId} has been accepted by the restaurant.', 'check', TRUE, 168, CURRENT_TIMESTAMP),
    ('ORDER_PREPARING', 'CUSTOMER', 'Order Preparing', 'Your order #{orderId} is being prepared.', 'cooking', TRUE, 168, CURRENT_TIMESTAMP),
    ('ORDER_READY', 'CUSTOMER', 'Order Ready!', 'Your order #{orderId} is ready for pickup.', 'ready', TRUE, 168, CURRENT_TIMESTAMP),
    ('ORDER_PICKED_UP', 'CUSTOMER', 'On the Way!', 'Your order #{orderId} is on its way to you.', 'delivery', TRUE, 168, CURRENT_TIMESTAMP),
    ('ORDER_DELIVERED', 'CUSTOMER', 'Delivered!', 'Your order #{orderId} has been delivered. Enjoy!', 'delivered', TRUE, 168, CURRENT_TIMESTAMP),
    ('ORDER_CANCELLED', 'CUSTOMER', 'Order Cancelled', 'Your order #{orderId} has been cancelled.', 'cancelled', TRUE, 168, CURRENT_TIMESTAMP),
    ('NEW_ORDER', 'RESTAURANT', 'New Order!', 'You have a new order #{orderId} to prepare.', 'order', TRUE, 24, CURRENT_TIMESTAMP),
    ('ORDER_ASSIGNED', 'COURIER', 'New Delivery!', 'You have been assigned order #{orderId}.', 'delivery', TRUE, 24, CURRENT_TIMESTAMP),
    ('PAYMENT_RECEIVED', 'CUSTOMER', 'Payment Confirmed', 'Your payment for order #{orderId} has been confirmed.', 'payment', TRUE, 168, CURRENT_TIMESTAMP),
    ('PAYMENT_FAILED', 'CUSTOMER', 'Payment Failed', 'Your payment for order #{orderId} failed. Please try again.', 'error', TRUE, 168, CURRENT_TIMESTAMP)
ON CONFLICT (notification_type, role) DO NOTHING;

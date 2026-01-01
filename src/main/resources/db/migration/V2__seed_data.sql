-- Food Delivery Platform - Seed Data
-- Flyway Migration V2

-- Insert admin user (password: Admin@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('admin@fooddelivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', '+1234567890', 'ADMIN', 'ACTIVE', true);

-- Insert platform user (password: Platform@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('platform@fooddelivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Platform Manager', '+1234567891', 'PLATFORM', 'ACTIVE', true);

-- Insert demo restaurant owner (password: Owner@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('owner@pizzapalace.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Mario Rossi', '+1234567892', 'RESTAURANT_OWNER', 'ACTIVE', true);

-- Insert demo consumer (password: Consumer@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('john.doe@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John Doe', '+1234567893', 'CONSUMER', 'ACTIVE', true);

-- Insert demo courier (password: Courier@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('courier@fooddelivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Fast Eddie', '+1234567894', 'COURIER', 'ACTIVE', true);

-- Insert demo restaurant
INSERT INTO restaurants (owner_id, name, slug, description, cuisine_type, address_line1, city, state, postal_code, country, latitude, longitude, phone, email, status, avg_prep_time, minimum_order, delivery_fee, delivery_radius_km, accepts_pickup, accepts_delivery, is_featured, is_online)
VALUES (
    (SELECT id FROM users WHERE email = 'owner@pizzapalace.com'),
    'Pizza Palace',
    'pizza-palace',
    'Authentic Italian pizza made with love. Fresh ingredients, traditional recipes.',
    'Italian',
    '123 Main Street',
    'New York',
    'NY',
    '10001',
    'US',
    40.7128,
    -74.0060,
    '+1234567800',
    'info@pizzapalace.com',
    'ACTIVE',
    25,
    15.00,
    3.99,
    10.00,
    true,
    true,
    true,
    true
);

-- Insert operating hours for Pizza Palace (Mon-Sun, 11:00-22:00)
INSERT INTO restaurant_operating_hours (restaurant_id, day_of_week, open_time, close_time, is_closed)
SELECT id, day, '11:00:00', '22:00:00', false
FROM restaurants, generate_series(0, 6) AS day
WHERE slug = 'pizza-palace';

-- Insert menu categories
INSERT INTO menu_categories (restaurant_id, name, description, display_order, is_active)
SELECT id, 'Pizzas', 'Our signature hand-tossed pizzas', 1, true FROM restaurants WHERE slug = 'pizza-palace';

INSERT INTO menu_categories (restaurant_id, name, description, display_order, is_active)
SELECT id, 'Pastas', 'Fresh pasta dishes', 2, true FROM restaurants WHERE slug = 'pizza-palace';

INSERT INTO menu_categories (restaurant_id, name, description, display_order, is_active)
SELECT id, 'Appetizers', 'Start your meal right', 3, true FROM restaurants WHERE slug = 'pizza-palace';

INSERT INTO menu_categories (restaurant_id, name, description, display_order, is_active)
SELECT id, 'Beverages', 'Drinks and refreshments', 4, true FROM restaurants WHERE slug = 'pizza-palace';

-- Insert menu items - Pizzas
INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Margherita', 'Classic tomato sauce, fresh mozzarella, basil', 14.99, true, true, true, 0, 20, 1
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pizzas';

INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Pepperoni', 'Tomato sauce, mozzarella, pepperoni', 16.99, true, true, false, 1, 20, 2
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pizzas';

INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Supreme', 'Pepperoni, sausage, mushrooms, peppers, onions', 19.99, true, false, false, 1, 25, 3
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pizzas';

-- Insert courier for demo user
INSERT INTO couriers (user_id, name, vehicle_type, license_plate, status, is_available)
SELECT id, 'Fast Eddie', 'BICYCLE', NULL, 'ONLINE', true FROM users WHERE email = 'courier@fooddelivery.com';

-- Insert a referral code for the admin
INSERT INTO referrals (referrer_id, code, status, referrer_reward, referred_reward, expires_at)
SELECT id, 'ADMIN2024', 'PENDING', 20.00, 15.00, CURRENT_TIMESTAMP + INTERVAL '90 days' FROM users WHERE email = 'admin@fooddelivery.com';

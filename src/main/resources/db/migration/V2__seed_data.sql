-- Food Delivery Platform - Seed Data
-- Flyway Migration V2

-- Insert admin user (password: Admin@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('admin@fooddelivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', '+1234567890', 'ADMIN', 'ACTIVE', true)
ON CONFLICT (email) DO NOTHING;

-- Insert platform user (password: Platform@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('platform@fooddelivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Platform Manager', '+1234567891', 'PLATFORM', 'ACTIVE', true)
ON CONFLICT (email) DO NOTHING;

-- Insert demo restaurant owner (password: Owner@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('owner@pizzapalace.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Mario Rossi', '+1234567892', 'RESTAURANT_OWNER', 'ACTIVE', true)
ON CONFLICT (email) DO NOTHING;

-- Insert demo consumer (password: Consumer@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('john.doe@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John Doe', '+1234567893', 'CONSUMER', 'ACTIVE', true)
ON CONFLICT (email) DO NOTHING;

-- Insert demo courier (password: Courier@123)
INSERT INTO users (email, password_hash, full_name, phone, role, status, email_verified)
VALUES ('courier@fooddelivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Fast Eddie', '+1234567894', 'COURIER', 'ACTIVE', true)
ON CONFLICT (email) DO NOTHING;

-- Insert demo restaurant
INSERT INTO restaurants (owner_id, name, slug, description, cuisine_type, address_line1, city, state, postal_code, country, latitude, longitude, phone, email, status, avg_prep_time, minimum_order, delivery_fee, delivery_radius_km, accepts_pickup, accepts_delivery, is_featured)
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
    true
)
ON CONFLICT (slug) DO NOTHING;

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

INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Quattro Formaggi', 'Four cheese blend: mozzarella, gorgonzola, parmesan, fontina', 18.99, true, false, true, 0, 20, 4
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pizzas';

-- Insert menu items - Pastas
INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Spaghetti Bolognese', 'Traditional meat sauce with spaghetti', 15.99, true, false, false, 0, 15, 1
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pastas';

INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Fettuccine Alfredo', 'Creamy parmesan sauce with fettuccine', 14.99, true, false, true, 0, 15, 2
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pastas';

INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Penne Arrabbiata', 'Spicy tomato sauce with penne', 13.99, true, false, true, 3, 15, 3
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pastas';

-- Insert menu items - Appetizers
INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Garlic Bread', 'Toasted bread with garlic butter', 5.99, true, false, true, 0, 10, 1
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Appetizers';

INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Mozzarella Sticks', 'Breaded mozzarella with marinara sauce', 8.99, true, true, true, 0, 12, 2
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Appetizers';

-- Insert menu items - Beverages
INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Soft Drink', 'Coke, Sprite, or Fanta', 2.99, true, false, true, 0, 1, 1
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Beverages';

INSERT INTO menu_items (restaurant_id, category_id, name, description, base_price, is_available, is_featured, is_vegetarian, spice_level, prep_time_minutes, display_order)
SELECT r.id, c.id, 'Italian Soda', 'Refreshing fruit-flavored soda', 3.99, true, false, true, 0, 2, 2
FROM restaurants r JOIN menu_categories c ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Beverages';

-- Insert item variants for pizzas (sizes)
INSERT INTO item_variants (menu_item_id, name, price_adjustment, is_default, is_available, display_order)
SELECT id, 'Small (10")', -4.00, false, true, 1 FROM menu_items WHERE name IN ('Margherita', 'Pepperoni', 'Supreme', 'Quattro Formaggi');

INSERT INTO item_variants (menu_item_id, name, price_adjustment, is_default, is_available, display_order)
SELECT id, 'Medium (12")', 0.00, true, true, 2 FROM menu_items WHERE name IN ('Margherita', 'Pepperoni', 'Supreme', 'Quattro Formaggi');

INSERT INTO item_variants (menu_item_id, name, price_adjustment, is_default, is_available, display_order)
SELECT id, 'Large (14")', 4.00, false, true, 3 FROM menu_items WHERE name IN ('Margherita', 'Pepperoni', 'Supreme', 'Quattro Formaggi');

INSERT INTO item_variants (menu_item_id, name, price_adjustment, is_default, is_available, display_order)
SELECT id, 'Family (16")', 8.00, false, true, 4 FROM menu_items WHERE name IN ('Margherita', 'Pepperoni', 'Supreme', 'Quattro Formaggi');

-- Insert item options for pizzas (toppings)
INSERT INTO item_options (menu_item_id, name, price, is_available, max_selections, is_required, option_group)
SELECT id, 'Extra Cheese', 2.00, true, 1, false, 'extras' FROM menu_items WHERE name IN ('Margherita', 'Pepperoni', 'Supreme', 'Quattro Formaggi');

INSERT INTO item_options (menu_item_id, name, price, is_available, max_selections, is_required, option_group)
SELECT id, 'Mushrooms', 1.50, true, 1, false, 'toppings' FROM menu_items WHERE name IN ('Margherita', 'Pepperoni', 'Supreme', 'Quattro Formaggi');

INSERT INTO item_options (menu_item_id, name, price, is_available, max_selections, is_required, option_group)
SELECT id, 'Olives', 1.50, true, 1, false, 'toppings' FROM menu_items WHERE name IN ('Margherita', 'Pepperoni', 'Supreme', 'Quattro Formaggi');

INSERT INTO item_options (menu_item_id, name, price, is_available, max_selections, is_required, option_group)
SELECT id, 'Jalapenos', 1.00, true, 1, false, 'toppings' FROM menu_items WHERE name IN ('Margherita', 'Pepperoni', 'Supreme', 'Quattro Formaggi');

-- Insert courier for demo user
INSERT INTO couriers (user_id, vehicle_type, license_plate, status, is_available)
SELECT id, 'BICYCLE', NULL, 'ONLINE', true FROM users WHERE email = 'courier@fooddelivery.com'
ON CONFLICT (user_id) DO NOTHING;

-- Insert notification preferences for all users
INSERT INTO notification_preferences (user_id, email_enabled, sms_enabled, push_enabled, order_updates, promotions, newsletter)
SELECT id, true, true, true, true, true, false FROM users
ON CONFLICT (user_id) DO NOTHING;

-- Insert a referral code for the admin
INSERT INTO referrals (referrer_id, code, status, referrer_reward, referred_reward, expires_at)
SELECT id, 'ADMIN2024', 'PENDING', 20.00, 15.00, CURRENT_TIMESTAMP + INTERVAL '90 days' FROM users WHERE email = 'admin@fooddelivery.com'
ON CONFLICT (code) DO NOTHING;

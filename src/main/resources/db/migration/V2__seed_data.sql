-- Food Delivery Platform - Seed Data
-- Flyway Migration V2

-- Insert admin user (password: Admin@123)
INSERT INTO users (email, password_hash, first_name, last_name, phone, role, status, email_verified)
VALUES ('admin@fooddelivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System', 'Admin', '+1234567890', 'ADMIN', 'ACTIVE', true);

-- Insert platform user (password: Platform@123)
INSERT INTO users (email, password_hash, first_name, last_name, phone, role, status, email_verified)
VALUES ('platform@fooddelivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Platform', 'Manager', '+1234567891', 'PLATFORM', 'ACTIVE', true);

-- Insert demo restaurant owner (password: Owner@123)
INSERT INTO users (email, password_hash, first_name, last_name, phone, role, status, email_verified)
VALUES ('owner@pizzapalace.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Mario', 'Rossi', '+1234567892', 'RESTAURANT_OWNER', 'ACTIVE', true);

-- Insert demo consumer (password: Consumer@123)
INSERT INTO users (email, password_hash, first_name, last_name, phone, role, status, email_verified)
VALUES ('john.doe@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John', 'Doe', '+1234567893', 'CONSUMER', 'ACTIVE', true);

-- Insert demo courier (password: Courier@123)
INSERT INTO users (email, password_hash, first_name, last_name, phone, role, status, email_verified)
VALUES ('courier@fooddelivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Fast', 'Eddie', '+1234567894', 'COURIER', 'ACTIVE', true);

-- Insert demo restaurant
INSERT INTO restaurants (owner_id, name, slug, description, address_line1, city, state, postal_code, country, latitude, longitude, phone, email, status, average_prep_time_minutes, minimum_order, delivery_fee, delivery_radius_km, accepts_takeaway, accepts_delivery, is_featured, is_open)
VALUES (
    (SELECT id FROM users WHERE email = 'owner@pizzapalace.com'),
    'Pizza Palace',
    'pizza-palace',
    'Authentic Italian pizza made with love. Fresh ingredients, traditional recipes.',
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
    10,
    true,
    true,
    true,
    true
);

-- Insert menu categories (using sort_order and active to match entity)
INSERT INTO menu_categories (restaurant_id, name, description, sort_order, active)
SELECT id, 'Pizzas', 'Our signature hand-tossed pizzas', 1, true FROM restaurants WHERE slug = 'pizza-palace';

INSERT INTO menu_categories (restaurant_id, name, description, sort_order, active)
SELECT id, 'Pastas', 'Fresh pasta dishes', 2, true FROM restaurants WHERE slug = 'pizza-palace';

INSERT INTO menu_categories (restaurant_id, name, description, sort_order, active)
SELECT id, 'Appetizers', 'Start your meal right', 3, true FROM restaurants WHERE slug = 'pizza-palace';

INSERT INTO menu_categories (restaurant_id, name, description, sort_order, active)
SELECT id, 'Beverages', 'Drinks and refreshments', 4, true FROM restaurants WHERE slug = 'pizza-palace';

-- Insert menu items - Pizzas (using price, in_stock, featured, is_spicy, sort_order to match entity)
INSERT INTO menu_items (category_id, name, description, price, in_stock, featured, is_vegetarian, is_spicy, prep_time_minutes, sort_order)
SELECT c.id, 'Margherita', 'Classic tomato sauce, fresh mozzarella, basil', 14.99, true, true, true, false, 20, 1
FROM menu_categories c JOIN restaurants r ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pizzas';

INSERT INTO menu_items (category_id, name, description, price, in_stock, featured, is_vegetarian, is_spicy, prep_time_minutes, sort_order)
SELECT c.id, 'Pepperoni', 'Tomato sauce, mozzarella, pepperoni', 16.99, true, true, false, false, 20, 2
FROM menu_categories c JOIN restaurants r ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pizzas';

INSERT INTO menu_items (category_id, name, description, price, in_stock, featured, is_vegetarian, is_spicy, prep_time_minutes, sort_order)
SELECT c.id, 'Supreme', 'Pepperoni, sausage, mushrooms, peppers, onions', 19.99, true, false, false, false, 25, 3
FROM menu_categories c JOIN restaurants r ON r.id = c.restaurant_id WHERE r.slug = 'pizza-palace' AND c.name = 'Pizzas';

-- Insert courier for demo user
INSERT INTO couriers (user_id, status, vehicle_type)
SELECT id, 'AVAILABLE', 'BICYCLE' FROM users WHERE email = 'courier@fooddelivery.com';

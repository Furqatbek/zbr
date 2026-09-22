-- Cuisine categories: the chips above the restaurant list.
--
-- Not to be confused with menu_categories, which group dishes INSIDE one
-- restaurant. This groups restaurants, and the two have nothing to do with
-- each other beyond the word.
--
-- Names are stored per language rather than once. The app renders what we send
-- verbatim and ships in three languages, so a single name would be wrong for
-- two thirds of its customers — and renaming a category after restaurants are
-- assigned to it is the expensive version of this conversation.

CREATE TABLE restaurant_categories (
    id          BIGSERIAL PRIMARY KEY,

    -- Stable identity for anything that must not move when a name is edited:
    -- analytics, deep links, a hard-coded chip order in a future client.
    slug        VARCHAR(80)  NOT NULL UNIQUE,

    -- Uzbek is the one that must exist. The others fall back to it rather than
    -- rendering blank, so a category added in a hurry is still usable.
    name_uz     VARCHAR(120) NOT NULL,
    name_ru     VARCHAR(120),
    name_en     VARCHAR(120),

    image_url   VARCHAR(500),
    sort_order  INT          NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Nullable on purpose. Four restaurants already exist and nobody can say what
-- cuisine they are without asking them; guessing would put a wrong chip on a
-- real venue. The API therefore returns category: null until an admin assigns
-- one, and the app has been told to expect that.
ALTER TABLE restaurants
    ADD COLUMN category_id BIGINT REFERENCES restaurant_categories (id);

CREATE INDEX idx_restaurants_category ON restaurants (category_id);
CREATE INDEX idx_restaurant_categories_active ON restaurant_categories (active, sort_order);

-- A starting set, in the three languages the app ships in. Edit, reorder,
-- deactivate or add through the admin API — none of this is load-bearing, it
-- is a first draft so the screen has something to show.
INSERT INTO restaurant_categories (slug, name_uz, name_ru, name_en, sort_order) VALUES
    ('national',  'Milliy taomlar', 'Национальная кухня', 'National',   10),
    ('fast-food', 'Fast food',      'Фастфуд',            'Fast food',  20),
    ('burgers',   'Burgerlar',      'Бургеры',            'Burgers',    30),
    ('pizza',     'Pitsa',          'Пицца',              'Pizza',      40),
    ('shashlik',  'Shashlik',       'Шашлык',             'Grill',      50),
    ('coffee',    'Kofe',           'Кофе',               'Coffee',     60),
    ('desserts',  'Shirinliklar',   'Десерты',            'Desserts',   70),
    ('sushi',     'Sushi',          'Суши',               'Sushi',      80),
    ('drinks',    'Ichimliklar',    'Напитки',            'Drinks',     90);

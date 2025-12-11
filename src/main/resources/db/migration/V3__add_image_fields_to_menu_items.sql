-- Add image fields to menu_items table
-- Flyway Migration V3

ALTER TABLE menu_items
    ADD COLUMN IF NOT EXISTS image_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS image_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS image_size BIGINT,
    ADD COLUMN IF NOT EXISTS image_content_type VARCHAR(100);

-- Add index for image lookups
CREATE INDEX IF NOT EXISTS idx_menu_items_image_path ON menu_items(image_path);

-- Comment for documentation
COMMENT ON COLUMN menu_items.image_path IS 'Server filesystem path to the stored image';
COMMENT ON COLUMN menu_items.image_name IS 'Original filename of the uploaded image';
COMMENT ON COLUMN menu_items.image_size IS 'File size in bytes';
COMMENT ON COLUMN menu_items.image_content_type IS 'MIME type of the image (e.g., image/jpeg)';

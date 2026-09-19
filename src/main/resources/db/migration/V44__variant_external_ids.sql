-- Variants imported from a partner POS need their id kept, for the same reason
-- products do: an order we push names the variant in THEIR numbering, and a
-- dish sold by size is refused outright if we cannot say which size.
--
-- Before this, the import dropped variants entirely. Every Restos dish sold by
-- size arrived here as one item at the base price, with no size to choose — so
-- a Large was orderable only as a Regular, and the kitchen would have cooked
-- what the ticket did not say.

ALTER TABLE item_variants ADD COLUMN IF NOT EXISTS external_id BIGINT;
ALTER TABLE item_variants ADD COLUMN IF NOT EXISTS external_source VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS idx_item_variants_external
    ON item_variants(menu_item_id, external_source, external_id)
    WHERE external_id IS NOT NULL;

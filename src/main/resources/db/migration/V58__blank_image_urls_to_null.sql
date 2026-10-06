-- An empty string is not a URL.
--
-- Restos sends imageUrl "" for products and categories that have no picture,
-- and the import stored it verbatim. Jackson is configured
-- default-property-inclusion: non_null, so a null is absent from the response
-- and a client can treat missing as "nothing here" — but "" serialises, so the
-- QR landing page received imageUrl: "" on all 45 items of Qahvoon's menu and
-- rendered a broken image frame for each one.
--
-- The import now normalises blank to null. These are the rows written before
-- it did.
UPDATE menu_items      SET image_url = NULL WHERE image_url IS NOT NULL AND TRIM(image_url) = '';
UPDATE menu_categories SET image_url = NULL WHERE image_url IS NOT NULL AND TRIM(image_url) = '';

-- The same shape, found by the customer app on a restaurant's email. Cheap to
-- fix while we are here, and left as "" it is a value that reads as present.
UPDATE restaurants SET email        = NULL WHERE email        IS NOT NULL AND TRIM(email) = '';
UPDATE restaurants SET logo_url     = NULL WHERE logo_url     IS NOT NULL AND TRIM(logo_url) = '';
UPDATE restaurants SET cover_image_url = NULL WHERE cover_image_url IS NOT NULL AND TRIM(cover_image_url) = '';

-- Deliberately only these columns. A blanket sweep over every text column in
-- the schema would also rewrite rows nobody has complained about, including
-- places where "" may carry meaning, and a migration is the wrong instrument
-- for a change nobody can review.

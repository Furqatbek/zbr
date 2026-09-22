-- Undo the 10% that was added to prices restaurants typed themselves.
--
-- menu_items.price is what the venue entered; price_with_margin is what the
-- customer is charged (MenuItem.getEffectivePrice). Two code paths quietly set
-- the second to the first plus 10%: MenuService, for anything created in the
-- vendor app or the admin panel, and the Restos importer when a partner sent no
-- channel price. Both are now fixed, but the rows they wrote still carry the
-- markup and will keep charging it until something rewrites them.
--
-- On the day this was written: L'Amoura 7 items and Mini food 8 items, every
-- one at exactly price x 1.1. Qahvoon and Jangirov's were imported after the
-- importer was fixed and are already clean.
--
-- Deliberately narrow. It only touches rows whose charged price is EXACTLY the
-- invented markup, so a venue that genuinely sets a different channel price
-- later is untouched. A blanket "set price_with_margin = price" would also
-- erase a real one.
UPDATE menu_items
SET price_with_margin = price
WHERE price IS NOT NULL
  AND price_with_margin IS NOT NULL
  AND price_with_margin = ROUND(price * 1.10, 2);

-- Variants store a delta from the item's price rather than an absolute, so a
-- markup on the base moved every size with it and there is nothing separate to
-- correct here.

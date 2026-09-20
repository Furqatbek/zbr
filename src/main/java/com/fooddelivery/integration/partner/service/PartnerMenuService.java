package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.integration.partner.dto.PartnerBulkResult;
import com.fooddelivery.integration.partner.dto.PartnerItemUpdate;
import com.fooddelivery.restaurant.entity.ItemVariant;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Price and availability changes pushed by a partner.
 *
 * <p>This is the endpoint that stops an imported menu going stale between
 * syncs: without it a price moved at the till is wrong in the app until the
 * next nightly import, and a dish that sold out at lunch is still orderable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerMenuService {

    private final MenuItemRepository menuItemRepository;

    /**
     * Apply one change.
     *
     * @param externalSource the partner's code. Menu items imported from a
     *        partner are stamped with the same value, so a partner can only
     *        ever reach items that came from them — an item the restaurant
     *        typed in by hand is not theirs to reprice.
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    public MenuItem applyUpdate(Restaurant restaurant, String externalSource, PartnerItemUpdate update) {
        MenuItem item = findItem(restaurant, externalSource, update.getExternalItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No item in this venue has id: " + update.getExternalItemId()));

        if (update.getExternalVariantId() != null && !update.getExternalVariantId().isBlank()) {
            ItemVariant variant = findVariant(item, externalSource, update.getExternalVariantId());
            if (!applyToVariant(item, variant, update)) {
                throw new ResourceNotFoundException("Item " + update.getExternalItemId()
                        + " in this venue has no size with id: " + update.getExternalVariantId());
            }
        } else {
            apply(item, update);
        }
        return menuItemRepository.save(item);
    }

    /**
     * Apply many changes, reporting rather than rolling back what did not work.
     *
     * <p>A markup change across four hundred dishes must not be lost because
     * three of them were deleted here last week. The caller gets those three
     * back by id and can reconcile them; the other prices are already live.
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    public PartnerBulkResult applyUpdates(Restaurant restaurant, String externalSource,
                                          List<PartnerItemUpdate> updates) {
        PartnerBulkResult result = PartnerBulkResult.builder().build();

        // Product changes before size changes, always. A size's price is stored
        // as a difference from the product's, so a size computed against the
        // old base and then re-based by a product update in the same call would
        // land at the wrong number. Sorting here costs nothing and removes the
        // ordering as something a caller has to know about.
        List<PartnerItemUpdate> ordered = updates.stream()
                .sorted(java.util.Comparator.comparing(u ->
                        u.getExternalVariantId() != null && !u.getExternalVariantId().isBlank()))
                .toList();

        for (PartnerItemUpdate update : ordered) {
            if (update.isEmpty()) {
                result.getRejected().add(update.getExternalItemId() + ": nothing to change");
                continue;
            }

            Optional<MenuItem> found = findItem(restaurant, externalSource, update.getExternalItemId());
            if (found.isEmpty()) {
                result.getUnknownItemIds().add(update.getExternalItemId());
                continue;
            }

            MenuItem item = found.get();
            if (update.getExternalVariantId() != null && !update.getExternalVariantId().isBlank()) {
                ItemVariant variant = findVariant(item, externalSource, update.getExternalVariantId());
                if (!applyToVariant(item, variant, update)) {
                    // Reported with both ids: "4417" alone would send them
                    // looking at a product that is perfectly present.
                    result.getUnknownItemIds().add(
                            update.getExternalItemId() + "/" + update.getExternalVariantId());
                    continue;
                }
            } else {
                apply(item, update);
            }
            menuItemRepository.save(item);
            result.setUpdated(result.getUpdated() + 1);
        }

        log.info("Partner bulk menu update on restaurant {}: {} updated, {} unknown, {} rejected",
                restaurant.getId(), result.getUpdated(),
                result.getUnknownItemIds().size(), result.getRejected().size());
        return result;
    }

    /**
     * Apply a change to one size rather than the whole product.
     *
     * <p>Their price is absolute; ours is stored as a difference from the
     * product's, so it is converted here. Getting that backwards would add the
     * size's full price to the product's and charge a customer twice over.
     *
     * @return false when the size is not one we hold, so the caller can report
     *         it rather than silently doing nothing
     */
    private boolean applyToVariant(MenuItem item, ItemVariant variant, PartnerItemUpdate update) {
        if (variant == null) {
            return false;
        }
        if (update.getPrice() != null) {
            variant.setPriceDelta(update.getPrice().subtract(item.getEffectivePrice()));
        }
        if (update.getAvailable() != null) {
            variant.setInStock(update.getAvailable());
        }
        return true;
    }

    private ItemVariant findVariant(MenuItem item, String externalSource, String externalVariantId) {
        Long id = parseId(externalVariantId);
        if (id == null || item.getVariants() == null) {
            return null;
        }
        return item.getVariants().stream()
                .filter(v -> externalSource.equals(v.getExternalSource()) && id.equals(v.getExternalId()))
                .findFirst()
                .orElse(null);
    }

    private void apply(MenuItem item, PartnerItemUpdate update) {
        if (update.getPrice() != null) {
            item.setPrice(update.getPrice());
            // Both fields, and to the SAME number. priceWithMargin is what the
            // customer is actually charged (see MenuItem.getEffectivePrice), so
            // setting only price would move the stored cost while leaving the
            // sale price stale — the partner would see their change accepted
            // and the old price still on the menu.
            //
            // Equal rather than marked up because that is the pricing agreement:
            // a partner's published price is charged verbatim.
            item.setPriceWithMargin(update.getPrice());
        }
        if (update.getAvailable() != null) {
            // Availability, not delisting. Taking an item off the menu for good
            // is done by dropping it from the menu snapshot, which the sync then
            // deactivates — conflating the two here would let a lunchtime
            // sell-out permanently retire a dish.
            item.setInStock(update.getAvailable());
        }
    }

    private Optional<MenuItem> findItem(Restaurant restaurant, String externalSource, String externalItemId) {
        Long id = parseId(externalItemId);
        if (id == null) {
            return Optional.empty();
        }
        return menuItemRepository.findByRestaurantAndExternalId(restaurant.getId(), externalSource, id);
    }

    /**
     * Partner ids arrive as strings so the API does not assume every partner
     * numbers their products, but ours are stored as a number. An id we cannot
     * parse is reported as unknown rather than as a validation error: from the
     * partner's side both mean the same thing, and 400-ing a whole bulk request
     * over one odd id would lose the other 399 updates.
     */
    private Long parseId(String externalItemId) {
        try {
            return Long.parseLong(externalItemId.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}

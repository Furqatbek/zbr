package com.fooddelivery.integration.restos.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.integration.restos.client.RestosMenuClient;
import com.fooddelivery.integration.restos.dto.*;
import com.fooddelivery.restaurant.entity.MenuCategory;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuCategoryRepository;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import com.fooddelivery.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestosMenuImportService {

    private static final String EXTERNAL_SOURCE = "RESTOS";

    /**
     * What the upstream snapshot actually contained, and whether it can be
     * trusted to be the whole menu.
     *
     * <p>The second part carries the weight. Deactivating "everything we did not
     * see" is only sound if not seeing something means it is gone — and a
     * category whose product fetch threw, or a product we could not key, means
     * we did not see items that are perfectly alive. One such failure makes the
     * whole snapshot unusable for deletion, because we cannot tell which of the
     * unseen items were missing on purpose.
     */
    private static final class Snapshot {
        private final Set<Long> categoryIds = new HashSet<>();
        private final Set<Long> productIds = new HashSet<>();
        private boolean complete = true;
        private String incompleteReason;

        void incomplete(String reason) {
            if (complete) {
                complete = false;
                incompleteReason = reason;
            }
        }
    }

    private final RestosMenuClient menuClient;
    private final RestaurantService restaurantService;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final com.fooddelivery.integration.restos.config.RestosProperties restosProperties;
    private final com.fooddelivery.integration.restos.config.UrlSafetyValidator urlSafetyValidator;

    /**
     * Import full menu from Restos using endpoint #3 (full menu).
     * Falls back to endpoint #1 (categories) + #2 (products per category) if #3 fails.
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    public MenuImportResult importFullMenu(Long restaurantId, MenuImportRequest request) {
        Restaurant restaurant = restaurantService.getRestaurantEntityById(restaurantId);

        String baseUrl = normalizeBaseUrl(request.getBaseUrl());
        Long externalRestaurantId = request.getExternalRestaurantId();
        String apiKey = request.getApiKey();
        boolean overwrite = Boolean.TRUE.equals(request.getOverwriteExisting());

        MenuImportResult result = MenuImportResult.builder()
                .restaurantId(restaurantId)
                .externalRestaurantId(externalRestaurantId)
                .syncedAt(LocalDateTime.now())
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        List<RestosCategory> menu;
        try {
            menu = menuClient.fetchFullMenu(baseUrl, externalRestaurantId, apiKey);
        } catch (Exception e) {
            log.warn("Full menu endpoint (#3) failed, falling back to separate fetch: {}", e.getMessage());
            return importMenuSeparately(restaurant, baseUrl, externalRestaurantId, apiKey, overwrite, result);
        }

        if (menu.isEmpty()) {
            result.getWarnings().add("Restos returned empty menu for restaurant " + externalRestaurantId);
            return result;
        }

        Snapshot snapshot = new Snapshot();

        for (RestosCategory extCategory : menu) {
            try {
                MenuCategory category = upsertCategory(restaurant, extCategory, overwrite, result, snapshot);
                if (category == null) {
                    continue;
                }

                if (extCategory.getProducts() == null) {
                    // A category with a null product list is not the same as one
                    // with no products: the full-menu payload should nest them,
                    // so this is missing data, and treating its items as deleted
                    // would empty the category.
                    snapshot.incomplete("category '" + extCategory.getName() + "' returned no product list");
                    continue;
                }

                for (RestosProduct extProduct : extCategory.getProducts()) {
                    try {
                        upsertProduct(category, extProduct, overwrite, result, snapshot);
                    } catch (Exception e) {
                        result.getErrors().add("Failed to import product '" + extProduct.getName() + "': " + e.getMessage());
                        result.setProductsSkipped(result.getProductsSkipped() + 1);
                        snapshot.incomplete("product '" + extProduct.getName() + "' failed to import");
                    }
                }
            } catch (Exception e) {
                result.getErrors().add("Failed to import category '" + extCategory.getName() + "': " + e.getMessage());
                snapshot.incomplete("category '" + extCategory.getName() + "' failed to import");
            }
        }

        retireVanished(restaurant, snapshot, overwrite, result);
        warnAboutUnkeyedItems(restaurant, result);

        restaurant.setExternalSystemUrl(baseUrl);
        restaurant.setLastMenuSyncAt(LocalDateTime.now());

        log.info("Menu import completed for restaurant {}: {} categories created, {} updated, {} deactivated, "
                        + "{} products created, {} updated, {} skipped, {} deactivated",
                restaurantId, result.getCategoriesCreated(), result.getCategoriesUpdated(),
                result.getCategoriesDeactivated(), result.getProductsCreated(), result.getProductsUpdated(),
                result.getProductsSkipped(), result.getProductsDeactivated());

        return result;
    }

    /**
     * Fallback: fetch categories (#1) then products per category (#2) separately.
     */
    private MenuImportResult importMenuSeparately(Restaurant restaurant, String baseUrl,
                                                   Long externalRestaurantId, String apiKey,
                                                   boolean overwrite, MenuImportResult result) {
        List<RestosCategory> categories = menuClient.fetchCategories(baseUrl, externalRestaurantId, apiKey);
        Snapshot snapshot = new Snapshot();

        for (RestosCategory extCategory : categories) {
            try {
                MenuCategory category = upsertCategory(restaurant, extCategory, overwrite, result, snapshot);
                if (category == null) {
                    continue;
                }

                List<RestosProduct> products = menuClient.fetchProductsByCategory(
                        baseUrl, extCategory.getId(), apiKey);

                for (RestosProduct extProduct : products) {
                    try {
                        upsertProduct(category, extProduct, overwrite, result, snapshot);
                    } catch (Exception e) {
                        result.getErrors().add("Failed to import product '" + extProduct.getName() + "': " + e.getMessage());
                        result.setProductsSkipped(result.getProductsSkipped() + 1);
                        snapshot.incomplete("product '" + extProduct.getName() + "' failed to import");
                    }
                }
            } catch (Exception e) {
                // This path is more failure-prone than the nested one: it makes
                // one HTTP call per category, so a single flaky response hides a
                // whole category's products. All the more reason a failure here
                // must stop the deactivation pass.
                result.getErrors().add("Failed to import category '" + extCategory.getName() + "': " + e.getMessage());
                snapshot.incomplete("category '" + extCategory.getName() + "' failed to import");
            }
        }

        retireVanished(restaurant, snapshot, overwrite, result);
        warnAboutUnkeyedItems(restaurant, result);

        restaurant.setExternalSystemUrl(baseUrl);
        restaurant.setLastMenuSyncAt(LocalDateTime.now());
        return result;
    }

    /**
     * Deactivate what the upstream menu no longer has.
     *
     * <p>Soft, never a delete: past orders reference these rows, and
     * {@code active = false} is what already removes an item from every public
     * menu query. An item that comes back upstream is reactivated by the normal
     * upsert.
     */
    /**
     * Report items left stranded by the unkeyed-product collision this import
     * used to have. They cannot be matched upstream any more, so no sync will
     * ever update or retire them; saying so is all we can do from here.
     */
    private void warnAboutUnkeyedItems(Restaurant restaurant, MenuImportResult result) {
        long stranded = menuItemRepository.countUnkeyedExternalItems(restaurant.getId(), EXTERNAL_SOURCE);
        if (stranded > 0) {
            result.getWarnings().add(stranded + " item(s) are marked as coming from Restos but carry no "
                    + "Restos id, so they cannot be synced. They were created before unkeyed products "
                    + "were refused, and may be merged copies of several dishes. Check them by hand.");
        }
    }

    private void retireVanished(Restaurant restaurant, Snapshot snapshot,
                                boolean overwrite, MenuImportResult result) {
        // An import (overwriteExisting = false) deliberately leaves existing
        // products untouched, so it has no business retiring them either.
        // Deactivation belongs to a sync.
        if (!overwrite) {
            return;
        }

        if (!snapshot.complete) {
            result.getWarnings().add("Nothing was deactivated: the snapshot was incomplete ("
                    + snapshot.incompleteReason + "). Items missing from a partial snapshot are not "
                    + "necessarily deleted upstream.");
            return;
        }

        retireVanishedProducts(restaurant, snapshot, result);
        retireVanishedCategories(restaurant, snapshot, result);
    }

    private void retireVanishedProducts(Restaurant restaurant, Snapshot snapshot, MenuImportResult result) {
        List<MenuItem> live = menuItemRepository.findActiveExternalItems(restaurant.getId(), EXTERNAL_SOURCE);
        List<MenuItem> vanished = live.stream()
                .filter(item -> !snapshot.productIds.contains(item.getExternalId()))
                .toList();

        if (vanished.isEmpty()) {
            return;
        }
        if (exceedsDeactivationLimit(vanished.size(), live.size())) {
            String message = String.format(
                    "Refused to deactivate %d of %d Restos products (over the %.0f%% limit). "
                            + "This usually means Restos returned a partial menu rather than that the "
                            + "dishes were removed. Nothing was changed.",
                    vanished.size(), live.size(), restosProperties.getMaxDeactivationRatio() * 100);
            result.getWarnings().add(message);
            log.warn("Restaurant {}: {}", restaurant.getId(), message);
            return;
        }

        for (MenuItem item : vanished) {
            item.setActive(false);
            // Also out of stock: active=false hides it from the menu, but an
            // order already in a basket, or any path that reaches the item
            // directly, should see it as unavailable rather than buyable.
            item.setInStock(false);
            menuItemRepository.save(item);
            log.info("Deactivated menu item {} ('{}') — no longer in the Restos menu",
                    item.getId(), item.getName());
        }
        result.setProductsDeactivated(vanished.size());
    }

    private void retireVanishedCategories(Restaurant restaurant, Snapshot snapshot, MenuImportResult result) {
        List<MenuCategory> live = categoryRepository
                .findByRestaurantIdAndExternalSource(restaurant.getId(), EXTERNAL_SOURCE).stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()) && c.getExternalId() != null)
                .toList();

        List<MenuCategory> vanished = live.stream()
                .filter(category -> !snapshot.categoryIds.contains(category.getExternalId()))
                .toList();

        if (vanished.isEmpty()) {
            return;
        }
        if (exceedsDeactivationLimit(vanished.size(), live.size())) {
            String message = String.format(
                    "Refused to deactivate %d of %d Restos categories (over the %.0f%% limit). "
                            + "Nothing was changed.",
                    vanished.size(), live.size(), restosProperties.getMaxDeactivationRatio() * 100);
            result.getWarnings().add(message);
            log.warn("Restaurant {}: {}", restaurant.getId(), message);
            return;
        }

        int deactivated = 0;
        for (MenuCategory category : vanished) {
            // An inactive category hides everything inside it, so a category the
            // restaurant also filled with their own dishes must stay. Products
            // were retired just above, so anything still active here was never
            // Restos's to remove.
            if (hasItemsWeDoNotOwn(category)) {
                result.getWarnings().add("Category '" + category.getName() + "' is gone from Restos but "
                        + "still holds items added here, so it was left active.");
                continue;
            }
            category.setActive(false);
            categoryRepository.save(category);
            deactivated++;
            log.info("Deactivated menu category {} ('{}') — no longer in the Restos menu",
                    category.getId(), category.getName());
        }
        result.setCategoriesDeactivated(deactivated);
    }

    private boolean hasItemsWeDoNotOwn(MenuCategory category) {
        return menuItemRepository.findByCategoryIdAndActiveOrderBySortOrderAsc(category.getId(), true).stream()
                .anyMatch(item -> item.getExternalId() == null
                        || !EXTERNAL_SOURCE.equals(item.getExternalSource()));
    }

    /**
     * Whether retiring this many of that many looks like an upstream outage
     * rather than a menu change. Below the configured floor it never does — a
     * small menu dropping a dish must stay possible.
     */
    private boolean exceedsDeactivationLimit(int vanished, int live) {
        if (vanished <= restosProperties.getDeactivationFloor()) {
            return false;
        }
        return vanished > live * restosProperties.getMaxDeactivationRatio();
    }

    /**
     * Fetch categories with kitchen station info using endpoint #5.
     */
    public List<RestosCategory> fetchCategoriesWithKitchenInfo(String baseUrl, Long externalRestaurantId, String apiKey) {
        return menuClient.fetchCategoriesWithKitchenInfo(normalizeBaseUrl(baseUrl), externalRestaurantId, apiKey);
    }

    /**
     * Fetch all products using endpoint #6 (POS view with full category info).
     */
    public List<RestosProduct> fetchAllProducts(String baseUrl, Long externalRestaurantId, String apiKey) {
        return menuClient.fetchAllProducts(normalizeBaseUrl(baseUrl), externalRestaurantId, apiKey);
    }

    /**
     * Fetch cached menu using endpoint #4.
     */
    public List<RestosCategory> fetchCachedMenu(String baseUrl, Long externalRestaurantId, String apiKey) {
        return menuClient.fetchCachedMenu(normalizeBaseUrl(baseUrl), externalRestaurantId, apiKey);
    }

    private MenuCategory upsertCategory(Restaurant restaurant, RestosCategory ext,
                                         boolean overwrite, MenuImportResult result, Snapshot snapshot) {
        if (ext.getId() == null) {
            // Not importable, and importing it anyway is actively destructive.
            // A null id is not a miss in the lookup below — Spring Data turns it
            // into "external_id IS NULL", which matches the FIRST unkeyed row in
            // this restaurant. So every category arriving without an id landed
            // on that one row and overwrote it, silently collapsing distinct
            // categories into one that changed identity on every sync.
            //
            // Nothing can fix that here: without a stable key there is no way to
            // tell two unkeyed categories apart, or to recognise either of them
            // next time. Refusing the row is the only honest option.
            result.getWarnings().add("Skipped category '" + ext.getName()
                    + "' — Restos sent no id for it, so it cannot be kept in step with your system.");
            snapshot.incomplete("category '" + ext.getName() + "' has no external id");
            return null;
        }
        snapshot.categoryIds.add(ext.getId());

        Optional<MenuCategory> existingOpt = categoryRepository
                .findByRestaurantIdAndExternalSourceAndExternalId(restaurant.getId(), EXTERNAL_SOURCE, ext.getId());

        if (existingOpt.isPresent()) {
            MenuCategory existing = existingOpt.get();
            result.setCategoriesUpdated(result.getCategoriesUpdated() + 1);

            if (overwrite) {
                existing.setName(ext.getName());
                existing.setDescription(ext.getDescription());
                if (ext.getImageUrl() != null) existing.setImageUrl(ext.getImageUrl());
                if (ext.getSortOrder() != null) existing.setSortOrder(ext.getSortOrder());
                existing.setActive(ext.getActive() != null ? ext.getActive() : true);
            }
            return categoryRepository.save(existing);
        }

        result.setCategoriesCreated(result.getCategoriesCreated() + 1);

        MenuCategory category = MenuCategory.builder()
                .restaurant(restaurant)
                .name(ext.getName())
                .description(ext.getDescription())
                .imageUrl(ext.getImageUrl())
                .sortOrder(ext.getSortOrder() != null ? ext.getSortOrder() : 0)
                .active(ext.getActive() != null ? ext.getActive() : true)
                .externalId(ext.getId())
                .externalSource(EXTERNAL_SOURCE)
                .build();

        return categoryRepository.save(category);
    }

    private void upsertProduct(MenuCategory category, RestosProduct ext,
                                boolean overwrite, MenuImportResult result, Snapshot snapshot) {
        if (ext.getId() == null) {
            // Same collision as in upsertCategory, one level down: the lookup
            // becomes "external_id IS NULL" and matches the first unkeyed item
            // in this category, so every product without an id overwrote the
            // same row. Two different dishes became one, and which one it was
            // depended on the order Restos happened to send them in.
            result.getWarnings().add("Skipped product '" + ext.getName()
                    + "' — Restos sent no id for it, so it cannot be kept in step with your system.");
            result.setProductsSkipped(result.getProductsSkipped() + 1);
            snapshot.incomplete("product '" + ext.getName() + "' has no external id");
            return;
        }

        if ("ARCHIVED".equalsIgnoreCase(ext.getStatus())) {
            // Deliberately NOT recorded as seen. Archived upstream means retired,
            // so letting it fall through to the deactivation pass is the point —
            // previously it was skipped here and stayed live with us forever.
            result.getWarnings().add("Skipped archived product '" + ext.getName() + "'");
            result.setProductsSkipped(result.getProductsSkipped() + 1);
            return;
        }

        // Recorded as seen from here on, including the no-price case below:
        // the dish is still on their menu, it just arrived with a field missing.
        // Retiring a live dish over a blank price field would be a data glitch
        // taking food off sale.
        snapshot.productIds.add(ext.getId());

        if (ext.getPrice() == null) {
            result.getWarnings().add("Skipped product '" + ext.getName() + "' — no price");
            result.setProductsSkipped(result.getProductsSkipped() + 1);
            return;
        }

        Optional<MenuItem> existingOpt = menuItemRepository
                .findByCategoryIdAndExternalSourceAndExternalId(category.getId(), EXTERNAL_SOURCE, ext.getId());

        if (existingOpt.isPresent()) {
            MenuItem existing = existingOpt.get();

            // overwriteExisting=false (Import) must NOT touch an existing product —
            // skip it. Only overwriteExisting=true (Sync) updates in place.
            if (!overwrite) {
                result.setProductsSkipped(result.getProductsSkipped() + 1);
                return;
            }

            result.setProductsUpdated(result.getProductsUpdated() + 1);

            existing.setName(ext.getName());
            existing.setPrice(ext.getPrice());
            existing.setPriceWithMargin(ext.getPriceWithMargin() != null
                    ? ext.getPriceWithMargin()
                    : ext.getPrice().multiply(new BigDecimal("1.10")));
            existing.setInStock(ext.isAvailable());
            existing.setFeatured(ext.isFeaturedProduct());
            existing.setActive(true);
            if (ext.getDescription() != null) existing.setDescription(ext.getDescription());
            if (ext.getImageUrl() != null) existing.setImageUrl(ext.getImageUrl());
            if (ext.getSortOrder() != null) existing.setSortOrder(ext.getSortOrder());
            if (ext.getCostPrice() != null) existing.setOriginalPrice(ext.getCostPrice());

            menuItemRepository.save(existing);
            return;
        }

        result.setProductsCreated(result.getProductsCreated() + 1);

        MenuItem item = MenuItem.builder()
                .category(category)
                .name(ext.getName())
                .description(ext.getDescription())
                .price(ext.getPrice())
                .priceWithMargin(ext.getPriceWithMargin() != null
                        ? ext.getPriceWithMargin()
                        : ext.getPrice().multiply(new BigDecimal("1.10")))
                .originalPrice(ext.getCostPrice())
                .imageUrl(ext.getImageUrl())
                .inStock(ext.isAvailable())
                .featured(ext.isFeaturedProduct())
                .sortOrder(ext.getSortOrder() != null ? ext.getSortOrder() : 0)
                .active(true)
                .externalId(ext.getId())
                .externalSource(EXTERNAL_SOURCE)
                .build();

        menuItemRepository.save(item);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!restosProperties.isEnabled()) {
            throw new BusinessException("Restos integration is disabled");
        }
        if (baseUrl == null || baseUrl.isBlank()) throw new BusinessException("Base URL is required");
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        // SSRF guard: reject non-public / internal / metadata targets before any server-side fetch.
        urlSafetyValidator.validate(normalized);
        return normalized;
    }
}

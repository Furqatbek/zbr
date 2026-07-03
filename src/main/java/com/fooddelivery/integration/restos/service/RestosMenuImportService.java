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

        for (RestosCategory extCategory : menu) {
            try {
                MenuCategory category = upsertCategory(restaurant, extCategory, overwrite, result);

                if (extCategory.getProducts() != null) {
                    for (RestosProduct extProduct : extCategory.getProducts()) {
                        try {
                            upsertProduct(category, extProduct, overwrite, result);
                        } catch (Exception e) {
                            result.getErrors().add("Failed to import product '" + extProduct.getName() + "': " + e.getMessage());
                            result.setProductsSkipped(result.getProductsSkipped() + 1);
                        }
                    }
                }
            } catch (Exception e) {
                result.getErrors().add("Failed to import category '" + extCategory.getName() + "': " + e.getMessage());
            }
        }

        restaurant.setExternalSystemUrl(baseUrl);
        restaurant.setLastMenuSyncAt(LocalDateTime.now());

        log.info("Menu import completed for restaurant {}: {} categories created, {} updated, {} products created, {} updated, {} skipped",
                restaurantId, result.getCategoriesCreated(), result.getCategoriesUpdated(),
                result.getProductsCreated(), result.getProductsUpdated(), result.getProductsSkipped());

        return result;
    }

    /**
     * Fallback: fetch categories (#1) then products per category (#2) separately.
     */
    private MenuImportResult importMenuSeparately(Restaurant restaurant, String baseUrl,
                                                   Long externalRestaurantId, String apiKey,
                                                   boolean overwrite, MenuImportResult result) {
        List<RestosCategory> categories = menuClient.fetchCategories(baseUrl, externalRestaurantId, apiKey);

        for (RestosCategory extCategory : categories) {
            try {
                MenuCategory category = upsertCategory(restaurant, extCategory, overwrite, result);

                List<RestosProduct> products = menuClient.fetchProductsByCategory(
                        baseUrl, extCategory.getId(), apiKey);

                for (RestosProduct extProduct : products) {
                    try {
                        upsertProduct(category, extProduct, overwrite, result);
                    } catch (Exception e) {
                        result.getErrors().add("Failed to import product '" + extProduct.getName() + "': " + e.getMessage());
                        result.setProductsSkipped(result.getProductsSkipped() + 1);
                    }
                }
            } catch (Exception e) {
                result.getErrors().add("Failed to import category '" + extCategory.getName() + "': " + e.getMessage());
            }
        }

        restaurant.setExternalSystemUrl(baseUrl);
        restaurant.setLastMenuSyncAt(LocalDateTime.now());
        return result;
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
                                         boolean overwrite, MenuImportResult result) {
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
                                boolean overwrite, MenuImportResult result) {
        if (ext.getPrice() == null) {
            result.getWarnings().add("Skipped product '" + ext.getName() + "' — no price");
            result.setProductsSkipped(result.getProductsSkipped() + 1);
            return;
        }

        if ("ARCHIVED".equalsIgnoreCase(ext.getStatus())) {
            result.getWarnings().add("Skipped archived product '" + ext.getName() + "'");
            result.setProductsSkipped(result.getProductsSkipped() + 1);
            return;
        }

        Optional<MenuItem> existingOpt = menuItemRepository
                .findByCategoryIdAndExternalSourceAndExternalId(category.getId(), EXTERNAL_SOURCE, ext.getId());

        if (existingOpt.isPresent()) {
            MenuItem existing = existingOpt.get();
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

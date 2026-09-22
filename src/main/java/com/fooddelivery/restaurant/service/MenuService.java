package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.service.ImageStorageService;
import com.fooddelivery.common.service.ImageStorageService.ImageInfo;
import com.fooddelivery.restaurant.dto.*;
import com.fooddelivery.restaurant.entity.*;
import com.fooddelivery.restaurant.mapper.RestaurantMapper;
import com.fooddelivery.restaurant.repository.MenuCategoryRepository;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for menu management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final RestaurantService restaurantService;
    private final RestaurantMapper mapper;
    private final ImageStorageService imageStorageService;

    /**
     * A markup added to every price a restaurant types, as a fraction.
     *
     * <p><strong>Zero by default.</strong> It was a hard-coded 10% — a venue
     * entering 35 000 in the vendor app had 38 500 charged to the customer,
     * with the venue's own screen showing the number they typed. The difference
     * was not commission (that is 15%, taken out of the venue's payout, and
     * still is), so the platform was charging the customer a tenth on top AND
     * taking commission underneath, under a constant labelled "could be
     * configurable".
     *
     * <p>It is the third markup of this kind found in two days: the same 10% in
     * the Restos importer, and an 8% "tax" that was a US sales-tax default. All
     * three were inherited rather than chosen, and none was written down
     * anywhere a restaurant could see it.
     *
     * <p>Now configurable and off. Charging is a decision someone makes.
     */
    @org.springframework.beans.factory.annotation.Value("${app.menu.platform-margin-rate:0}")
    private BigDecimal platformMargin;

    /**
     * A rate outside [0, 1) would be discovered on a customer's bill. Checked
     * at startup instead.
     */
    @jakarta.annotation.PostConstruct
    void validatePlatformMargin() {
        if (platformMargin == null
                || platformMargin.compareTo(BigDecimal.ZERO) < 0
                || platformMargin.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalStateException("app.menu.platform-margin-rate must be at least 0 and "
                    + "below 1 (it is a fraction, not a percentage). Got: " + platformMargin);
        }
        if (platformMargin.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Menu prices carry a platform margin of {}%",
                    platformMargin.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString());
        }
    }

    // ============== Category Operations ==============

    /**
     * Create a menu category.
     */
    @Transactional
    @CacheEvict(value = "menus", key = "#restaurantId")
    @Auditable(action = "CREATE_CATEGORY", entityType = "MenuCategory")
    public MenuCategoryDto createCategory(Long restaurantId, CreateMenuCategoryRequest request) {
        log.info("Creating category: {} for restaurant: {}", request.getName(), restaurantId);

        Restaurant restaurant = restaurantService.getRestaurantEntityById(restaurantId);

        // Check for duplicate name
        if (categoryRepository.existsByRestaurantIdAndName(restaurantId, request.getName())) {
            throw new DuplicateResourceException("MenuCategory", "name", request.getName());
        }

        MenuCategory category = mapper.toEntity(request);
        category.setRestaurant(restaurant);

        // Set sort order if not provided
        if (category.getSortOrder() == null) {
            Integer maxOrder = categoryRepository.findMaxSortOrderByRestaurantId(restaurantId);
            category.setSortOrder(maxOrder != null ? maxOrder + 1 : 0);
        }

        category = categoryRepository.save(category);
        log.info("Category created with id: {}", category.getId());

        return mapper.toCategoryDto(category);
    }

    /**
     * Get all categories for a restaurant.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "menus", key = "#restaurantId")
    public List<MenuCategoryDto> getCategoriesByRestaurant(Long restaurantId) {
        List<MenuCategory> categories = categoryRepository.findByRestaurantIdAndActiveOrderBySortOrderAsc(
                restaurantId, true);
        return mapper.toCategoryDtoList(categories);
    }

    /**
     * Get full menu with items for a restaurant.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "menus", key = "'full:' + #restaurantId")
    public List<MenuCategoryDto> getFullMenu(Long restaurantId) {
        List<MenuCategory> categories = categoryRepository.findActiveMenuWithItems(restaurantId);
        return categories.stream()
                .map(cat -> {
                    MenuCategoryDto dto = mapper.toCategoryDto(cat);
                    dto.setItems(mapper.toItemDtoList(cat.getItems()));
                    return dto;
                })
                .toList();
    }

    /**
     * Update a category.
     */
    @Transactional
    @CacheEvict(value = "menus", allEntries = true)
    @Auditable(action = "UPDATE_CATEGORY", entityType = "MenuCategory")
    public MenuCategoryDto updateCategory(Long restaurantId, Long categoryId, CreateMenuCategoryRequest request) {
        MenuCategory category = getCategoryForRestaurant(restaurantId, categoryId);

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        category = categoryRepository.save(category);
        return mapper.toCategoryDto(category);
    }

    /**
     * Delete a category.
     */
    @Transactional
    @CacheEvict(value = "menus", allEntries = true)
    @Auditable(action = "DELETE_CATEGORY", entityType = "MenuCategory")
    public void deleteCategory(Long restaurantId, Long categoryId) {
        MenuCategory category = getCategoryForRestaurant(restaurantId, categoryId);

        category.setActive(false);
        categoryRepository.save(category);
        log.info("Category deleted (soft): {}", categoryId);
    }

    // ============== Item Operations ==============

    /**
     * Create a menu item.
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "CREATE_ITEM", entityType = "MenuItem")
    public MenuItemDto createItem(Long restaurantId, CreateMenuItemRequest request) {
        log.info("Creating item: {} for restaurant: {}", request.getName(), restaurantId);

        MenuCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", "id", request.getCategoryId()));

        // Verify category belongs to restaurant
        if (!category.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("MenuCategory", "id", request.getCategoryId());
        }

        MenuItem item = mapper.toEntity(request);
        item.setCategory(category);

        // Calculate price with platform margin
        item.setPriceWithMargin(calculatePriceWithMargin(request.getPrice()));

        // Set sort order if not provided
        if (item.getSortOrder() == null) {
            Integer maxOrder = itemRepository.findMaxSortOrderByCategoryId(request.getCategoryId());
            item.setSortOrder(maxOrder != null ? maxOrder + 1 : 0);
        }

        item = itemRepository.save(item);

        // Add variants
        if (request.getVariants() != null) {
            for (CreateItemVariantRequest variantReq : request.getVariants()) {
                ItemVariant variant = mapper.toEntity(variantReq);
                variant.setMenuItem(item);
                item.getVariants().add(variant);
            }
        }

        // Add options
        if (request.getOptions() != null) {
            for (CreateItemOptionRequest optionReq : request.getOptions()) {
                ItemOption option = mapper.toEntity(optionReq);
                option.setMenuItem(item);
                item.getOptions().add(option);
            }
        }

        item = itemRepository.save(item);
        log.info("Menu item created with id: {}", item.getId());

        return mapper.toItemDto(item);
    }

    /**
     * Get menu item by ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "menuItems", key = "#itemId")
    public MenuItemDto getItemById(Long itemId) {
        MenuItem item = itemRepository.findByIdWithVariantsAndOptions(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));

        MenuItemDto dto = mapper.toItemDto(item);
        dto.setVariants(mapper.toVariantDtoList(item.getVariants()));
        dto.setOptions(mapper.toOptionDtoList(item.getOptions()));

        return dto;
    }

    /**
     * Get items by restaurant with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<MenuItemDto> getItemsByRestaurant(Long restaurantId, Pageable pageable) {
        Page<MenuItem> items = itemRepository.findActiveByRestaurantId(restaurantId, pageable);
        return PagedResponse.from(items, mapper.toItemDtoList(items.getContent()));
    }

    /**
     * Get featured items for a restaurant.
     */
    @Transactional(readOnly = true)
    public List<MenuItemDto> getFeaturedItems(Long restaurantId) {
        List<MenuItem> items = itemRepository.findFeaturedByRestaurantId(restaurantId);
        return mapper.toItemDtoList(items);
    }

    /**
     * Search items in a restaurant.
     */
    @Transactional(readOnly = true)
    public PagedResponse<MenuItemDto> searchItems(Long restaurantId, String query, Pageable pageable) {
        Page<MenuItem> items = itemRepository.searchByRestaurantId(restaurantId, query, pageable);
        return PagedResponse.from(items, mapper.toItemDtoList(items.getContent()));
    }

    /**
     * Update a menu item.
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "UPDATE_ITEM", entityType = "MenuItem")
    public MenuItemDto updateItem(Long restaurantId, Long itemId, CreateMenuItemRequest request) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);

        if (request.getName() != null) {
            item.setName(request.getName());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
            item.setPriceWithMargin(calculatePriceWithMargin(request.getPrice()));
        }
        if (request.getOriginalPrice() != null) {
            item.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl());
        }
        if (request.getPrepTimeMinutes() != null) {
            item.setPrepTimeMinutes(request.getPrepTimeMinutes());
        }
        if (request.getCalories() != null) {
            item.setCalories(request.getCalories());
        }
        if (request.getVegetarian() != null) {
            item.setVegetarian(request.getVegetarian());
        }
        if (request.getVegan() != null) {
            item.setVegan(request.getVegan());
        }
        if (request.getGlutenFree() != null) {
            item.setGlutenFree(request.getGlutenFree());
        }
        if (request.getSpicy() != null) {
            item.setSpicy(request.getSpicy());
        }
        if (request.getAllergens() != null) {
            item.setAllergens(request.getAllergens());
        }
        if (request.getFeatured() != null) {
            item.setFeatured(request.getFeatured());
        }
        if (request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
        }

        item = itemRepository.save(item);
        return mapper.toItemDto(item);
    }

    /**
     * Update item stock status.
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    public MenuItemDto updateItemStock(Long restaurantId, Long itemId, Boolean inStock) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);

        item.setInStock(inStock);
        item = itemRepository.save(item);
        log.info("Item {} stock updated to: {}", itemId, inStock);

        return mapper.toItemDto(item);
    }

    /**
     * Delete a menu item (soft delete).
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "DELETE_ITEM", entityType = "MenuItem")
    public void deleteItem(Long restaurantId, Long itemId) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);

        item.setActive(false);
        itemRepository.save(item);
        log.info("Menu item deleted (soft): {}", itemId);
    }

    /**
     * Upload and update menu item image.
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "UPDATE_ITEM_IMAGE", entityType = "MenuItem")
    public MenuItemDto updateItemImage(Long restaurantId, Long itemId, MultipartFile imageFile) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);

        // Delete old image if exists
        if (item.getImagePath() != null) {
            String relativePath = extractRelativePath(item.getImagePath());
            if (relativePath != null) {
                imageStorageService.deleteImage(relativePath);
            }
        }

        // Store new image
        ImageInfo imageInfo = imageStorageService.storeImage(imageFile, "menu-items");

        // Update item with image info
        item.setImageUrl(imageInfo.getUrl());
        item.setImagePath(imageInfo.getPath());
        item.setImageName(imageInfo.getOriginalName());
        item.setImageSize(imageInfo.getSize());
        item.setImageContentType(imageInfo.getContentType());

        item = itemRepository.save(item);
        log.info("Menu item {} image updated: {}", itemId, imageInfo.getUrl());

        return mapper.toItemDto(item);
    }

    /**
     * Delete menu item image.
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "DELETE_ITEM_IMAGE", entityType = "MenuItem")
    public MenuItemDto deleteItemImage(Long restaurantId, Long itemId) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);

        if (item.getImagePath() != null) {
            String relativePath = extractRelativePath(item.getImagePath());
            if (relativePath != null) {
                imageStorageService.deleteImage(relativePath);
            }
        }

        item.setImageUrl(null);
        item.setImagePath(null);
        item.setImageName(null);
        item.setImageSize(null);
        item.setImageContentType(null);

        item = itemRepository.save(item);
        log.info("Menu item {} image deleted", itemId);

        return mapper.toItemDto(item);
    }

    /**
     * What the customer is charged for a price the restaurant typed.
     *
     * <p>With the margin at zero — the default — this is the price itself, to
     * the cent. That is the same rule the Partner API already states for a
     * partner's published price and the Restos import now follows: the number
     * the venue set is the number charged.
     */
    private BigDecimal calculatePriceWithMargin(BigDecimal basePrice) {
        if (basePrice == null) {
            return null;
        }
        if (platformMargin.compareTo(BigDecimal.ZERO) == 0) {
            return basePrice;
        }
        return basePrice.add(basePrice.multiply(platformMargin))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String extractRelativePath(String fullPath) {
        if (fullPath == null) return null;
        int menuItemsIndex = fullPath.indexOf("menu-items");
        if (menuItemsIndex >= 0) {
            return fullPath.substring(menuItemsIndex);
        }
        return null;
    }

    /**
     * Load a menu item and prove it belongs to the restaurant in the URL.
     *
     * <p>The controller validates that the caller owns {restaurantId}, but the
     * mutation targets {itemId} — two different things. Without this, an owner
     * could pass their OWN restaurant id with ANOTHER restaurant's item id and
     * rewrite its price, delete it, or mark it out of stock. Menu item ids are
     * sequential and returned in public menu responses, so they take no effort
     * to find. createItem already made this check for categories; these are its
     * missing siblings.
     *
     * <p>Reports 404, not 403: a caller must not learn that someone else's item
     * exists.
     */
    private MenuItem getItemForRestaurant(Long restaurantId, Long itemId) {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));
        Long owner = item.getCategory().getRestaurant().getId();
        if (!owner.equals(restaurantId)) {
            log.warn("SECURITY: restaurant {} attempted to modify menu item {} owned by restaurant {}",
                    restaurantId, itemId, owner);
            throw new ResourceNotFoundException("MenuItem", "id", itemId);
        }
        return item;
    }

    private MenuCategory getCategoryForRestaurant(Long restaurantId, Long categoryId) {
        MenuCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", "id", categoryId));
        Long owner = category.getRestaurant().getId();
        if (!owner.equals(restaurantId)) {
            log.warn("SECURITY: restaurant {} attempted to modify menu category {} owned by restaurant {}",
                    restaurantId, categoryId, owner);
            throw new ResourceNotFoundException("MenuCategory", "id", categoryId);
        }
        return category;
    }
}

package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.service.ImageStorageService;
import com.fooddelivery.common.service.ImageStorageService.ImageInfo;
import com.fooddelivery.restaurant.dto.*;
import com.fooddelivery.restaurant.entity.ItemOption;
import com.fooddelivery.restaurant.entity.ItemVariant;
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

        item.setPriceWithMargin(chargedPriceFor(request.getPrice()));

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
            item.setPriceWithMargin(chargedPriceFor(request.getPrice()));
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

    // ============== Sizes and add-ons ==============
    //
    // These exist because there was no way to add either to a dish that already
    // existed. They could only be supplied nested inside the item at creation,
    // and PUT /items/{id} accepted "variants" and "options" in the body and
    // silently dropped them — so a restaurant wanting one add-on had to delete
    // the dish and build it again, losing its id, its image and its history.
    //
    // Managed through the MenuItem aggregate rather than their own
    // repositories: the collections cascade and orphan-remove, so adding to the
    // set and saving the item is the whole operation.

    /**
     * Add a size to a dish.
     *
     * <p>DELETE really deletes. An order line snapshots the variant's name and
     * its price at the time, and order_items.variant_id carries no foreign key,
     * so removing a size cannot damage history. {@code active: false} is the
     * switch for hiding one you may want back; {@code inStock: false} is for
     * today's lunch service.
     */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "ADD_ITEM_VARIANT", entityType = "MenuItem")
    public ItemVariantDto addVariant(Long restaurantId, Long itemId, SaveItemVariantRequest request) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);

        ItemVariant variant = ItemVariant.builder()
                .menuItem(item)
                .name(request.getName())
                .priceDelta(request.getPriceDelta() != null ? request.getPriceDelta() : BigDecimal.ZERO)
                .inStock(request.getInStock() == null || request.getInStock())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextVariantSortOrder(item))
                .active(request.getActive() == null || request.getActive())
                .build();

        item.getVariants().add(variant);
        itemRepository.save(item);

        log.info("Variant '{}' added to item {}", variant.getName(), itemId);
        return mapper.toVariantDto(variant);
    }

    /** Partial: a field left out is left alone. */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "UPDATE_ITEM_VARIANT", entityType = "MenuItem")
    public ItemVariantDto updateVariant(Long restaurantId, Long itemId, Long variantId,
                                        SaveItemVariantRequest request) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);
        ItemVariant variant = findVariant(item, variantId);

        if (request.getName() != null) variant.setName(request.getName());
        if (request.getPriceDelta() != null) variant.setPriceDelta(request.getPriceDelta());
        if (request.getInStock() != null) variant.setInStock(request.getInStock());
        if (request.getSortOrder() != null) variant.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) variant.setActive(request.getActive());

        itemRepository.save(item);
        return mapper.toVariantDto(variant);
    }

    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "DELETE_ITEM_VARIANT", entityType = "MenuItem")
    public void deleteVariant(Long restaurantId, Long itemId, Long variantId) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);
        ItemVariant variant = findVariant(item, variantId);

        item.getVariants().remove(variant);
        itemRepository.save(item);
        log.info("Variant {} removed from item {}", variantId, itemId);
    }

    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "ADD_ITEM_OPTION", entityType = "MenuItem")
    public ItemOptionDto addOption(Long restaurantId, Long itemId, SaveItemOptionRequest request) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);

        ItemOption option = ItemOption.builder()
                .menuItem(item)
                .groupName(request.getGroupName())
                .name(request.getName())
                .priceDelta(request.getPriceDelta() != null ? request.getPriceDelta() : BigDecimal.ZERO)
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .maxSelections(request.getMaxSelections() != null ? request.getMaxSelections() : 1)
                .required(Boolean.TRUE.equals(request.getRequired()))
                .inStock(request.getInStock() == null || request.getInStock())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextOptionSortOrder(item))
                .active(request.getActive() == null || request.getActive())
                .build();

        item.getOptions().add(option);
        itemRepository.save(item);

        log.info("Option '{}' ({}) added to item {}", option.getName(), option.getGroupName(), itemId);
        return mapper.toOptionDto(option);
    }

    /** Partial: a field left out is left alone. */
    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "UPDATE_ITEM_OPTION", entityType = "MenuItem")
    public ItemOptionDto updateOption(Long restaurantId, Long itemId, Long optionId,
                                      SaveItemOptionRequest request) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);
        ItemOption option = findOption(item, optionId);

        if (request.getGroupName() != null) option.setGroupName(request.getGroupName());
        if (request.getName() != null) option.setName(request.getName());
        if (request.getPriceDelta() != null) option.setPriceDelta(request.getPriceDelta());
        if (request.getIsDefault() != null) option.setIsDefault(request.getIsDefault());
        if (request.getMaxSelections() != null) option.setMaxSelections(request.getMaxSelections());
        if (request.getRequired() != null) option.setRequired(request.getRequired());
        if (request.getInStock() != null) option.setInStock(request.getInStock());
        if (request.getSortOrder() != null) option.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) option.setActive(request.getActive());

        itemRepository.save(item);
        return mapper.toOptionDto(option);
    }

    @Transactional
    @CacheEvict(value = {"menus", "menuItems"}, allEntries = true)
    @Auditable(action = "DELETE_ITEM_OPTION", entityType = "MenuItem")
    public void deleteOption(Long restaurantId, Long itemId, Long optionId) {
        MenuItem item = getItemForRestaurant(restaurantId, itemId);
        ItemOption option = findOption(item, optionId);

        item.getOptions().remove(option);
        itemRepository.save(item);
        log.info("Option {} removed from item {}", optionId, itemId);
    }

    /**
     * A variant of THIS item, or a 404.
     *
     * <p>Answering "not found" rather than "not yours" for a variant belonging
     * to another dish is the same rule the item lookup uses: an id that is not
     * yours does not exist as far as you are concerned.
     */
    private ItemVariant findVariant(MenuItem item, Long variantId) {
        return item.getVariants().stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ItemVariant", "id", variantId));
    }

    private ItemOption findOption(MenuItem item, Long optionId) {
        return item.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ItemOption", "id", optionId));
    }

    private int nextVariantSortOrder(MenuItem item) {
        return item.getVariants().stream()
                .map(ItemVariant::getSortOrder).filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(-1) + 1;
    }

    private int nextOptionSortOrder(MenuItem item) {
        return item.getOptions().stream()
                .map(ItemOption::getSortOrder).filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(-1) + 1;
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
     * What the customer is charged for a price the restaurant set: that price.
     *
     * <p>Nothing is added. There is no rate, no configuration key and no
     * fallback that could put one back — a price changes when an admin or the
     * restaurant changes it, and at no other time.
     *
     * <p>This was a hard-coded 10%, and the field it writes is what
     * {@link com.fooddelivery.restaurant.entity.MenuItem#getEffectivePrice()}
     * returns and what the customer pays. A venue entering 35 000 had 38 500
     * charged while its own screen kept showing 35 000, so the one party able
     * to notice could not see it. Two sibling markups were found the same
     * week: the same 10% in the Restos importer, and an 8% "tax" that was a US
     * sales-tax default. All three were inherited and none was chosen, which is
     * why this is now an assignment rather than an arithmetic.
     *
     * <p>{@code priceWithMargin} keeps its name and its purpose: it is the
     * charged price, and a PARTNER may still set it away from {@code price}
     * when they publish a separate channel price for us. That is the venue's
     * own number, not ours.
     */
    private BigDecimal chargedPriceFor(BigDecimal priceSetByTheRestaurant) {
        return priceSetByTheRestaurant;
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

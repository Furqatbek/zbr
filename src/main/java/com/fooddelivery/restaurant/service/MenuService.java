package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
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

    // Platform margin percentage (could be configurable)
    private static final BigDecimal PLATFORM_MARGIN = new BigDecimal("0.10"); // 10%

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
    public MenuCategoryDto updateCategory(Long categoryId, CreateMenuCategoryRequest request) {
        MenuCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", "id", categoryId));

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
    public void deleteCategory(Long categoryId) {
        MenuCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", "id", categoryId));

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
    public MenuItemDto updateItem(Long itemId, CreateMenuItemRequest request) {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));

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
    public MenuItemDto updateItemStock(Long itemId, Boolean inStock) {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));

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
    public void deleteItem(Long itemId) {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));

        item.setActive(false);
        itemRepository.save(item);
        log.info("Menu item deleted (soft): {}", itemId);
    }

    private BigDecimal calculatePriceWithMargin(BigDecimal basePrice) {
        return basePrice.add(basePrice.multiply(PLATFORM_MARGIN))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}

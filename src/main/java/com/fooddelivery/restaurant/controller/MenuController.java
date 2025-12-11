package com.fooddelivery.restaurant.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.restaurant.dto.*;
import com.fooddelivery.restaurant.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for menu management operations.
 */
@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menu")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Menu", description = "Menu management endpoints")
public class MenuController {

    private final MenuService menuService;

    // ============== Public Endpoints ==============

    @GetMapping
    @Operation(summary = "Get full menu", description = "Get full menu with categories and items for a restaurant")
    public ResponseEntity<ApiResponse<List<MenuCategoryDto>>> getFullMenu(
            @PathVariable Long restaurantId) {

        List<MenuCategoryDto> menu = menuService.getFullMenu(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(menu));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get categories", description = "Get all menu categories for a restaurant")
    public ResponseEntity<ApiResponse<List<MenuCategoryDto>>> getCategories(
            @PathVariable Long restaurantId) {

        List<MenuCategoryDto> categories = menuService.getCategoriesByRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/items")
    @Operation(summary = "Get all items", description = "Get all menu items for a restaurant with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<MenuItemDto>>> getItems(
            @PathVariable Long restaurantId,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<MenuItemDto> items = menuService.getItemsByRestaurant(restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/items/featured")
    @Operation(summary = "Get featured items", description = "Get featured menu items for a restaurant")
    public ResponseEntity<ApiResponse<List<MenuItemDto>>> getFeaturedItems(
            @PathVariable Long restaurantId) {

        List<MenuItemDto> items = menuService.getFeaturedItems(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/items/search")
    @Operation(summary = "Search items", description = "Search menu items in a restaurant")
    public ResponseEntity<ApiResponse<PagedResponse<MenuItemDto>>> searchItems(
            @PathVariable Long restaurantId,
            @Parameter(description = "Search query") @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<MenuItemDto> items = menuService.searchItems(restaurantId, q, pageable);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "Get item details", description = "Get menu item details with variants and options")
    public ResponseEntity<ApiResponse<MenuItemDto>> getItem(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId) {

        MenuItemDto item = menuService.getItemById(itemId);
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    // ============== Protected Endpoints (Restaurant Management) ==============

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create category", description = "Create a new menu category")
    public ResponseEntity<ApiResponse<MenuCategoryDto>> createCategory(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateMenuCategoryRequest request) {

        MenuCategoryDto category = menuService.createCategory(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", category));
    }

    @PutMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update category", description = "Update a menu category")
    public ResponseEntity<ApiResponse<MenuCategoryDto>> updateCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId,
            @Valid @RequestBody CreateMenuCategoryRequest request) {

        MenuCategoryDto category = menuService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", category));
    }

    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete category", description = "Delete a menu category (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId) {

        menuService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create item", description = "Create a new menu item")
    public ResponseEntity<ApiResponse<MenuItemDto>> createItem(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateMenuItemRequest request) {

        MenuItemDto item = menuService.createItem(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created successfully", item));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update item", description = "Update a menu item")
    public ResponseEntity<ApiResponse<MenuItemDto>> updateItem(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @Valid @RequestBody CreateMenuItemRequest request) {

        MenuItemDto item = menuService.updateItem(itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Menu item updated successfully", item));
    }

    @PatchMapping("/items/{itemId}/stock")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update item stock", description = "Update menu item stock status")
    public ResponseEntity<ApiResponse<MenuItemDto>> updateItemStock(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @RequestParam Boolean inStock) {

        MenuItemDto item = menuService.updateItemStock(itemId, inStock);
        return ResponseEntity.ok(ApiResponse.success(
                "Item is now " + (inStock ? "in stock" : "out of stock"), item));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete item", description = "Delete a menu item (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId) {

        menuService.deleteItem(itemId);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted successfully"));
    }

    @PostMapping(value = "/items/{itemId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload item image", description = "Upload or update menu item image")
    public ResponseEntity<ApiResponse<MenuItemDto>> uploadItemImage(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file) {

        log.info("Uploading image for menu item: {} in restaurant: {}", itemId, restaurantId);
        MenuItemDto item = menuService.updateItemImage(itemId, file);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", item));
    }

    @DeleteMapping("/items/{itemId}/image")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete item image", description = "Delete menu item image")
    public ResponseEntity<ApiResponse<MenuItemDto>> deleteItemImage(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId) {

        log.info("Deleting image for menu item: {} in restaurant: {}", itemId, restaurantId);
        MenuItemDto item = menuService.deleteItemImage(itemId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", item));
    }
}

package com.fooddelivery.restaurant.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.restaurant.dto.*;
import com.fooddelivery.restaurant.service.MenuService;
import com.fooddelivery.restaurant.service.RestaurantService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
    private final RestaurantService restaurantService;

    /**
     * Helper method to validate restaurant access.
     */
    private void validateAccess(Long restaurantId, UserPrincipal currentUser) {
        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(restaurantId, currentUser.getId(), isAdminOrPlatform);
    }

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
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateMenuCategoryRequest request) {

        validateAccess(restaurantId, currentUser);
        MenuCategoryDto category = menuService.createCategory(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", category));
    }

    @PutMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update category", description = "Update a menu category")
    public ResponseEntity<ApiResponse<MenuCategoryDto>> updateCategory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId,
            @Valid @RequestBody CreateMenuCategoryRequest request) {

        validateAccess(restaurantId, currentUser);
        MenuCategoryDto category = menuService.updateCategory(restaurantId, categoryId, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", category));
    }

    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete category", description = "Delete a menu category (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId) {

        validateAccess(restaurantId, currentUser);
        menuService.deleteCategory(restaurantId, categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create item", description = "Create a new menu item")
    public ResponseEntity<ApiResponse<MenuItemDto>> createItem(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateMenuItemRequest request) {

        validateAccess(restaurantId, currentUser);
        MenuItemDto item = menuService.createItem(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created successfully", item));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update item", description = "Update a menu item")
    public ResponseEntity<ApiResponse<MenuItemDto>> updateItem(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @Valid @RequestBody CreateMenuItemRequest request) {

        validateAccess(restaurantId, currentUser);

        // Refused rather than ignored. This endpoint took "variants" and
        // "options" in the body and dropped them on the floor, which reads from
        // the outside exactly like a request that worked — the same trap as a
        // query parameter that has not shipped. Sizes and add-ons have their own
        // endpoints below, and saying so is more use than a silent success.
        if (request.getVariants() != null || request.getOptions() != null) {
            throw new BusinessException(
                    "Sizes and add-ons are not changed here. Use "
                            + "POST/PUT/DELETE /api/v1/restaurants/" + restaurantId
                            + "/menu/items/" + itemId + "/variants and /options.");
        }

        MenuItemDto item = menuService.updateItem(restaurantId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Menu item updated successfully", item));
    }

    // ============== Sizes ==============

    @PostMapping("/items/{itemId}/variants")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a size to an item",
            description = "priceDelta is a DIFFERENCE from the item's price, not the price of the size.")
    public ResponseEntity<ApiResponse<ItemVariantDto>> addVariant(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @Validated(SaveItemVariantRequest.OnCreate.class) @RequestBody SaveItemVariantRequest request) {

        validateAccess(restaurantId, currentUser);
        ItemVariantDto variant = menuService.addVariant(restaurantId, itemId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Size added", variant));
    }

    @PutMapping("/items/{itemId}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a size", description = "Partial: a field left out is left alone.")
    public ResponseEntity<ApiResponse<ItemVariantDto>> updateVariant(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @PathVariable Long variantId,
            @Valid @RequestBody SaveItemVariantRequest request) {

        validateAccess(restaurantId, currentUser);
        ItemVariantDto variant = menuService.updateVariant(restaurantId, itemId, variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Size updated", variant));
    }

    @DeleteMapping("/items/{itemId}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a size",
            description = "Really deletes. Past orders keep the name and price they were charged. "
                    + "To hide a size you may want back, set active=false instead.")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @PathVariable Long variantId) {

        validateAccess(restaurantId, currentUser);
        menuService.deleteVariant(restaurantId, itemId, variantId);
        return ResponseEntity.ok(ApiResponse.success("Size deleted"));
    }

    // ============== Add-ons ==============

    @PostMapping("/items/{itemId}/options")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add an add-on to an item",
            description = "groupName turns a flat list into one question asked of the customer; "
                    + "required and maxSelections describe how that question may be answered.")
    public ResponseEntity<ApiResponse<ItemOptionDto>> addOption(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @Validated(SaveItemVariantRequest.OnCreate.class) @RequestBody SaveItemOptionRequest request) {

        validateAccess(restaurantId, currentUser);
        ItemOptionDto option = menuService.addOption(restaurantId, itemId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Add-on added", option));
    }

    @PutMapping("/items/{itemId}/options/{optionId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update an add-on", description = "Partial: a field left out is left alone.")
    public ResponseEntity<ApiResponse<ItemOptionDto>> updateOption(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @PathVariable Long optionId,
            @Valid @RequestBody SaveItemOptionRequest request) {

        validateAccess(restaurantId, currentUser);
        ItemOptionDto option = menuService.updateOption(restaurantId, itemId, optionId, request);
        return ResponseEntity.ok(ApiResponse.success("Add-on updated", option));
    }

    @DeleteMapping("/items/{itemId}/options/{optionId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete an add-on",
            description = "Really deletes. Past orders keep what they were charged.")
    public ResponseEntity<ApiResponse<Void>> deleteOption(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @PathVariable Long optionId) {

        validateAccess(restaurantId, currentUser);
        menuService.deleteOption(restaurantId, itemId, optionId);
        return ResponseEntity.ok(ApiResponse.success("Add-on deleted"));
    }

    @PatchMapping("/items/{itemId}/stock")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update item stock", description = "Update menu item stock status")
    public ResponseEntity<ApiResponse<MenuItemDto>> updateItemStock(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @RequestParam Boolean inStock) {

        validateAccess(restaurantId, currentUser);
        MenuItemDto item = menuService.updateItemStock(restaurantId, itemId, inStock);
        return ResponseEntity.ok(ApiResponse.success(
                "Item is now " + (inStock ? "in stock" : "out of stock"), item));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete item", description = "Delete a menu item (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId) {

        validateAccess(restaurantId, currentUser);
        menuService.deleteItem(restaurantId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted successfully"));
    }

    @PostMapping(value = "/items/{itemId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload item image", description = "Upload or update menu item image")
    public ResponseEntity<ApiResponse<MenuItemDto>> uploadItemImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file) {

        validateAccess(restaurantId, currentUser);
        log.info("Uploading image for menu item: {} in restaurant: {}", itemId, restaurantId);
        MenuItemDto item = menuService.updateItemImage(restaurantId, itemId, file);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", item));
    }

    @DeleteMapping("/items/{itemId}/image")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete item image", description = "Delete menu item image")
    public ResponseEntity<ApiResponse<MenuItemDto>> deleteItemImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId) {

        validateAccess(restaurantId, currentUser);
        log.info("Deleting image for menu item: {} in restaurant: {}", itemId, restaurantId);
        MenuItemDto item = menuService.deleteItemImage(restaurantId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", item));
    }
}

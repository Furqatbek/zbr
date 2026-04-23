package com.fooddelivery.integration.restos.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.integration.restos.dto.*;
import com.fooddelivery.integration.restos.service.RestosMenuImportService;
import com.fooddelivery.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for integrating with external Restos restaurant system.
 * Allows restaurant owners and admins to import menus from Restos.
 */
@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/restos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restos Integration", description = "Import menu from external Restos restaurant system")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'PLATFORM', 'ADMIN')")
public class RestosIntegrationController {

    private final RestosMenuImportService importService;
    private final RestaurantService restaurantService;

    /**
     * Import full menu from Restos (uses endpoint #3, falls back to #1 + #2).
     */
    @PostMapping("/import-menu")
    @Operation(summary = "Import menu from Restos", description = "Fetch categories and products from Restos and import into our system")
    public ResponseEntity<ApiResponse<MenuImportResult>> importMenu(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuImportRequest request) {

        validateAccess(restaurantId, currentUser);
        log.info("Menu import requested for restaurant {} from Restos {}", restaurantId, request.getBaseUrl());

        MenuImportResult result = importService.importFullMenu(restaurantId, request);
        return ResponseEntity.ok(ApiResponse.success("Menu imported successfully", result));
    }

    /**
     * Preview: fetch categories from Restos without importing (endpoint #1).
     */
    @PostMapping("/preview/categories")
    @Operation(summary = "Preview Restos categories", description = "Fetch categories from Restos without importing")
    public ResponseEntity<ApiResponse<List<RestosCategory>>> previewCategories(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuImportRequest request) {

        validateAccess(restaurantId, currentUser);
        String baseUrl = request.getBaseUrl();
        List<RestosCategory> categories = importService.fetchCachedMenu(
                baseUrl, request.getExternalRestaurantId(), request.getApiKey());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Preview: fetch categories with kitchen station info (endpoint #5).
     */
    @PostMapping("/preview/categories-kitchen")
    @Operation(summary = "Preview Restos categories with kitchen info", description = "Fetch categories with kitchen station assignments")
    public ResponseEntity<ApiResponse<List<RestosCategory>>> previewCategoriesWithKitchen(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuImportRequest request) {

        validateAccess(restaurantId, currentUser);
        List<RestosCategory> categories = importService.fetchCategoriesWithKitchenInfo(
                request.getBaseUrl(), request.getExternalRestaurantId(), request.getApiKey());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Preview: fetch all products from Restos (endpoint #6).
     */
    @PostMapping("/preview/products")
    @Operation(summary = "Preview Restos products", description = "Fetch all products from Restos with category info")
    public ResponseEntity<ApiResponse<List<RestosProduct>>> previewProducts(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuImportRequest request) {

        validateAccess(restaurantId, currentUser);
        List<RestosProduct> products = importService.fetchAllProducts(
                request.getBaseUrl(), request.getExternalRestaurantId(), request.getApiKey());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    private void validateAccess(Long restaurantId, UserPrincipal currentUser) {
        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(restaurantId, currentUser.getId(), isAdminOrPlatform);
    }
}

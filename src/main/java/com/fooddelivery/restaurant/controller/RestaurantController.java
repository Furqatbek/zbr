package com.fooddelivery.restaurant.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.order.dto.ReviewDto;
import com.fooddelivery.order.service.ReviewService;
import com.fooddelivery.restaurant.dto.CreateRestaurantRequest;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for restaurant operations.
 */
@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restaurants", description = "Restaurant management endpoints")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create restaurant", description = "Create a new restaurant")
    public ResponseEntity<ApiResponse<RestaurantDto>> createRestaurant(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateRestaurantRequest request) {

        RestaurantDto restaurant = restaurantService.createRestaurant(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant created successfully", restaurant));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant by ID", description = "Get restaurant details by ID")
    public ResponseEntity<ApiResponse<RestaurantDto>> getRestaurantById(@PathVariable Long id) {
        RestaurantDto restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(ApiResponse.success(restaurant));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get restaurant by slug", description = "Get restaurant details by URL slug")
    public ResponseEntity<ApiResponse<RestaurantDto>> getRestaurantBySlug(@PathVariable String slug) {
        RestaurantDto restaurant = restaurantService.getRestaurantBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(restaurant));
    }

    @GetMapping
    @Operation(summary = "Get all restaurants", description = "Get all restaurants with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantDto>>> getAllRestaurants(
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<RestaurantDto> restaurants = restaurantService.getAllRestaurants(pageable);
        return ResponseEntity.ok(ApiResponse.success(restaurants));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active restaurants", description = "Get active and open restaurants")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantDto>>> getActiveRestaurants(
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<RestaurantDto> restaurants = restaurantService.getActiveRestaurants(pageable);
        return ResponseEntity.ok(ApiResponse.success(restaurants));
    }

    @GetMapping("/search")
    @Operation(summary = "Search restaurants", description = "Search restaurants by name or description")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantDto>>> searchRestaurants(
            @Parameter(description = "Search query") @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<RestaurantDto> restaurants = restaurantService.searchRestaurants(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(restaurants));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured restaurants", description = "Get featured restaurants")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantDto>>> getFeaturedRestaurants(
            @PageableDefault(size = 10) Pageable pageable) {

        PagedResponse<RestaurantDto> restaurants = restaurantService.getFeaturedRestaurants(pageable);
        return ResponseEntity.ok(ApiResponse.success(restaurants));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby restaurants", description = "Get restaurants near a location")
    public ResponseEntity<ApiResponse<List<RestaurantDto>>> getNearbyRestaurants(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "10") double radius) {

        List<RestaurantDto> restaurants = restaurantService.getNearbyRestaurants(lat, lng, radius);
        return ResponseEntity.ok(ApiResponse.success(restaurants));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my restaurants", description = "Get restaurants owned by current user")
    public ResponseEntity<ApiResponse<List<RestaurantDto>>> getMyRestaurants(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<RestaurantDto> restaurants = restaurantService.getRestaurantsByOwner(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(restaurants));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update restaurant", description = "Update restaurant details")
    public ResponseEntity<ApiResponse<RestaurantDto>> updateRestaurant(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @Valid @RequestBody CreateRestaurantRequest request) {

        // Validate ownership (Admin/Platform can update any restaurant)
        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(id, currentUser.getId(), isAdminOrPlatform);

        RestaurantDto restaurant = restaurantService.updateRestaurant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated successfully", restaurant));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update restaurant status", description = "Update restaurant status (Admin only)")
    public ResponseEntity<ApiResponse<RestaurantDto>> updateRestaurantStatus(
            @PathVariable Long id,
            @RequestParam RestaurantStatus status) {

        RestaurantDto restaurant = restaurantService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Restaurant status updated", restaurant));
    }

    @PatchMapping("/{id}/toggle-open")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle open status", description = "Toggle restaurant open/closed status")
    public ResponseEntity<ApiResponse<RestaurantDto>> toggleOpenStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam Boolean isOpen) {

        // Validate ownership (Admin/Platform can toggle any restaurant)
        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(id, currentUser.getId(), isAdminOrPlatform);

        RestaurantDto restaurant = restaurantService.toggleOpenStatus(id, isOpen);
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant is now " + (isOpen ? "open" : "closed"), restaurant));
    }

    @GetMapping("/{restaurantId}/reviews")
    @Operation(summary = "Get restaurant reviews", description = "Get reviews for a restaurant")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewDto>>> getRestaurantReviews(
            @PathVariable Long restaurantId,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<ReviewDto> reviews = reviewService.getRestaurantReviews(restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
}

package com.fooddelivery.restaurant.controller;

import com.fooddelivery.analytics.financial.dto.GmvMetricsDto;
import com.fooddelivery.analytics.financial.dto.RestaurantPayoutMetricsDto;
import com.fooddelivery.analytics.financial.service.FinancialAnalyticsService;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.order.dto.ReviewDto;
import com.fooddelivery.order.service.ReviewService;
import com.fooddelivery.restaurant.dto.CreateRestaurantRequest;
import com.fooddelivery.restaurant.dto.UpdateRestaurantRequest;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import com.fooddelivery.restaurant.dto.RestaurantFinancialReportDto;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final FinancialAnalyticsService financialAnalyticsService;

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
    @Operation(summary = "Update restaurant", description = "Partial update — only provided fields are changed")
    public ResponseEntity<ApiResponse<RestaurantDto>> updateRestaurant(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRestaurantRequest request) {

        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(id, currentUser.getId(), isAdminOrPlatform);

        RestaurantDto restaurant = restaurantService.updateRestaurantPartial(id, request);
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated successfully", restaurant));
    }

    @PatchMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update restaurant location", description = "Update only latitude and longitude")
    public ResponseEntity<ApiResponse<RestaurantDto>> updateLocation(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {

        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(id, currentUser.getId(), isAdminOrPlatform);

        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
        RestaurantDto restaurant = restaurantService.updateRestaurantPartial(id, request);
        return ResponseEntity.ok(ApiResponse.success("Location updated", restaurant));
    }

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload restaurant logo", description = "Upload a logo image for the restaurant")
    public ResponseEntity<ApiResponse<RestaurantDto>> uploadLogo(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(id, currentUser.getId(), isAdminOrPlatform);

        RestaurantDto restaurant = restaurantService.updateRestaurantImage(id, file, "logo");
        return ResponseEntity.ok(ApiResponse.success("Logo uploaded successfully", restaurant));
    }

    @PostMapping("/{id}/cover-image")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload restaurant cover image", description = "Upload a cover/banner image for the restaurant")
    public ResponseEntity<ApiResponse<RestaurantDto>> uploadCoverImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(id, currentUser.getId(), isAdminOrPlatform);

        RestaurantDto restaurant = restaurantService.updateRestaurantImage(id, file, "cover");
        return ResponseEntity.ok(ApiResponse.success("Cover image uploaded successfully", restaurant));
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

    @GetMapping("/{restaurantId}/financial-report")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get restaurant financial report",
               description = "Get financial report for a specific restaurant including revenue, payouts, and order metrics")
    public ResponseEntity<ApiResponse<RestaurantFinancialReportDto>> getFinancialReport(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        // Validate ownership (Admin/Platform can view any restaurant's report)
        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(restaurantId, currentUser.getId(), isAdminOrPlatform);

        // Get GMV metrics for this restaurant
        GmvMetricsDto gmvMetrics = financialAnalyticsService.getGmvByRestaurants(
                startDate, endDate, List.of(restaurantId));

        // Get payout details for this restaurant
        RestaurantPayoutMetricsDto payoutMetrics = financialAnalyticsService.getRestaurantPayoutDetails(
                restaurantId, startDate, endDate);

        // Build the financial report
        RestaurantFinancialReportDto report = RestaurantFinancialReportDto.builder()
                .restaurantId(restaurantId)
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalRevenue(gmvMetrics.getTotalGmv())
                .totalOrders(gmvMetrics.getTotalOrders())
                .averageOrderValue(gmvMetrics.getAverageOrderValue())
                .foodRevenue(gmvMetrics.getFoodGmv())
                .deliveryFeeRevenue(gmvMetrics.getDeliveryFeeGmv())
                .tipRevenue(gmvMetrics.getTipGmv())
                .growthRate(gmvMetrics.getGrowthRate())
                .grossSales(payoutMetrics.getTotalGrossSales())
                .commissionsDeducted(payoutMetrics.getTotalCommissionsDeducted())
                .deliverySubsidies(payoutMetrics.getTotalDeliverySubsidies())
                .promotionCosts(payoutMetrics.getTotalPromotionCosts())
                .adjustments(payoutMetrics.getTotalAdjustments())
                .fees(payoutMetrics.getTotalFees())
                .netPayout(payoutMetrics.getNetPayoutAmount())
                .pendingPayouts(payoutMetrics.getPendingPayouts())
                .completedPayouts(payoutMetrics.getCompletedPayouts())
                .dailyRevenueTrend(gmvMetrics.getDailyTrend())
                .dailyPayoutTrend(payoutMetrics.getDailyTrend())
                .build();

        return ResponseEntity.ok(ApiResponse.success(report));
    }
}

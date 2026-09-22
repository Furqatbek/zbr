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
import com.fooddelivery.restaurant.dto.TransferOwnershipRequest;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import com.fooddelivery.common.i18n.RequestLanguage;
import com.fooddelivery.restaurant.dto.RestaurantCategoryDto;
import com.fooddelivery.restaurant.dto.SaveRestaurantCategoryRequest;
import com.fooddelivery.restaurant.service.RestaurantCategoryService;
import com.fooddelivery.restaurant.service.RestaurantEtaEnricher;
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
    private final RestaurantEtaEnricher etaEnricher;
    private final RestaurantCategoryService categoryService;
    private final ReviewService reviewService;
    private final FinancialAnalyticsService financialAnalyticsService;

    /*
     * lat/lng are optional on every customer-facing read below. Sent, the
     * response carries distanceKm and an arrival range; omitted, it is exactly
     * what it was before — the app then shows preparation time alone, which is
     * what it showed until now.
     *
     * They are query parameters rather than something read from the customer's
     * saved address because the answer depends on where the phone is standing,
     * which is the question the customer is actually asking of a restaurant
     * list.
     */

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

    /**
     * The chips above the restaurant list.
     *
     * <p>Kept next to {@code /{id}} for readability, not for correctness:
     * Spring matches on pattern specificity, not declaration order, so a
     * literal segment beats a path variable wherever it is written. The live
     * API answering {@code 400 Invalid id: 'categories'} means this endpoint
     * is not deployed there yet, not that it is ordered wrongly.
     */
    @GetMapping("/categories")
    @Operation(summary = "List cuisine categories",
            description = "Public. Only categories with at least one open restaurant, so a chip "
                    + "never filters to an empty list. Names follow Accept-Language (uz, ru, en).")
    public ResponseEntity<ApiResponse<List<RestaurantCategoryDto>>> getCategories(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        List<RestaurantCategoryDto> categories =
                categoryService.listForCustomers(RequestLanguage.from(acceptLanguage));
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant by ID", description = "Get restaurant details by ID")
    public ResponseEntity<ApiResponse<RestaurantDto>> getRestaurantById(
            @PathVariable Long id,
            @Parameter(description = "Customer latitude, for distance and arrival time")
            @RequestParam(required = false) BigDecimal lat,
            @Parameter(description = "Customer longitude, for distance and arrival time")
            @RequestParam(required = false) BigDecimal lng,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        RestaurantDto restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(ApiResponse.success(etaEnricher.forRequest(
                restaurant, lat, lng, RequestLanguage.from(acceptLanguage))));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get restaurant by slug", description = "Get restaurant details by URL slug")
    public ResponseEntity<ApiResponse<RestaurantDto>> getRestaurantBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        RestaurantDto restaurant = restaurantService.getRestaurantBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(etaEnricher.forRequest(
                restaurant, lat, lng, RequestLanguage.from(acceptLanguage))));
    }

    /**
     * Every restaurant, whatever its status.
     *
     * <p>It takes {@code lat}/{@code lng}, {@code categoryId} and
     * {@code Accept-Language} like the customer-facing lists do. It did not,
     * and that mattered more than it looked: this is the endpoint the customer
     * app called for its home screen, so it sent coordinates on every request
     * and no customer ever saw a distance or an arrival estimate, while
     * {@code /active} answered both. An endpoint that accepts half its query
     * string and silently drops the rest will catch the next caller too.
     *
     * <p>Note what this list still is: {@code findAll}, including PENDING and
     * INACTIVE restaurants. {@code /active} is the one that means "open for
     * business".
     */
    @GetMapping
    @Operation(summary = "Get all restaurants",
            description = "Every restaurant regardless of status. For a customer-facing list use "
                    + "/active, which returns only active and open ones.")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantDto>>> getAllRestaurants(
            @Parameter(description = "Show only this cuisine") @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<RestaurantDto> restaurants =
                restaurantService.getAllRestaurants(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(etaEnricher.forRequest(
                restaurants, lat, lng, RequestLanguage.from(acceptLanguage))));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active restaurants", description = "Get active and open restaurants")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantDto>>> getActiveRestaurants(
            @Parameter(description = "Show only this cuisine") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Show only featured restaurants") @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PageableDefault(size = 20) Pageable pageable) {

        // Both filters go into the query, not into the page that comes back.
        // Filtering a page would answer "the burgers on page 1", which looks
        // identical to "the burgers" and is not.
        PagedResponse<RestaurantDto> restaurants =
                restaurantService.getActiveRestaurants(categoryId, featured, pageable);
        return ResponseEntity.ok(ApiResponse.success(etaEnricher.forRequest(
                restaurants, lat, lng, RequestLanguage.from(acceptLanguage))));
    }

    @GetMapping("/search")
    @Operation(summary = "Search restaurants", description = "Search restaurants by name or description")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantDto>>> searchRestaurants(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Search only within this cuisine")
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<RestaurantDto> restaurants = restaurantService.searchRestaurants(q, categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(etaEnricher.forRequest(
                restaurants, lat, lng, RequestLanguage.from(acceptLanguage))));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured restaurants", description = "Get featured restaurants")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantDto>>> getFeaturedRestaurants(
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @PageableDefault(size = 10) Pageable pageable) {

        PagedResponse<RestaurantDto> restaurants = restaurantService.getFeaturedRestaurants(pageable);
        return ResponseEntity.ok(ApiResponse.success(etaEnricher.forRequest(
                restaurants, lat, lng, RequestLanguage.from(acceptLanguage))));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby restaurants", description = "Get restaurants near a location")
    public ResponseEntity<ApiResponse<List<RestaurantDto>>> getNearbyRestaurants(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "10") double radius,
            @Parameter(description = "Show only this cuisine") @RequestParam(required = false) Long categoryId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        // Nearby already knows where the customer is, so distance and arrival
        // time are not optional here — a list sorted by proximity that does not
        // say the proximity was the odd part.
        List<RestaurantDto> restaurants =
                restaurantService.getNearbyRestaurants(lat, lng, radius, categoryId);
        return ResponseEntity.ok(ApiResponse.success(etaEnricher.forRequest(
                restaurants, lat, lng, RequestLanguage.from(acceptLanguage))));
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

    // ADMIN/PLATFORM only, deliberately. An owner must not be able to hand a
    // restaurant to someone else — that is a commercial decision, and letting
    // the current owner make it turns a compromised owner account into a way to
    // move the business out of reach. Transfers go through the platform.
    @PatchMapping("/{id}/owner")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Transfer restaurant ownership",
               description = "Move a restaurant to a different owner. Grants the new owner the "
                       + "RESTAURANT_OWNER role if they lack it, and revokes the previous owner's "
                       + "access immediately. Admin/platform only.")
    public ResponseEntity<ApiResponse<RestaurantDto>> transferOwnership(
            @PathVariable Long id,
            @Valid @RequestBody TransferOwnershipRequest request) {

        RestaurantDto restaurant = restaurantService.transferOwnership(id, request.getNewOwnerId());
        return ResponseEntity.ok(ApiResponse.success("Ownership transferred", restaurant));
    }

    @PatchMapping("/{id}/featured")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Feature a restaurant",
            description = "Puts the restaurant in the recommended carousel, or takes it out. "
                    + "Admin/platform only — a restaurant that could feature itself would.")
    public ResponseEntity<ApiResponse<RestaurantDto>> setFeatured(
            @PathVariable Long id,
            @RequestParam Boolean featured) {

        RestaurantDto restaurant = restaurantService.setFeatured(id, featured);
        return ResponseEntity.ok(ApiResponse.success(
                Boolean.TRUE.equals(featured) ? "Restaurant featured" : "Restaurant unfeatured",
                restaurant));
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

        // Get restaurant-scoped revenue metrics (food revenue, order count, commission)
        GmvMetricsDto gmvMetrics = financialAnalyticsService.getGmvForRestaurant(
                restaurantId, startDate, endDate);

        // Get payout details for this restaurant
        RestaurantPayoutMetricsDto payoutMetrics = financialAnalyticsService.getRestaurantPayoutDetails(
                restaurantId, startDate, endDate);

        // Build the financial report
        RestaurantFinancialReportDto report = RestaurantFinancialReportDto.builder()
                .restaurantId(restaurantId)
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalRevenue(gmvMetrics.getFoodGmv())
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

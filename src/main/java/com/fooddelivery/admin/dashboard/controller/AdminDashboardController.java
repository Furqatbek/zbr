package com.fooddelivery.admin.dashboard.controller;

import com.fooddelivery.admin.dashboard.dto.*;
import com.fooddelivery.admin.dashboard.service.AdminDashboardService;
import com.fooddelivery.admin.dashboard.util.DashboardConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * REST controller for admin dashboard operations.
 * Provides endpoints for real-time operational insights.
 */
@Slf4j
@RestController
@RequestMapping(DashboardConstants.API_BASE_PATH)
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Real-time operational dashboard for administrators")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    /**
     * Get dashboard overview with key metrics.
     */
    @GetMapping(DashboardConstants.ENDPOINT_OVERVIEW)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    @Operation(summary = "Get dashboard overview",
            description = "Returns comprehensive overview including orders, revenue, active entities, and system status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<DashboardOverviewDto> getOverview(
            @Parameter(description = "Start date (defaults to today)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date (defaults to today)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = startDate != null
                ? startDate.atStartOfDay()
                : LocalDate.now().atStartOfDay();
        LocalDateTime end = endDate != null
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        log.info("Fetching dashboard overview for period: {} to {}", start, end);

        DashboardOverviewDto overview = dashboardService.getOverview(start, end);
        return ResponseEntity.ok(overview);
    }

    /**
     * Get active orders.
     */
    @PostMapping(DashboardConstants.ENDPOINT_ACTIVE_ORDERS)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'SUPPORT_AGENT')")
    @Operation(summary = "Get active orders",
            description = "Returns all orders currently in progress with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active orders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ActiveOrdersDto> getActiveOrders(
            @Valid @RequestBody(required = false) DashboardFilterRequest filter) {

        filter = ensureDefaults(filter);
        log.info("Fetching active orders with filter: {}", filter);

        ActiveOrdersDto activeOrders = dashboardService.getActiveOrders(filter);
        return ResponseEntity.ok(activeOrders);
    }

    /**
     * Get active orders with GET (simpler access).
     */
    @GetMapping(DashboardConstants.ENDPOINT_ACTIVE_ORDERS)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'SUPPORT_AGENT')")
    @Operation(summary = "Get active orders (GET)",
            description = "Returns active orders with default filters")
    public ResponseEntity<ActiveOrdersDto> getActiveOrdersGet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {

        DashboardFilterRequest filter = DashboardFilterRequest.builder()
                .startDate(date != null ? date.atStartOfDay() : LocalDate.now().atStartOfDay())
                .endDate(date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now())
                .page(page)
                .pageSize(pageSize)
                .build();

        return getActiveOrders(filter);
    }

    /**
     * Get stuck orders.
     */
    @PostMapping(DashboardConstants.ENDPOINT_STUCK_ORDERS)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    @Operation(summary = "Get stuck orders",
            description = "Returns orders that have exceeded their stage thresholds, prioritized by urgency")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stuck orders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<StuckOrdersDto> getStuckOrders(
            @Valid @RequestBody(required = false) DashboardFilterRequest filter) {

        filter = ensureDefaults(filter);
        log.info("Fetching stuck orders with filter: {}", filter);

        StuckOrdersDto stuckOrders = dashboardService.getStuckOrders(filter);
        return ResponseEntity.ok(stuckOrders);
    }

    /**
     * Get stuck orders with GET.
     */
    @GetMapping(DashboardConstants.ENDPOINT_STUCK_ORDERS)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    @Operation(summary = "Get stuck orders (GET)",
            description = "Returns stuck orders with default thresholds")
    public ResponseEntity<StuckOrdersDto> getStuckOrdersGet(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {

        DashboardFilterRequest filter = DashboardFilterRequest.builder()
                .startDate(LocalDate.now().atStartOfDay())
                .endDate(LocalDateTime.now())
                .page(page)
                .pageSize(pageSize)
                .build();

        return getStuckOrders(filter);
    }

    /**
     * Get cancelled orders.
     */
    @PostMapping("/orders/cancelled")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    @Operation(summary = "Get cancelled orders",
            description = "Returns cancelled orders with breakdown by reason")
    public ResponseEntity<CanceledOrdersDto> getCancelledOrders(
            @Valid @RequestBody(required = false) DashboardFilterRequest filter) {

        filter = ensureDefaults(filter);
        log.info("Fetching cancelled orders with filter: {}", filter);

        CanceledOrdersDto cancelledOrders = dashboardService.getCancelledOrders(filter);
        return ResponseEntity.ok(cancelledOrders);
    }

    /**
     * Get rejected orders.
     */
    @PostMapping("/orders/rejected")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    @Operation(summary = "Get rejected orders",
            description = "Returns rejected orders with restaurant breakdown")
    public ResponseEntity<RejectedOrdersDto> getRejectedOrders(
            @Valid @RequestBody(required = false) DashboardFilterRequest filter) {

        filter = ensureDefaults(filter);
        log.info("Fetching rejected orders with filter: {}", filter);

        RejectedOrdersDto rejectedOrders = dashboardService.getRejectedOrders(filter);
        return ResponseEntity.ok(rejectedOrders);
    }

    /**
     * Get restaurant metrics.
     */
    @PostMapping(DashboardConstants.ENDPOINT_RESTAURANTS)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'RESTAURANT_MANAGER')")
    @Operation(summary = "Get restaurant metrics",
            description = "Returns restaurant metrics including online/offline status and performance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant metrics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<RestaurantMetricsDto> getRestaurantMetrics(
            @Valid @RequestBody(required = false) DashboardFilterRequest filter) {

        filter = ensureDefaults(filter);
        log.info("Fetching restaurant metrics with filter: {}", filter);

        RestaurantMetricsDto restaurantMetrics = dashboardService.getRestaurantMetrics(filter);
        return ResponseEntity.ok(restaurantMetrics);
    }

    /**
     * Get restaurant metrics with GET.
     */
    @GetMapping(DashboardConstants.ENDPOINT_RESTAURANTS)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'RESTAURANT_MANAGER')")
    @Operation(summary = "Get restaurant metrics (GET)")
    public ResponseEntity<RestaurantMetricsDto> getRestaurantMetricsGet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {

        DashboardFilterRequest filter = DashboardFilterRequest.builder()
                .startDate(date != null ? date.atStartOfDay() : LocalDate.now().atStartOfDay())
                .endDate(date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now())
                .page(page)
                .pageSize(pageSize)
                .build();

        return getRestaurantMetrics(filter);
    }

    /**
     * Get courier metrics.
     */
    @PostMapping(DashboardConstants.ENDPOINT_COURIERS)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'FLEET_MANAGER')")
    @Operation(summary = "Get courier metrics",
            description = "Returns courier metrics including locations and availability")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier metrics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CourierMetricsDto> getCourierMetrics(
            @Valid @RequestBody(required = false) DashboardFilterRequest filter) {

        filter = ensureDefaults(filter);
        log.info("Fetching courier metrics with filter: {}", filter);

        CourierMetricsDto courierMetrics = dashboardService.getCourierMetrics(filter);
        return ResponseEntity.ok(courierMetrics);
    }

    /**
     * Get courier metrics with GET.
     */
    @GetMapping(DashboardConstants.ENDPOINT_COURIERS)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'FLEET_MANAGER')")
    @Operation(summary = "Get courier metrics (GET)")
    public ResponseEntity<CourierMetricsDto> getCourierMetricsGet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {

        DashboardFilterRequest filter = DashboardFilterRequest.builder()
                .startDate(date != null ? date.atStartOfDay() : LocalDate.now().atStartOfDay())
                .endDate(date != null ? date.atTime(LocalTime.MAX) : LocalDateTime.now())
                .page(page)
                .pageSize(pageSize)
                .build();

        return getCourierMetrics(filter);
    }

    /**
     * Get finance metrics.
     */
    @PostMapping(DashboardConstants.ENDPOINT_FINANCE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER')")
    @Operation(summary = "Get finance metrics",
            description = "Returns financial metrics including GMV, revenue, and payouts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Finance metrics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<FinanceMetricsDto> getFinanceMetrics(
            @Valid @RequestBody(required = false) DashboardFilterRequest filter) {

        filter = ensureDefaults(filter);
        log.info("Fetching finance metrics with filter: {}", filter);

        FinanceMetricsDto financeMetrics = dashboardService.getFinanceMetrics(filter);
        return ResponseEntity.ok(financeMetrics);
    }

    /**
     * Get finance metrics with GET.
     */
    @GetMapping(DashboardConstants.ENDPOINT_FINANCE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER')")
    @Operation(summary = "Get finance metrics (GET)")
    public ResponseEntity<FinanceMetricsDto> getFinanceMetricsGet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        DashboardFilterRequest filter = DashboardFilterRequest.builder()
                .startDate(startDate != null ? startDate.atStartOfDay() : LocalDate.now().atStartOfDay())
                .endDate(endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now())
                .build();

        return getFinanceMetrics(filter);
    }

    /**
     * Get support metrics.
     */
    @PostMapping(DashboardConstants.ENDPOINT_SUPPORT)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_MANAGER', 'SUPPORT_AGENT')")
    @Operation(summary = "Get support metrics",
            description = "Returns support metrics including tickets and agent performance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Support metrics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<SupportMetricsDto> getSupportMetrics(
            @Valid @RequestBody(required = false) DashboardFilterRequest filter) {

        filter = ensureDefaults(filter);
        log.info("Fetching support metrics with filter: {}", filter);

        SupportMetricsDto supportMetrics = dashboardService.getSupportMetrics(filter);
        return ResponseEntity.ok(supportMetrics);
    }

    /**
     * Get support metrics with GET.
     */
    @GetMapping(DashboardConstants.ENDPOINT_SUPPORT)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_MANAGER', 'SUPPORT_AGENT')")
    @Operation(summary = "Get support metrics (GET)")
    public ResponseEntity<SupportMetricsDto> getSupportMetricsGet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {

        DashboardFilterRequest filter = DashboardFilterRequest.builder()
                .startDate(startDate != null ? startDate.atStartOfDay() : LocalDate.now().atStartOfDay())
                .endDate(endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now())
                .page(page)
                .pageSize(pageSize)
                .build();

        return getSupportMetrics(filter);
    }

    /**
     * Refresh all dashboard caches.
     */
    @PostMapping(DashboardConstants.ENDPOINT_CACHE_REFRESH)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refresh all caches",
            description = "Clears all dashboard caches to force data refresh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caches refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin only")
    })
    public ResponseEntity<Map<String, String>> refreshAllCaches() {
        log.info("Refreshing all dashboard caches");
        dashboardService.refreshCache();
        return ResponseEntity.ok(Map.of("status", "success", "message", "All caches refreshed"));
    }

    /**
     * Refresh specific cache.
     */
    @PostMapping(DashboardConstants.ENDPOINT_CACHE_REFRESH + "/{cacheKey}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refresh specific cache",
            description = "Clears a specific dashboard cache")
    public ResponseEntity<Map<String, String>> refreshCache(
            @PathVariable String cacheKey) {
        log.info("Refreshing dashboard cache: {}", cacheKey);
        dashboardService.refreshCache(cacheKey);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Cache '" + cacheKey + "' refreshed"));
    }

    /**
     * Ensure filter has default values.
     */
    private DashboardFilterRequest ensureDefaults(DashboardFilterRequest filter) {
        if (filter == null) {
            return DashboardFilterRequest.builder()
                    .startDate(LocalDate.now().atStartOfDay())
                    .endDate(LocalDateTime.now())
                    .page(DashboardConstants.DEFAULT_PAGE)
                    .pageSize(DashboardConstants.DEFAULT_PAGE_SIZE)
                    .build();
        }

        if (filter.getStartDate() == null) {
            filter.setStartDate(LocalDate.now().atStartOfDay());
        }
        if (filter.getEndDate() == null) {
            filter.setEndDate(LocalDateTime.now());
        }
        if (filter.getPage() == null) {
            filter.setPage(DashboardConstants.DEFAULT_PAGE);
        }
        if (filter.getSize() == null) {
            filter.setSize(DashboardConstants.DEFAULT_PAGE_SIZE);
        }

        return filter;
    }
}

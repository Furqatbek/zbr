package com.fooddelivery.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for restaurant metrics panel.
 * Provides operational metrics for restaurants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantMetricsDto {

    // Summary for all restaurants or single restaurant
    private Long totalRestaurants;
    private Long onlineRestaurants;
    private Long offlineRestaurants;
    private Long busyRestaurants;
    private Long acceptingOrders;
    private Long pausedRestaurants;

    // Aggregate metrics
    private Double avgPrepTime; // minutes
    private Double avgAcceptanceLatency; // minutes from order to acceptance
    private Double avgOrderRating;
    private Double orderAcceptanceRate; // percentage

    // Performance tiers
    private Long highPerformingRestaurants;  // rating > 4.5, fast prep
    private Long averagePerformingRestaurants;
    private Long underPerformingRestaurants; // high rejection, slow prep

    // List of restaurant details
    private List<RestaurantDetailDto> restaurants;

    // Single restaurant detail (when querying by ID)
    private RestaurantDetailDto restaurantDetail;

    // Pagination
    private Integer currentPage;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantDetailDto {
        private Long restaurantId;
        private String name;
        private String status; // ONLINE, OFFLINE, PAUSED, BUSY
        private Boolean isOpen;
        private Boolean isOnline;
        private Boolean isAcceptingOrders;

        // Location
        private String address;
        private String city;
        private String cuisineType;
        private BigDecimal latitude;
        private BigDecimal longitude;

        // Operating hours
        private LocalTime opensAt;
        private LocalTime closesAt;

        // Today's metrics
        private Long ordersToday;
        private Long totalOrdersToday;
        private Long ordersAccepted;
        private Long ordersRejected;
        private Long rejectedOrdersToday;
        private Long ordersCanceled;
        private BigDecimal revenueToday;

        // Performance metrics
        private Double avgPrepTime; // minutes
        private Double avgPreparationTimeMinutes;
        private Double avgAcceptanceLatency; // seconds from order to acceptance
        private Double orderAcceptanceLatencySeconds;
        private Double acceptanceRate;
        private Double rejectionRate;
        private Double cancellationRate;
        private Double performanceScore;

        // Rating
        private Double rating;
        private BigDecimal avgRating;
        private Integer totalRatings;
        private Integer ratingsToday;

        // Capacity
        private Integer currentActiveOrders;
        private Integer maxConcurrentOrders;
        private Double capacityUtilization; // percentage

        // Historical comparison
        private Long ordersYesterday;
        private BigDecimal revenueYesterday;
        private Double orderChangePercent;

        // Timestamps
        private LocalDateTime lastOrderAt;

        // Issues/Alerts
        private List<String> activeAlerts;
        private Integer stuckOrdersCount;

        // Order breakdown by hour (for today)
        private Map<Integer, Long> ordersByHour;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantPerformanceSummaryDto {
        private Long restaurantId;
        private String name;
        private Double performanceScore; // 0-100
        private String tier; // TOP, GOOD, AVERAGE, POOR
        private Double avgPrepTime;
        private Double avgPreparationTimeMinutes;
        private Double acceptanceRate;
        private BigDecimal avgRating;
        private Long totalOrders;
        private Long rejectedOrdersCount;
        private Double rejectionRate;
        private List<String> improvementSuggestions;
    }
}

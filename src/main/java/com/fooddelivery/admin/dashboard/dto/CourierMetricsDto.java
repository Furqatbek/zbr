package com.fooddelivery.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for courier metrics panel.
 * Provides operational metrics for couriers including locations and performance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierMetricsDto {

    // Summary counts
    private Long totalCouriers;
    private Long onlineCouriers;
    private Long offlineCouriers;
    private Long availableCouriers;
    private Long busyCouriers;
    private Long onBreakCouriers;
    private Long onDeliveryCouriers;

    // Active couriers (recent location ping)
    private Long activeCouriers; // Location ping < 3 minutes
    private Double activePercentage;
    private Double utilizationRate;

    // Performance aggregates
    private Double avgDeliveryTime; // minutes
    private Double avgAcceptanceRate;
    private Double avgCancellationRate;
    private Double avgRating;

    // Performance summary
    private CourierPerformanceSummaryDto performanceSummary;

    // Today's metrics
    private Long deliveriesToday;
    private BigDecimal earningsToday;
    private Long ordersAcceptedToday;
    private Long ordersRejectedToday;

    // Vehicle type breakdown
    private Map<String, Long> couriersByVehicle; // BICYCLE, MOTORCYCLE, CAR
    private Map<String, Long> vehicleDistribution;

    // Status breakdown
    private Map<String, Long> statusBreakdown;

    // Availability heatmap (day -> hour -> count)
    private Map<String, Map<Integer, Long>> availabilityHeatmap;

    // Top performers
    private List<CourierDetailDto> topPerformers;

    // List of courier details
    private List<CourierDetailDto> couriers;

    // Single courier detail (when querying by ID)
    private CourierDetailDto courierDetail;

    // Availability heatmap (couriers available per hour)
    private Map<Integer, Long> availabilityByHour;

    // Geographic distribution
    private List<CourierLocationDto> courierLocations;

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
    public static class CourierDetailDto {
        private Long courierId;
        private String name;
        private String phone;
        private String email;
        private String status; // AVAILABLE, BUSY, OFFLINE, ON_BREAK
        private Boolean isActive;
        private String vehicleType;
        private String vehicleNumber;

        // Current location
        private BigDecimal currentLat;
        private BigDecimal currentLng;
        private Double currentLatitude;
        private Double currentLongitude;
        private LocalDateTime locationUpdatedAt;
        private LocalDateTime lastLocationPingAt;
        private Long secondsSinceLastPing;
        private Boolean isLocationStale; // > 3 minutes

        // Current assignments
        private Long currentOrderId;
        private Integer currentOrderCount;
        private Integer maxConcurrentOrders;
        private List<Long> activeOrderIds;

        // Today's performance
        private Long deliveriesToday;
        private BigDecimal earningsToday;
        private Long ordersAcceptedToday;
        private Long ordersRejectedToday;
        private Double avgDeliveryTimeToday; // minutes
        private Double avgDeliveryTimeMinutes;

        // Overall performance
        private Integer totalDeliveries;
        private BigDecimal totalEarnings;
        private Double acceptanceRate;
        private Double cancellationRate;
        private Double onTimeDeliveryRate;
        private Double performanceScore;
        private Double rating;
        private BigDecimal avgRating;
        private Integer totalRatings;

        // Verification
        private Boolean isVerified;
        private LocalDateTime verifiedAt;

        // Issues/Alerts
        private List<String> activeAlerts;
        private Integer lateDeliveriesToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourierLocationDto {
        private Long courierId;
        private String name;
        private String courierName;
        private String status;
        private BigDecimal latitudeBd;
        private BigDecimal longitudeBd;
        private Double latitude;
        private Double longitude;
        private LocalDateTime lastPingAt;
        private Long currentOrderId;
        private Integer activeOrders;
        private Boolean isAvailable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourierPerformanceSummaryDto {
        private Long courierId;
        private String name;
        private Double performanceScore; // 0-100
        private String tier; // TOP, GOOD, AVERAGE, POOR
        private Double avgDeliveryTime;
        private Double avgDeliveryTimeMinutes;
        private Double acceptanceRate;
        private BigDecimal avgRating;
        private Long totalDeliveries;
        private Long totalDeliveriesToday;
        private Double onTimeDeliveryRate;
        private Double avgDeliveriesPerCourier;
        private Double avgDistancePerDeliveryKm;
        private List<String> improvementSuggestions;
    }
}

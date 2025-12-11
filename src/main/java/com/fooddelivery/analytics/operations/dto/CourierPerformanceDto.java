package com.fooddelivery.analytics.operations.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Courier Performance metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Courier performance metrics")
public class CourierPerformanceDto implements Serializable {

    @Schema(description = "Courier ID")
    private Long courierId;

    @Schema(description = "Courier name")
    private String courierName;

    @Schema(description = "Analysis period start")
    private LocalDateTime periodStart;

    @Schema(description = "Analysis period end")
    private LocalDateTime periodEnd;

    // Job acceptance metrics
    @Schema(description = "Total orders offered to courier")
    private Long totalOrdersOffered;

    @Schema(description = "Orders accepted")
    private Long ordersAccepted;

    @Schema(description = "Orders rejected")
    private Long ordersRejected;

    @Schema(description = "Orders timed out (no response)")
    private Long ordersTimedOut;

    @Schema(description = "Acceptance rate (%)", example = "87.5")
    private Double acceptanceRate;

    @Schema(description = "Average response time to offers (seconds)")
    private Double averageResponseTimeSeconds;

    // Delivery metrics
    @Schema(description = "Total deliveries completed")
    private Long deliveriesCompleted;

    @Schema(description = "Average delivery time (minutes)", example = "12.5")
    private Double averageDeliveryTimeMinutes;

    @Schema(description = "Median delivery time (minutes)")
    private Double medianDeliveryTimeMinutes;

    @Schema(description = "90th percentile delivery time (minutes)")
    private Double p90DeliveryTimeMinutes;

    @Schema(description = "Fastest delivery (minutes)")
    private Long fastestDeliveryMinutes;

    @Schema(description = "Slowest delivery (minutes)")
    private Long slowestDeliveryMinutes;

    // Cancellation metrics
    @Schema(description = "Orders cancelled by courier after accepting")
    private Long ordersCancelledByCourier;

    @Schema(description = "Cancellation rate (%)")
    private Double cancellationRate;

    @Schema(description = "Common cancellation reasons")
    private Map<String, Long> cancellationReasons;

    // Availability metrics
    @Schema(description = "Total active (online) time in period (minutes)")
    private Long totalActiveTimeMinutes;

    @Schema(description = "Total available time (not busy) in period (minutes)")
    private Long totalAvailableTimeMinutes;

    @Schema(description = "Utilization rate (busy time / active time %)")
    private Double utilizationRate;

    @Schema(description = "Average active hours per day")
    private Double averageActiveHoursPerDay;

    @Schema(description = "Availability by hour of day (minutes available)")
    private Map<Integer, Long> availabilityByHour;

    // Location update metrics
    @Schema(description = "Total location updates in period")
    private Long totalLocationUpdates;

    @Schema(description = "Location updates per hour (average)")
    private Double locationUpdatesPerHour;

    @Schema(description = "Percentage of time with recent location (< 5 min old)")
    private Double locationFreshnessRate;

    // Distance and earnings
    @Schema(description = "Total distance traveled (km)")
    private Double totalDistanceKm;

    @Schema(description = "Total earnings in period")
    private BigDecimal totalEarnings;

    @Schema(description = "Earnings per delivery")
    private BigDecimal earningsPerDelivery;

    @Schema(description = "Earnings per hour")
    private BigDecimal earningsPerHour;

    // Rating metrics
    @Schema(description = "Average customer rating")
    private BigDecimal averageRating;

    @Schema(description = "Total ratings received")
    private Integer totalRatings;

    @Schema(description = "5-star ratings count")
    private Integer fiveStarRatings;

    @Schema(description = "1-star ratings count")
    private Integer oneStarRatings;

    // Daily breakdown
    @Schema(description = "Daily performance metrics")
    private List<DailyCourierMetrics> dailyMetrics;

    /**
     * Calculate derived metrics.
     */
    public void calculateDerivedMetrics() {
        if (totalOrdersOffered != null && totalOrdersOffered > 0) {
            if (ordersAccepted != null) {
                this.acceptanceRate = (ordersAccepted * 100.0) / totalOrdersOffered;
            }
        }

        if (ordersAccepted != null && ordersAccepted > 0 && ordersCancelledByCourier != null) {
            this.cancellationRate = (ordersCancelledByCourier * 100.0) / ordersAccepted;
        }

        if (totalActiveTimeMinutes != null && totalActiveTimeMinutes > 0) {
            long busyTime = totalActiveTimeMinutes - (totalAvailableTimeMinutes != null ? totalAvailableTimeMinutes : 0);
            this.utilizationRate = (busyTime * 100.0) / totalActiveTimeMinutes;
        }

        if (totalActiveTimeMinutes != null && totalActiveTimeMinutes > 0 && totalLocationUpdates != null) {
            double hours = totalActiveTimeMinutes / 60.0;
            this.locationUpdatesPerHour = totalLocationUpdates / hours;
        }

        if (deliveriesCompleted != null && deliveriesCompleted > 0 && totalEarnings != null) {
            this.earningsPerDelivery = totalEarnings.divide(
                    BigDecimal.valueOf(deliveriesCompleted), 2, java.math.RoundingMode.HALF_UP);
        }

        if (totalActiveTimeMinutes != null && totalActiveTimeMinutes > 0 && totalEarnings != null) {
            double hours = totalActiveTimeMinutes / 60.0;
            this.earningsPerHour = totalEarnings.divide(
                    BigDecimal.valueOf(hours), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCourierMetrics implements Serializable {
        private LocalDateTime date;
        private Long ordersOffered;
        private Long ordersAccepted;
        private Long deliveriesCompleted;
        private Double avgDeliveryTimeMinutes;
        private Long activeMinutes;
        private Long locationUpdates;
        private BigDecimal earnings;
    }
}

package com.fooddelivery.analytics.operations.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Delivery Success Rate metrics.
 * Tracks percentage of completed deliveries without cancellations, returns, or major delays.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Delivery success rate metrics")
public class DeliverySuccessRateDto implements Serializable {

    @Schema(description = "Analysis period start")
    private LocalDateTime periodStart;

    @Schema(description = "Analysis period end")
    private LocalDateTime periodEnd;

    // Overall success metrics
    @Schema(description = "Total orders in period")
    private Long totalOrders;

    @Schema(description = "Successfully completed deliveries")
    private Long successfulDeliveries;

    @Schema(description = "Overall success rate (%)", example = "94.5")
    private Double successRate;

    // Failure breakdown
    @Schema(description = "Orders cancelled by customer")
    private Long cancelledByCustomer;

    @Schema(description = "Orders cancelled by restaurant")
    private Long cancelledByRestaurant;

    @Schema(description = "Orders cancelled by courier")
    private Long cancelledByCourier;

    @Schema(description = "Orders cancelled by system/platform")
    private Long cancelledBySystem;

    @Schema(description = "Total cancellations")
    private Long totalCancellations;

    @Schema(description = "Cancellation rate (%)")
    private Double cancellationRate;

    // Delay metrics
    @Schema(description = "Orders with major delays (> 15 min late)")
    private Long ordersWithMajorDelays;

    @Schema(description = "Major delay rate (%)")
    private Double majorDelayRate;

    @Schema(description = "Orders delivered on time")
    private Long onTimeDeliveries;

    @Schema(description = "On-time delivery rate (%)")
    private Double onTimeRate;

    // Return/refund metrics
    @Schema(description = "Orders returned/refunded")
    private Long ordersReturned;

    @Schema(description = "Return rate (%)")
    private Double returnRate;

    // Issue breakdown
    @Schema(description = "Delivery issues by type")
    private Map<String, Long> issuesByType;

    @Schema(description = "Cancellation reasons breakdown")
    private Map<String, Long> cancellationReasons;

    // Time-based analysis
    @Schema(description = "Success rate by hour of day")
    private Map<Integer, Double> successRateByHour;

    @Schema(description = "Success rate by day of week (1=Monday, 7=Sunday)")
    private Map<Integer, Double> successRateByDayOfWeek;

    // Segment breakdown
    @Schema(description = "Success rate by restaurant")
    private List<RestaurantSuccessRate> byRestaurant;

    @Schema(description = "Success rate by area/zone")
    private Map<String, Double> byArea;

    // Trends
    @Schema(description = "Daily success metrics")
    private List<DailySuccessMetrics> dailyMetrics;

    /**
     * Calculate derived metrics.
     */
    public void calculateDerivedMetrics() {
        if (totalOrders != null && totalOrders > 0) {
            if (successfulDeliveries != null) {
                this.successRate = (successfulDeliveries * 100.0) / totalOrders;
            }
            if (totalCancellations != null) {
                this.cancellationRate = (totalCancellations * 100.0) / totalOrders;
            }
            if (ordersWithMajorDelays != null) {
                this.majorDelayRate = (ordersWithMajorDelays * 100.0) / totalOrders;
            }
            if (onTimeDeliveries != null) {
                this.onTimeRate = (onTimeDeliveries * 100.0) / totalOrders;
            }
            if (ordersReturned != null) {
                this.returnRate = (ordersReturned * 100.0) / totalOrders;
            }
        }

        // Calculate total cancellations if not set
        if (totalCancellations == null) {
            this.totalCancellations = (cancelledByCustomer != null ? cancelledByCustomer : 0L)
                    + (cancelledByRestaurant != null ? cancelledByRestaurant : 0L)
                    + (cancelledByCourier != null ? cancelledByCourier : 0L)
                    + (cancelledBySystem != null ? cancelledBySystem : 0L);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantSuccessRate implements Serializable {
        private Long restaurantId;
        private String restaurantName;
        private Long totalOrders;
        private Long successfulOrders;
        private Double successRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySuccessMetrics implements Serializable {
        private LocalDateTime date;
        private Long totalOrders;
        private Long successfulOrders;
        private Double successRate;
        private Long cancellations;
        private Long majorDelays;
    }
}

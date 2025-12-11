package com.fooddelivery.analytics.operations.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Restaurant Performance metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Restaurant performance metrics")
public class RestaurantPerformanceDto implements Serializable {

    @Schema(description = "Restaurant ID")
    private Long restaurantId;

    @Schema(description = "Restaurant name")
    private String restaurantName;

    @Schema(description = "Analysis period start")
    private LocalDateTime periodStart;

    @Schema(description = "Analysis period end")
    private LocalDateTime periodEnd;

    // Order acceptance metrics
    @Schema(description = "Total orders received in period")
    private Long totalOrdersReceived;

    @Schema(description = "Orders accepted")
    private Long ordersAccepted;

    @Schema(description = "Orders rejected")
    private Long ordersRejected;

    @Schema(description = "Order acceptance rate (%)", example = "95.5")
    private Double orderAcceptanceRate;

    @Schema(description = "Average time to accept orders (minutes)", example = "2.5")
    private Double averageAcceptanceTimeMinutes;

    @Schema(description = "Median acceptance time (minutes)")
    private Double medianAcceptanceTimeMinutes;

    @Schema(description = "90th percentile acceptance time (minutes)")
    private Double p90AcceptanceTimeMinutes;

    // Preparation metrics
    @Schema(description = "Average preparation time (minutes)", example = "18.3")
    private Double averagePreparationTimeMinutes;

    @Schema(description = "Median preparation time (minutes)")
    private Double medianPreparationTimeMinutes;

    @Schema(description = "90th percentile preparation time (minutes)")
    private Double p90PreparationTimeMinutes;

    @Schema(description = "Orders prepared on time (within estimate)")
    private Long ordersPreparedOnTime;

    @Schema(description = "On-time preparation rate (%)")
    private Double onTimePreparationRate;

    // Online status metrics
    @Schema(description = "Total offline time in period (minutes)")
    private Long totalOfflineTimeMinutes;

    @Schema(description = "Number of times went offline")
    private Integer offlineCount;

    @Schema(description = "Average offline duration (minutes)")
    private Double averageOfflineDurationMinutes;

    @Schema(description = "Uptime percentage during business hours", example = "98.5")
    private Double uptimePercentage;

    @Schema(description = "Last time restaurant went offline")
    private LocalDateTime lastOfflineAt;

    // Menu update metrics
    @Schema(description = "Total menu updates in period")
    private Long totalMenuUpdates;

    @Schema(description = "Menu updates per day average")
    private Double menuUpdatesPerDay;

    @Schema(description = "Menu updates per week average")
    private Double menuUpdatesPerWeek;

    @Schema(description = "Last menu update time")
    private LocalDateTime lastMenuUpdateAt;

    @Schema(description = "Items added in period")
    private Integer itemsAdded;

    @Schema(description = "Items removed in period")
    private Integer itemsRemoved;

    @Schema(description = "Price changes in period")
    private Integer priceChanges;

    // Order completion metrics
    @Schema(description = "Total orders completed (delivered)")
    private Long ordersCompleted;

    @Schema(description = "Orders cancelled by restaurant")
    private Long ordersCancelledByRestaurant;

    @Schema(description = "Completion rate (%)")
    private Double completionRate;

    // Hourly breakdown
    @Schema(description = "Orders by hour of day")
    private Map<Integer, Long> ordersByHour;

    @Schema(description = "Peak hour")
    private Integer peakHour;

    @Schema(description = "Orders during peak hour")
    private Long peakHourOrders;

    // Daily trends
    @Schema(description = "Daily performance breakdown")
    private List<DailyRestaurantMetrics> dailyMetrics;

    /**
     * Calculate derived metrics.
     */
    public void calculateDerivedMetrics() {
        if (totalOrdersReceived != null && totalOrdersReceived > 0) {
            if (ordersAccepted != null) {
                this.orderAcceptanceRate = (ordersAccepted * 100.0) / totalOrdersReceived;
            }
            if (ordersCompleted != null) {
                this.completionRate = (ordersCompleted * 100.0) / totalOrdersReceived;
            }
            if (ordersPreparedOnTime != null) {
                this.onTimePreparationRate = (ordersPreparedOnTime * 100.0) / totalOrdersReceived;
            }
        }

        if (offlineCount != null && offlineCount > 0 && totalOfflineTimeMinutes != null) {
            this.averageOfflineDurationMinutes = totalOfflineTimeMinutes.doubleValue() / offlineCount;
        }

        // Find peak hour
        if (ordersByHour != null && !ordersByHour.isEmpty()) {
            Map.Entry<Integer, Long> peak = ordersByHour.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if (peak != null) {
                this.peakHour = peak.getKey();
                this.peakHourOrders = peak.getValue();
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRestaurantMetrics implements Serializable {
        private LocalDateTime date;
        private Long ordersReceived;
        private Long ordersCompleted;
        private Double avgAcceptanceTimeMinutes;
        private Double avgPreparationTimeMinutes;
        private Long offlineMinutes;
        private Integer menuUpdates;
    }
}

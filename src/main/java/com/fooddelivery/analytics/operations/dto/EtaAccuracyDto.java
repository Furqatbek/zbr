package com.fooddelivery.analytics.operations.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for ETA Accuracy metrics.
 * Tracks difference between predicted and actual delivery times.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "ETA accuracy metrics")
public class EtaAccuracyDto implements Serializable {

    @Schema(description = "Analysis period start")
    private LocalDateTime periodStart;

    @Schema(description = "Analysis period end")
    private LocalDateTime periodEnd;

    // Overall accuracy
    @Schema(description = "Total deliveries analyzed")
    private Long totalDeliveries;

    @Schema(description = "Average ETA error in minutes (positive = late)", example = "2.5")
    private Double averageEtaErrorMinutes;

    @Schema(description = "Average absolute ETA error in minutes", example = "5.3")
    private Double averageAbsoluteErrorMinutes;

    @Schema(description = "Median absolute ETA error in minutes")
    private Double medianAbsoluteErrorMinutes;

    @Schema(description = "90th percentile absolute error (minutes)")
    private Double p90AbsoluteErrorMinutes;

    @Schema(description = "Standard deviation of ETA error")
    private Double etaErrorStdDev;

    // Accuracy buckets
    @Schema(description = "Deliveries within 5 minutes of estimate")
    private Long within5Minutes;

    @Schema(description = "Deliveries within 10 minutes of estimate")
    private Long within10Minutes;

    @Schema(description = "Deliveries within 15 minutes of estimate")
    private Long within15Minutes;

    @Schema(description = "Accuracy rate (within 5 min) %", example = "72.5")
    private Double accuracyRate5Min;

    @Schema(description = "Accuracy rate (within 10 min) %")
    private Double accuracyRate10Min;

    @Schema(description = "Accuracy rate (within 15 min) %")
    private Double accuracyRate15Min;

    // Over/Under estimation
    @Schema(description = "Count of over-estimations (delivered early)")
    private Long overEstimationCount;

    @Schema(description = "Count of under-estimations (delivered late)")
    private Long underEstimationCount;

    @Schema(description = "Average over-estimation (minutes early)")
    private Double averageOverEstimationMinutes;

    @Schema(description = "Average under-estimation (minutes late)")
    private Double averageUnderEstimationMinutes;

    @Schema(description = "Over-estimation rate (%)")
    private Double overEstimationRate;

    @Schema(description = "Under-estimation rate (%)")
    private Double underEstimationRate;

    // Breakdown by phase
    @Schema(description = "Preparation time ETA accuracy")
    private PhaseAccuracy preparationAccuracy;

    @Schema(description = "Pickup time ETA accuracy")
    private PhaseAccuracy pickupAccuracy;

    @Schema(description = "Delivery/travel time ETA accuracy")
    private PhaseAccuracy deliveryAccuracy;

    // Time-based patterns
    @Schema(description = "Average error by hour of day (minutes)")
    private Map<Integer, Double> errorByHour;

    @Schema(description = "Average error by day of week (1=Monday)")
    private Map<Integer, Double> errorByDayOfWeek;

    @Schema(description = "Hour with worst accuracy")
    private Integer worstAccuracyHour;

    @Schema(description = "Hour with best accuracy")
    private Integer bestAccuracyHour;

    // Segment breakdown
    @Schema(description = "ETA accuracy by restaurant")
    private List<RestaurantEtaAccuracy> byRestaurant;

    @Schema(description = "ETA accuracy by distance bucket")
    private Map<String, Double> byDistanceBucket;

    // Trends
    @Schema(description = "Daily ETA metrics")
    private List<DailyEtaMetrics> dailyMetrics;

    /**
     * Calculate derived metrics.
     */
    public void calculateDerivedMetrics() {
        if (totalDeliveries != null && totalDeliveries > 0) {
            if (within5Minutes != null) {
                this.accuracyRate5Min = (within5Minutes * 100.0) / totalDeliveries;
            }
            if (within10Minutes != null) {
                this.accuracyRate10Min = (within10Minutes * 100.0) / totalDeliveries;
            }
            if (within15Minutes != null) {
                this.accuracyRate15Min = (within15Minutes * 100.0) / totalDeliveries;
            }
            if (overEstimationCount != null) {
                this.overEstimationRate = (overEstimationCount * 100.0) / totalDeliveries;
            }
            if (underEstimationCount != null) {
                this.underEstimationRate = (underEstimationCount * 100.0) / totalDeliveries;
            }
        }

        // Find best/worst hours
        if (errorByHour != null && !errorByHour.isEmpty()) {
            this.worstAccuracyHour = errorByHour.entrySet().stream()
                    .max(Map.Entry.comparingByValue(java.util.Comparator.comparingDouble(Math::abs)))
                    .map(Map.Entry::getKey)
                    .orElse(null);

            this.bestAccuracyHour = errorByHour.entrySet().stream()
                    .min(Map.Entry.comparingByValue(java.util.Comparator.comparingDouble(Math::abs)))
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhaseAccuracy implements Serializable {
        @Schema(description = "Average estimated time (minutes)")
        private Double averageEstimatedMinutes;

        @Schema(description = "Average actual time (minutes)")
        private Double averageActualMinutes;

        @Schema(description = "Average error (minutes)")
        private Double averageErrorMinutes;

        @Schema(description = "Accuracy rate within 5 min (%)")
        private Double accuracyRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantEtaAccuracy implements Serializable {
        private Long restaurantId;
        private String restaurantName;
        private Long totalDeliveries;
        private Double averageErrorMinutes;
        private Double accuracyRate5Min;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyEtaMetrics implements Serializable {
        private LocalDateTime date;
        private Long totalDeliveries;
        private Double avgErrorMinutes;
        private Double avgAbsErrorMinutes;
        private Long within5Min;
        private Long lateDeliveries;
        private Long earlyDeliveries;
    }
}

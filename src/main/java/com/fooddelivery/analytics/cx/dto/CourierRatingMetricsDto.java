package com.fooddelivery.analytics.cx.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for courier rating metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierRatingMetricsDto {

    // Core metrics
    private Double averageRating;
    private Long ratingCount;
    private Long reviewCount; // Ratings with comments

    // Sub-ratings
    private Double averageProfessionalismRating;
    private Double averageCommunicationRating;
    private Double averageTimelinessRating;

    // Distribution (1-5 stars)
    private Map<Integer, Long> distribution;
    private Map<Integer, Double> distributionPercentage;

    // Trend data
    private List<RatingTrendDto> ratingTrend;

    // Courier-specific (when querying single courier)
    private Long courierId;
    private String courierName;

    // Aggregated per courier (when no specific courier)
    private List<CourierRatingSummaryDto> topRatedCouriers;
    private List<CourierRatingSummaryDto> lowestRatedCouriers;
    private List<CourierRatingSummaryDto> mostActiveRatedCouriers;

    // Tip metrics
    private Double averageTipAmount;
    private Long deliveriesWithTips;
    private Double tipRate;
    private Double totalTipsReceived;

    // Response rate
    private Long totalDeliveries;
    private Double ratingSubmissionRate;

    // Period info
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Comparison
    private Double previousPeriodAverage;
    private Double ratingChange;

    /**
     * Rating trend data point.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingTrendDto {
        private LocalDateTime date;
        private Double averageRating;
        private Long ratingCount;
        private Map<Integer, Long> distribution;
    }

    /**
     * Courier rating summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourierRatingSummaryDto {
        private Long courierId;
        private String courierName;
        private Double averageRating;
        private Double avgRating;
        private Long ratingCount;
        private Long totalRatings;
        private Long deliveryCount;
        private Double tipRate;
        private Double ratingChange;
    }
}

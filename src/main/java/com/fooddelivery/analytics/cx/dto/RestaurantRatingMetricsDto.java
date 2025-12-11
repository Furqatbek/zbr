package com.fooddelivery.analytics.cx.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for restaurant rating metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRatingMetricsDto {

    // Core metrics
    private Double averageRating;
    private Long ratingCount;
    private Long reviewCount; // Ratings with comments

    // Sub-ratings
    private Double averageFoodQualityRating;
    private Double averagePortionSizeRating;
    private Double averageValueForMoneyRating;

    // Distribution (1-5 stars)
    private Map<Integer, Long> distribution;
    private Map<Integer, Double> distributionPercentage;

    // Trend data
    private List<RatingTrendDto> ratingTrend;

    // Restaurant-specific (when aggregating across restaurants)
    private Long restaurantId;
    private String restaurantName;

    // Aggregated per restaurant (when no specific restaurant)
    private List<RestaurantRatingSummaryDto> topRatedRestaurants;
    private List<RestaurantRatingSummaryDto> lowestRatedRestaurants;
    private List<RestaurantRatingSummaryDto> mostReviewedRestaurants;

    // Response rate
    private Long totalOrders;
    private Double ratingSubmissionRate;

    // Response metrics
    private Long responsesWithComments;
    private Double commentRate;
    private Long restaurantResponseCount;
    private Double restaurantResponseRate;

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
     * Restaurant rating summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantRatingSummaryDto {
        private Long restaurantId;
        private String restaurantName;
        private Double averageRating;
        private Long ratingCount;
        private Long reviewCount;
        private Double ratingChange; // Compared to previous period
    }

    /**
     * Category rating breakdown.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRatingDto {
        private String category;
        private Double averageRating;
        private Long ratingCount;
    }
}

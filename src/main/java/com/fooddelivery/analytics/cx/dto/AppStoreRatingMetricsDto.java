package com.fooddelivery.analytics.cx.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for app store rating metrics (iOS + Android).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppStoreRatingMetricsDto {

    // Overall metrics
    private Double overallAverageRating;
    private Long totalReviewCount;

    // Platform breakdown
    private PlatformRatingDto iosRatings;
    private PlatformRatingDto androidRatings;

    // Alias getters for mapper compatibility
    private PlatformRatingDto iosPlatform;
    private PlatformRatingDto androidPlatform;

    // Distribution (1-5 stars) - combined
    private Map<Integer, Long> distribution;
    private Map<Integer, Double> distributionPercentage;

    // Trend data
    private List<RatingTrendDto> ratingTrend;

    // Version breakdown
    private List<VersionRatingDto> ratingsByVersion;

    // Country breakdown
    private List<CountryRatingDto> ratingsByCountry;

    // Sentiment analysis
    private SentimentSummaryDto sentimentSummary;

    // Common themes from reviews
    private List<ReviewThemeDto> positiveThemes;
    private List<ReviewThemeDto> negativeThemes;

    // Response metrics
    private Long reviewsWithResponse;
    private Double responseRate;
    private Double averageResponseTimeHours;

    // Period info
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Comparison
    private Double previousPeriodAverage;
    private Double ratingChange;
    private Long previousPeriodReviewCount;
    private Double reviewCountChange;

    /**
     * Platform-specific ratings.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformRatingDto {
        private String platform;
        private Double averageRating;
        private Long reviewCount;
        private Map<Integer, Long> distribution;
        private String latestVersion;
        private Double latestVersionRating;
    }

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
        private Long reviewCount;
        private Double iosRating;
        private Double androidRating;
    }

    /**
     * Version-specific ratings.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionRatingDto {
        private String version;
        private String platform;
        private Double averageRating;
        private Long reviewCount;
        private LocalDateTime releaseDate;
    }

    /**
     * Country-specific ratings.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryRatingDto {
        private String country;
        private String countryName;
        private Double averageRating;
        private Long reviewCount;
    }

    /**
     * Sentiment analysis summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentSummaryDto {
        private Long positiveCount;
        private Long neutralCount;
        private Long negativeCount;
        private Double positivePercentage;
        private Double neutralPercentage;
        private Double negativePercentage;
        private Double averageSentimentScore; // -1 to 1
    }

    /**
     * Review theme/topic.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewThemeDto {
        private String theme;
        private Long mentionCount;
        private Double averageRating;
        private String sentiment;
        private List<String> sampleReviews;
    }
}

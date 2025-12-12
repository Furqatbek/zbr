package com.fooddelivery.analytics.cx.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Summary DTO for overall customer experience metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CxSummaryDto {

    // NPS Summary
    private NpsSummary nps;
    private NpsSummary npsSummary;

    // Ratings Summary
    private RatingsSummary ratings;
    private RatingsSummary ratingsSummary;

    // Support Summary
    private SupportSummary support;
    private SupportSummary supportSummary;

    // Overall CX Score (composite)
    private Double cxScore; // 0-100
    private Double overallCxScore;
    private String cxStatus; // EXCELLENT, GOOD, FAIR, POOR

    // Period info
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime generatedAt;

    // Trends
    private Double cxScoreChange;
    private String cxTrend; // IMPROVING, STABLE, DECLINING

    /**
     * NPS summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NpsSummary {
        private Double npsScore;
        private Long totalResponses;
        private Double promotersPercentage;
        private Double detractorsPercentage;
        private Double npsChange;
        private String trend;
    }

    /**
     * Ratings summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingsSummary {
        private Double avgRestaurantRating;
        private Double restaurantAverageRating;
        private Long restaurantRatingCount;
        private Double avgCourierRating;
        private Double courierAverageRating;
        private Long courierRatingCount;
        private Double avgAppStoreRating;
        private Double overallAverageRating;
        private Double appStoreIosRating;
        private Double appStoreAndroidRating;
        private Long appStoreReviewCount;
        private Double overallRatingChange;
    }

    /**
     * Support summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportSummary {
        private Long totalTickets;
        private Long openTickets;
        private Double avgResolutionTimeHours;
        private Double slaComplianceRate;
        private Long slaBreaches;
        private Double csatScore;
        private Double ticketVolumeChange;
    }
}

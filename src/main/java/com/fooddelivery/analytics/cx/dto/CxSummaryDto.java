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

    // Ratings Summary
    private RatingsSummary ratings;

    // Support Summary
    private SupportSummary support;

    // Overall CX Score (composite)
    private Double cxScore; // 0-100
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
        private Long restaurantRatingCount;
        private Double avgCourierRating;
        private Long courierRatingCount;
        private Double avgAppStoreRating;
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
        private Double csatScore;
        private Double ticketVolumeChange;
    }
}

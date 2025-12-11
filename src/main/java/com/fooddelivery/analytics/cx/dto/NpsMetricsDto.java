package com.fooddelivery.analytics.cx.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Net Promoter Score (NPS) metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NpsMetricsDto {

    // Core NPS metrics
    private Double npsScore; // -100 to +100
    private Long totalResponses;
    private Long promotersCount;
    private Long passivesCount;
    private Long detractorsCount;

    // Percentages
    private Double promotersPercentage;
    private Double passivesPercentage;
    private Double detractorsPercentage;

    // Response rate
    private Long totalSurveysSent;
    private Double responseRate;

    // Score distribution (0-10)
    private Map<Integer, Long> scoreDistribution;

    // Breakdown by segment
    private Map<String, SegmentNpsDto> npsBySegment;

    // Breakdown by channel
    private Map<String, SegmentNpsDto> npsByChannel;

    // Trend over time
    private List<NpsTrendDto> npsTrend;

    // Common feedback themes from comments
    private List<FeedbackThemeDto> feedbackThemes;

    // Period info
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Comparison
    private Double previousPeriodNps;
    private Double npsChange;

    /**
     * Segment NPS breakdown DTO.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentNpsDto {
        private String segment;
        private Double npsScore;
        private Long responseCount;
        private Long promoters;
        private Long passives;
        private Long detractors;
    }

    /**
     * NPS trend data point.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NpsTrendDto {
        private LocalDateTime date;
        private Double npsScore;
        private Long responseCount;
        private Long promoters;
        private Long passives;
        private Long detractors;
    }

    /**
     * Feedback theme from NPS comments.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackThemeDto {
        private String theme;
        private Long mentionCount;
        private Double averageScore;
        private String sentiment; // POSITIVE, NEGATIVE, NEUTRAL
        private List<String> sampleComments;
    }
}

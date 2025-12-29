package com.fooddelivery.analytics.cx.mapper;

import com.fooddelivery.analytics.cx.dto.*;
import com.fooddelivery.analytics.cx.model.*;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for Customer Experience analytics entities.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CxMapper {

    // ==================== NPS Survey Mappings ====================

    /**
     * Map NpsSurvey entity to a summary DTO.
     */
    @Mapping(target = "category", expression = "java(getNpsCategory(survey))")
    NpsSurveyResponseDto toNpsSurveyResponse(NpsSurvey survey);

    List<NpsSurveyResponseDto> toNpsSurveyResponses(List<NpsSurvey> surveys);

    default String getNpsCategory(NpsSurvey survey) {
        if (survey == null) return null;
        if (survey.isPromoter()) return "PROMOTER";
        if (survey.isPassive()) return "PASSIVE";
        if (survey.isDetractor()) return "DETRACTOR";
        return "UNKNOWN";
    }

    // ==================== Restaurant Rating Mappings ====================

    /**
     * Map RestaurantRating entity to response DTO.
     */
    RestaurantRatingResponseDto toRestaurantRatingResponse(RestaurantRating rating);

    List<RestaurantRatingResponseDto> toRestaurantRatingResponses(List<RestaurantRating> ratings);

    /**
     * Map restaurant rating summary data.
     * Property names match RestaurantRatingSummaryDto fields.
     */
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "averageRating", source = "averageRating")
    @Mapping(target = "ratingCount", source = "ratingCount")
    RestaurantRatingMetricsDto.RestaurantRatingSummaryDto toRestaurantSummary(
            Long restaurantId, Double averageRating, Long ratingCount);

    // ==================== Courier Rating Mappings ====================

    /**
     * Map CourierRating entity to response DTO.
     */
    CourierRatingResponseDto toCourierRatingResponse(CourierRating rating);

    List<CourierRatingResponseDto> toCourierRatingResponses(List<CourierRating> ratings);

    /**
     * Map courier rating summary data.
     * Property names match CourierRatingSummaryDto fields.
     */
    @Mapping(target = "courierId", source = "courierId")
    @Mapping(target = "averageRating", source = "averageRating")
    @Mapping(target = "ratingCount", source = "ratingCount")
    CourierRatingMetricsDto.CourierRatingSummaryDto toCourierSummary(
            Long courierId, Double averageRating, Long ratingCount);

    // ==================== App Store Rating Mappings ====================

    /**
     * Map AppStoreRating entity to response DTO.
     */
    @Mapping(target = "platformName", expression = "java(rating.getPlatform() != null ? rating.getPlatform().name() : null)")
    AppStoreRatingResponseDto toAppStoreRatingResponse(AppStoreRating rating);

    List<AppStoreRatingResponseDto> toAppStoreRatingResponses(List<AppStoreRating> ratings);

    // ==================== Support Ticket Mappings ====================

    /**
     * Map SupportTicket entity to response DTO.
     */
    @Mapping(target = "typeName", expression = "java(ticket.getType() != null ? ticket.getType().name() : null)")
    @Mapping(target = "statusName", expression = "java(ticket.getStatus() != null ? ticket.getStatus().name() : null)")
    @Mapping(target = "priorityName", expression = "java(ticket.getPriority() != null ? ticket.getPriority().name() : null)")
    @Mapping(target = "channelName", expression = "java(ticket.getChannel() != null ? ticket.getChannel().name() : null)")
    @Mapping(target = "resolutionTimeHours", expression = "java(ticket.getResolutionTimeHours())")
    @Mapping(target = "firstResponseTimeMinutes", expression = "java(ticket.getFirstResponseTimeMinutes())")
    @Mapping(target = "isOpen", expression = "java(ticket.isOpen())")
    SupportTicketResponseDto toSupportTicketResponse(SupportTicket ticket);

    List<SupportTicketResponseDto> toSupportTicketResponses(List<SupportTicket> tickets);

    // ==================== CX Summary Mappings ====================

    /**
     * Create NPS summary from metrics.
     */
    default CxSummaryDto.NpsSummary toNpsSummary(NpsMetricsDto npsMetrics) {
        if (npsMetrics == null) return null;
        return CxSummaryDto.NpsSummary.builder()
                .npsScore(npsMetrics.getNpsScore())
                .totalResponses(npsMetrics.getTotalResponses())
                .promotersPercentage(npsMetrics.getPromotersPercentage())
                .detractorsPercentage(npsMetrics.getDetractorsPercentage())
                .trend(calculateTrend(npsMetrics.getNpsScore(), null))
                .build();
    }

    /**
     * Create ratings summary from metrics.
     */
    default CxSummaryDto.RatingsSummary toRatingsSummary(
            RestaurantRatingMetricsDto restaurantMetrics,
            CourierRatingMetricsDto courierMetrics,
            AppStoreRatingMetricsDto appStoreMetrics) {

        Double restaurantRating = restaurantMetrics != null ? restaurantMetrics.getAverageRating() : null;
        Double courierRating = courierMetrics != null ? courierMetrics.getAverageRating() : null;

        Double iosRating = null;
        Double androidRating = null;
        if (appStoreMetrics != null) {
            if (appStoreMetrics.getIosPlatform() != null) {
                iosRating = appStoreMetrics.getIosPlatform().getAverageRating();
            }
            if (appStoreMetrics.getAndroidPlatform() != null) {
                androidRating = appStoreMetrics.getAndroidPlatform().getAverageRating();
            }
        }

        // Calculate overall average
        double sum = 0.0;
        int count = 0;
        if (restaurantRating != null) { sum += restaurantRating; count++; }
        if (courierRating != null) { sum += courierRating; count++; }
        if (iosRating != null) { sum += iosRating; count++; }
        if (androidRating != null) { sum += androidRating; count++; }
        Double overallAverage = count > 0 ? sum / count : null;

        return CxSummaryDto.RatingsSummary.builder()
                .overallAverageRating(overallAverage)
                .restaurantAverageRating(restaurantRating)
                .courierAverageRating(courierRating)
                .appStoreIosRating(iosRating)
                .appStoreAndroidRating(androidRating)
                .build();
    }

    /**
     * Create support summary from metrics.
     */
    default CxSummaryDto.SupportSummary toSupportSummary(SupportTicketMetricsDto ticketMetrics) {
        if (ticketMetrics == null) return null;

        Double slaComplianceRate = null;
        Long slaBreaches = null;
        if (ticketMetrics.getSlaMetrics() != null) {
            slaComplianceRate = ticketMetrics.getSlaMetrics().getSlaComplianceRate();
            slaBreaches = ticketMetrics.getSlaMetrics().getTicketsBreachedSla();
        }

        Double avgResolutionTime = null;
        if (ticketMetrics.getPerformance() != null) {
            avgResolutionTime = ticketMetrics.getPerformance().getAvgResolutionTimeHours();
        }

        Double csatScore = null;
        if (ticketMetrics.getCsatMetrics() != null) {
            csatScore = ticketMetrics.getCsatMetrics().getAvgCsatScore();
        }

        return CxSummaryDto.SupportSummary.builder()
                .totalTickets(ticketMetrics.getTotalTickets())
                .openTickets(ticketMetrics.getOpenTickets())
                .avgResolutionTimeHours(avgResolutionTime)
                .slaComplianceRate(slaComplianceRate)
                .slaBreaches(slaBreaches)
                .csatScore(csatScore)
                .build();
    }

    /**
     * Calculate trend indicator.
     */
    default String calculateTrend(Double current, Double previous) {
        if (current == null || previous == null) return "STABLE";
        double diff = current - previous;
        if (diff > 5) return "UP";
        if (diff < -5) return "DOWN";
        return "STABLE";
    }

    /**
     * Calculate overall CX score (0-100).
     * Weighted average: NPS (30%), Ratings (40%), Support (30%)
     */
    default Double calculateOverallCxScore(
            CxSummaryDto.NpsSummary nps,
            CxSummaryDto.RatingsSummary ratings,
            CxSummaryDto.SupportSummary support) {

        double score = 0.0;
        double weightSum = 0.0;

        // NPS contribution (normalized to 0-100 from -100 to 100)
        if (nps != null && nps.getNpsScore() != null) {
            double normalizedNps = (nps.getNpsScore() + 100) / 2; // Convert -100..100 to 0..100
            score += normalizedNps * 0.3;
            weightSum += 0.3;
        }

        // Ratings contribution (normalized to 0-100 from 1-5 scale)
        if (ratings != null && ratings.getOverallAverageRating() != null) {
            double normalizedRating = (ratings.getOverallAverageRating() - 1) * 25; // Convert 1-5 to 0-100
            score += normalizedRating * 0.4;
            weightSum += 0.4;
        }

        // Support contribution (based on SLA compliance and CSAT)
        if (support != null) {
            double supportScore = 0.0;
            int supportFactors = 0;

            if (support.getSlaComplianceRate() != null) {
                supportScore += support.getSlaComplianceRate();
                supportFactors++;
            }
            if (support.getCsatScore() != null) {
                // Normalize CSAT (1-5) to 0-100
                supportScore += (support.getCsatScore() - 1) * 25;
                supportFactors++;
            }

            if (supportFactors > 0) {
                score += (supportScore / supportFactors) * 0.3;
                weightSum += 0.3;
            }
        }

        return weightSum > 0 ? score / weightSum : null;
    }

    /**
     * Determine CX status based on score.
     */
    default String determineCxStatus(Double cxScore) {
        if (cxScore == null) return "UNKNOWN";
        if (cxScore >= 80) return "EXCELLENT";
        if (cxScore >= 60) return "GOOD";
        if (cxScore >= 40) return "FAIR";
        if (cxScore >= 20) return "POOR";
        return "CRITICAL";
    }
}

package com.fooddelivery.analytics.cx.collector;

import com.fooddelivery.analytics.cx.dto.AppStoreRatingMetricsDto;
import com.fooddelivery.analytics.cx.dto.CourierRatingMetricsDto;
import com.fooddelivery.analytics.cx.dto.RestaurantRatingMetricsDto;
import com.fooddelivery.analytics.cx.model.AppStoreRating;
import com.fooddelivery.analytics.cx.repository.AppStoreRatingRepository;
import com.fooddelivery.analytics.cx.repository.CourierRatingRepository;
import com.fooddelivery.analytics.cx.repository.RestaurantRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for rating metrics (restaurant, courier, app store).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RatingCollector {

    private final RestaurantRatingRepository restaurantRatingRepository;
    private final CourierRatingRepository courierRatingRepository;
    private final AppStoreRatingRepository appStoreRatingRepository;

    // Minimum ratings threshold for top/lowest lists
    private static final long MIN_RATINGS_THRESHOLD = 5;

    private static final int DEFAULT_TOP_N = 10;

    /**
     * Collect restaurant rating metrics.
     */
    public RestaurantRatingMetricsDto collectRestaurantRatings(Long restaurantId, LocalDateTime startDate,
                                                               LocalDateTime endDate, boolean includeDistribution,
                                                               boolean includeTrend, boolean includeTopPerformers) {
        int topN = DEFAULT_TOP_N;
        log.debug("Collecting restaurant rating metrics for restaurantId={} from {} to {}",
                restaurantId, startDate, endDate);

        RestaurantRatingMetricsDto.RestaurantRatingMetricsDtoBuilder builder = RestaurantRatingMetricsDto.builder()
                .restaurantId(restaurantId)
                .periodStart(startDate)
                .periodEnd(endDate);

        if (restaurantId != null) {
            // Single restaurant metrics
            Double avgRating = restaurantRatingRepository.getAverageRating(restaurantId, startDate, endDate);
            Long ratingCount = restaurantRatingRepository.countRatings(restaurantId, startDate, endDate);
            Long reviewCount = restaurantRatingRepository.countRatingsWithComments(restaurantId, startDate, endDate);

            builder.averageRating(avgRating)
                    .ratingCount(ratingCount)
                    .reviewCount(reviewCount);

            // Sub-ratings
            Object[] subRatings = restaurantRatingRepository.getSubRatings(restaurantId, startDate, endDate);
            if (subRatings != null && subRatings.length >= 3) {
                builder.averageFoodQualityRating(subRatings[0] != null ? ((Number) subRatings[0]).doubleValue() : null)
                        .averagePortionSizeRating(subRatings[1] != null ? ((Number) subRatings[1]).doubleValue() : null)
                        .averageValueForMoneyRating(subRatings[2] != null ? ((Number) subRatings[2]).doubleValue() : null);
            }

            // Distribution
            Map<Integer, Long> distribution = getRatingDistribution(
                    restaurantRatingRepository.getRatingDistribution(restaurantId, startDate, endDate));
            builder.distribution(distribution);
            builder.distributionPercentage(calculateDistributionPercentage(distribution, ratingCount));

            // Restaurant response metrics
            Long responseCount = restaurantRatingRepository.countRestaurantResponses(restaurantId, startDate, endDate);
            if (reviewCount != null && reviewCount > 0) {
                builder.restaurantResponseCount(responseCount)
                        .restaurantResponseRate(responseCount * 100.0 / reviewCount);
            }

            // Trend
            if (includeTrend) {
                List<RestaurantRatingMetricsDto.RatingTrendDto> trend = restaurantRatingRepository
                        .getDailyRatingTrend(restaurantId, startDate, endDate).stream()
                        .map(row -> RestaurantRatingMetricsDto.RatingTrendDto.builder()
                                .date(((java.sql.Date) row[0]).toLocalDate().atStartOfDay())
                                .averageRating(row[1] != null ? ((Number) row[1]).doubleValue() : null)
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.ratingTrend(trend);
            }
        } else {
            // Overall metrics
            Double avgRating = restaurantRatingRepository.getOverallAverageRating(startDate, endDate);
            Long ratingCount = restaurantRatingRepository.countTotalRatings(startDate, endDate);
            Long reviewCount = restaurantRatingRepository.countTotalRatingsWithComments(startDate, endDate);

            builder.averageRating(avgRating)
                    .ratingCount(ratingCount)
                    .reviewCount(reviewCount);

            // Overall sub-ratings
            Object[] subRatings = restaurantRatingRepository.getOverallSubRatings(startDate, endDate);
            if (subRatings != null && subRatings.length >= 3) {
                builder.averageFoodQualityRating(subRatings[0] != null ? ((Number) subRatings[0]).doubleValue() : null)
                        .averagePortionSizeRating(subRatings[1] != null ? ((Number) subRatings[1]).doubleValue() : null)
                        .averageValueForMoneyRating(subRatings[2] != null ? ((Number) subRatings[2]).doubleValue() : null);
            }

            // Overall distribution
            Map<Integer, Long> distribution = getRatingDistribution(
                    restaurantRatingRepository.getOverallRatingDistribution(startDate, endDate));
            builder.distribution(distribution);
            builder.distributionPercentage(calculateDistributionPercentage(distribution, ratingCount));

            // Top/lowest performers
            if (includeTopPerformers) {
                List<RestaurantRatingMetricsDto.RestaurantRatingSummaryDto> topRated = restaurantRatingRepository
                        .getTopRatedRestaurants(startDate, endDate, MIN_RATINGS_THRESHOLD).stream()
                        .limit(topN)
                        .map(row -> RestaurantRatingMetricsDto.RestaurantRatingSummaryDto.builder()
                                .restaurantId(((Number) row[0]).longValue())
                                .averageRating(((Number) row[1]).doubleValue())
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.topRatedRestaurants(topRated);

                List<RestaurantRatingMetricsDto.RestaurantRatingSummaryDto> lowestRated = restaurantRatingRepository
                        .getLowestRatedRestaurants(startDate, endDate, MIN_RATINGS_THRESHOLD).stream()
                        .limit(topN)
                        .map(row -> RestaurantRatingMetricsDto.RestaurantRatingSummaryDto.builder()
                                .restaurantId(((Number) row[0]).longValue())
                                .averageRating(((Number) row[1]).doubleValue())
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.lowestRatedRestaurants(lowestRated);

                List<RestaurantRatingMetricsDto.RestaurantRatingSummaryDto> mostReviewed = restaurantRatingRepository
                        .getMostReviewedRestaurants(startDate, endDate).stream()
                        .limit(topN)
                        .map(row -> RestaurantRatingMetricsDto.RestaurantRatingSummaryDto.builder()
                                .restaurantId(((Number) row[0]).longValue())
                                .averageRating(((Number) row[1]).doubleValue())
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.mostReviewedRestaurants(mostReviewed);
            }

            // Overall trend
            if (includeTrend) {
                List<RestaurantRatingMetricsDto.RatingTrendDto> trend = restaurantRatingRepository
                        .getOverallDailyRatingTrend(startDate, endDate).stream()
                        .map(row -> RestaurantRatingMetricsDto.RatingTrendDto.builder()
                                .date(((java.sql.Date) row[0]).toLocalDate().atStartOfDay())
                                .averageRating(row[1] != null ? ((Number) row[1]).doubleValue() : null)
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.ratingTrend(trend);
            }
        }

        return builder.build();
    }

    /**
     * Collect courier rating metrics.
     */
    public CourierRatingMetricsDto collectCourierRatings(Long courierId, LocalDateTime startDate,
                                                         LocalDateTime endDate, boolean includeDistribution,
                                                         boolean includeTrend, boolean includeTopPerformers) {
        int topN = DEFAULT_TOP_N;
        log.debug("Collecting courier rating metrics for courierId={} from {} to {}", courierId, startDate, endDate);

        CourierRatingMetricsDto.CourierRatingMetricsDtoBuilder builder = CourierRatingMetricsDto.builder()
                .courierId(courierId)
                .periodStart(startDate)
                .periodEnd(endDate);

        if (courierId != null) {
            // Single courier metrics
            Double avgRating = courierRatingRepository.getAverageRating(courierId, startDate, endDate);
            Long ratingCount = courierRatingRepository.countRatings(courierId, startDate, endDate);
            Long reviewCount = courierRatingRepository.countRatingsWithComments(courierId, startDate, endDate);

            builder.averageRating(avgRating)
                    .ratingCount(ratingCount)
                    .reviewCount(reviewCount);

            // Sub-ratings
            Object[] subRatings = courierRatingRepository.getSubRatings(courierId, startDate, endDate);
            if (subRatings != null && subRatings.length >= 3) {
                builder.averageProfessionalismRating(subRatings[0] != null ? ((Number) subRatings[0]).doubleValue() : null)
                        .averageCommunicationRating(subRatings[1] != null ? ((Number) subRatings[1]).doubleValue() : null)
                        .averageTimelinessRating(subRatings[2] != null ? ((Number) subRatings[2]).doubleValue() : null);
            }

            // Distribution
            Map<Integer, Long> distribution = getRatingDistribution(
                    courierRatingRepository.getRatingDistribution(courierId, startDate, endDate));
            builder.distribution(distribution);
            builder.distributionPercentage(calculateDistributionPercentage(distribution, ratingCount));

            // Tip stats
            Object[] tipStats = courierRatingRepository.getTipStats(courierId, startDate, endDate);
            if (tipStats != null && tipStats.length >= 4) {
                Long totalDeliveries = tipStats[0] != null ? ((Number) tipStats[0]).longValue() : 0L;
                Double totalTips = tipStats[1] != null ? ((Number) tipStats[1]).doubleValue() : 0.0;
                Double avgTip = tipStats[2] != null ? ((Number) tipStats[2]).doubleValue() : 0.0;
                Long withTips = tipStats[3] != null ? ((Number) tipStats[3]).longValue() : 0L;

                builder.totalDeliveries(totalDeliveries)
                        .totalTipsReceived(totalTips)
                        .averageTipAmount(avgTip)
                        .deliveriesWithTips(withTips)
                        .tipRate(totalDeliveries > 0 ? withTips * 100.0 / totalDeliveries : 0.0);
            }

            // Trend
            if (includeTrend) {
                List<CourierRatingMetricsDto.RatingTrendDto> trend = courierRatingRepository
                        .getDailyRatingTrend(courierId, startDate, endDate).stream()
                        .map(row -> CourierRatingMetricsDto.RatingTrendDto.builder()
                                .date(((java.sql.Date) row[0]).toLocalDate().atStartOfDay())
                                .averageRating(row[1] != null ? ((Number) row[1]).doubleValue() : null)
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.ratingTrend(trend);
            }
        } else {
            // Overall courier metrics
            Double avgRating = courierRatingRepository.getOverallAverageRating(startDate, endDate);
            Long ratingCount = courierRatingRepository.countTotalRatings(startDate, endDate);
            Long reviewCount = courierRatingRepository.countTotalRatingsWithComments(startDate, endDate);

            builder.averageRating(avgRating)
                    .ratingCount(ratingCount)
                    .reviewCount(reviewCount);

            // Overall sub-ratings
            Object[] subRatings = courierRatingRepository.getOverallSubRatings(startDate, endDate);
            if (subRatings != null && subRatings.length >= 3) {
                builder.averageProfessionalismRating(subRatings[0] != null ? ((Number) subRatings[0]).doubleValue() : null)
                        .averageCommunicationRating(subRatings[1] != null ? ((Number) subRatings[1]).doubleValue() : null)
                        .averageTimelinessRating(subRatings[2] != null ? ((Number) subRatings[2]).doubleValue() : null);
            }

            // Overall distribution
            Map<Integer, Long> distribution = getRatingDistribution(
                    courierRatingRepository.getOverallRatingDistribution(startDate, endDate));
            builder.distribution(distribution);
            builder.distributionPercentage(calculateDistributionPercentage(distribution, ratingCount));

            // Overall tip stats
            Object[] tipStats = courierRatingRepository.getOverallTipStats(startDate, endDate);
            if (tipStats != null && tipStats.length >= 4) {
                Long totalDeliveries = tipStats[0] != null ? ((Number) tipStats[0]).longValue() : 0L;
                Double totalTips = tipStats[1] != null ? ((Number) tipStats[1]).doubleValue() : 0.0;
                Double avgTip = tipStats[2] != null ? ((Number) tipStats[2]).doubleValue() : 0.0;
                Long withTips = tipStats[3] != null ? ((Number) tipStats[3]).longValue() : 0L;

                builder.totalDeliveries(totalDeliveries)
                        .totalTipsReceived(totalTips)
                        .averageTipAmount(avgTip)
                        .deliveriesWithTips(withTips)
                        .tipRate(totalDeliveries > 0 ? withTips * 100.0 / totalDeliveries : 0.0);
            }

            // Top/lowest performers
            if (includeTopPerformers) {
                List<CourierRatingMetricsDto.CourierRatingSummaryDto> topRated = courierRatingRepository
                        .getTopRatedCouriers(startDate, endDate, MIN_RATINGS_THRESHOLD).stream()
                        .limit(topN)
                        .map(row -> CourierRatingMetricsDto.CourierRatingSummaryDto.builder()
                                .courierId(((Number) row[0]).longValue())
                                .averageRating(((Number) row[1]).doubleValue())
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.topRatedCouriers(topRated);

                List<CourierRatingMetricsDto.CourierRatingSummaryDto> lowestRated = courierRatingRepository
                        .getLowestRatedCouriers(startDate, endDate, MIN_RATINGS_THRESHOLD).stream()
                        .limit(topN)
                        .map(row -> CourierRatingMetricsDto.CourierRatingSummaryDto.builder()
                                .courierId(((Number) row[0]).longValue())
                                .averageRating(((Number) row[1]).doubleValue())
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.lowestRatedCouriers(lowestRated);

                List<CourierRatingMetricsDto.CourierRatingSummaryDto> mostActive = courierRatingRepository
                        .getMostActiveRatedCouriers(startDate, endDate).stream()
                        .limit(topN)
                        .map(row -> CourierRatingMetricsDto.CourierRatingSummaryDto.builder()
                                .courierId(((Number) row[0]).longValue())
                                .averageRating(((Number) row[1]).doubleValue())
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.mostActiveRatedCouriers(mostActive);
            }

            // Overall trend
            if (includeTrend) {
                List<CourierRatingMetricsDto.RatingTrendDto> trend = courierRatingRepository
                        .getOverallDailyRatingTrend(startDate, endDate).stream()
                        .map(row -> CourierRatingMetricsDto.RatingTrendDto.builder()
                                .date(((java.sql.Date) row[0]).toLocalDate().atStartOfDay())
                                .averageRating(row[1] != null ? ((Number) row[1]).doubleValue() : null)
                                .ratingCount(((Number) row[2]).longValue())
                                .build())
                        .collect(Collectors.toList());
                builder.ratingTrend(trend);
            }
        }

        return builder.build();
    }

    /**
     * Collect app store rating metrics.
     */
    public AppStoreRatingMetricsDto collectAppStoreRatings(LocalDateTime startDate, LocalDateTime endDate,
                                                           boolean includeVersionBreakdown,
                                                           boolean includeCountryBreakdown,
                                                           boolean includeTrend) {
        log.debug("Collecting app store rating metrics from {} to {}", startDate, endDate);

        // Overall metrics
        Double overallAvg = appStoreRatingRepository.getOverallAverageRating(startDate, endDate);
        Long totalReviews = appStoreRatingRepository.countTotalReviews(startDate, endDate);

        // Platform breakdowns
        Double iosAvg = appStoreRatingRepository.getAverageRatingByPlatform(AppStoreRating.Platform.IOS, startDate, endDate);
        Long iosCount = appStoreRatingRepository.countReviewsByPlatform(AppStoreRating.Platform.IOS, startDate, endDate);
        Map<Integer, Long> iosDistribution = getRatingDistribution(
                appStoreRatingRepository.getRatingDistributionByPlatform(AppStoreRating.Platform.IOS, startDate, endDate));
        String iosLatestVersion = appStoreRatingRepository.getLatestVersion(AppStoreRating.Platform.IOS);

        Double androidAvg = appStoreRatingRepository.getAverageRatingByPlatform(AppStoreRating.Platform.ANDROID, startDate, endDate);
        Long androidCount = appStoreRatingRepository.countReviewsByPlatform(AppStoreRating.Platform.ANDROID, startDate, endDate);
        Map<Integer, Long> androidDistribution = getRatingDistribution(
                appStoreRatingRepository.getRatingDistributionByPlatform(AppStoreRating.Platform.ANDROID, startDate, endDate));
        String androidLatestVersion = appStoreRatingRepository.getLatestVersion(AppStoreRating.Platform.ANDROID);

        // Overall distribution
        Map<Integer, Long> overallDistribution = getRatingDistribution(
                appStoreRatingRepository.getOverallRatingDistribution(startDate, endDate));

        // Response metrics
        Long reviewsWithResponse = appStoreRatingRepository.countReviewsWithResponse(startDate, endDate);
        Double responseRate = totalReviews != null && totalReviews > 0 ?
                reviewsWithResponse * 100.0 / totalReviews : 0.0;

        AppStoreRatingMetricsDto.AppStoreRatingMetricsDtoBuilder builder = AppStoreRatingMetricsDto.builder()
                .overallAverageRating(overallAvg)
                .totalReviewCount(totalReviews)
                .distribution(overallDistribution)
                .distributionPercentage(calculateDistributionPercentage(overallDistribution, totalReviews))
                .iosRatings(AppStoreRatingMetricsDto.PlatformRatingDto.builder()
                        .platform("IOS")
                        .averageRating(iosAvg)
                        .reviewCount(iosCount)
                        .distribution(iosDistribution)
                        .latestVersion(iosLatestVersion)
                        .build())
                .androidRatings(AppStoreRatingMetricsDto.PlatformRatingDto.builder()
                        .platform("ANDROID")
                        .averageRating(androidAvg)
                        .reviewCount(androidCount)
                        .distribution(androidDistribution)
                        .latestVersion(androidLatestVersion)
                        .build())
                .reviewsWithResponse(reviewsWithResponse)
                .responseRate(responseRate)
                .periodStart(startDate)
                .periodEnd(endDate);

        // Ratings by version
        List<AppStoreRatingMetricsDto.VersionRatingDto> versionRatings = appStoreRatingRepository
                .getRatingsByVersion(startDate, endDate).stream()
                .map(row -> AppStoreRatingMetricsDto.VersionRatingDto.builder()
                        .version((String) row[0])
                        .platform(row[1] != null ? ((AppStoreRating.Platform) row[1]).name() : null)
                        .averageRating(row[2] != null ? ((Number) row[2]).doubleValue() : null)
                        .reviewCount(((Number) row[3]).longValue())
                        .build())
                .collect(Collectors.toList());
        builder.ratingsByVersion(versionRatings);

        // Ratings by country
        List<AppStoreRatingMetricsDto.CountryRatingDto> countryRatings = appStoreRatingRepository
                .getRatingsByCountry(startDate, endDate).stream()
                .limit(20)
                .map(row -> AppStoreRatingMetricsDto.CountryRatingDto.builder()
                        .country((String) row[0])
                        .averageRating(row[1] != null ? ((Number) row[1]).doubleValue() : null)
                        .reviewCount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());
        builder.ratingsByCountry(countryRatings);

        // Sentiment summary
        Object[] sentimentData = appStoreRatingRepository.getSentimentSummary(startDate, endDate);
        if (sentimentData != null && sentimentData.length >= 4) {
            Long positive = sentimentData[0] != null ? ((Number) sentimentData[0]).longValue() : 0L;
            Long neutral = sentimentData[1] != null ? ((Number) sentimentData[1]).longValue() : 0L;
            Long negative = sentimentData[2] != null ? ((Number) sentimentData[2]).longValue() : 0L;
            Double avgSentiment = sentimentData[3] != null ? ((Number) sentimentData[3]).doubleValue() : null;
            Long sentimentTotal = positive + neutral + negative;

            builder.sentimentSummary(AppStoreRatingMetricsDto.SentimentSummaryDto.builder()
                    .positiveCount(positive)
                    .neutralCount(neutral)
                    .negativeCount(negative)
                    .positivePercentage(sentimentTotal > 0 ? positive * 100.0 / sentimentTotal : 0.0)
                    .neutralPercentage(sentimentTotal > 0 ? neutral * 100.0 / sentimentTotal : 0.0)
                    .negativePercentage(sentimentTotal > 0 ? negative * 100.0 / sentimentTotal : 0.0)
                    .averageSentimentScore(avgSentiment)
                    .build());
        }

        // Rating trend
        if (includeTrend) {
            List<AppStoreRatingMetricsDto.RatingTrendDto> trend = appStoreRatingRepository
                    .getDailyRatingTrend(startDate, endDate).stream()
                    .map(row -> AppStoreRatingMetricsDto.RatingTrendDto.builder()
                            .date(((java.sql.Date) row[0]).toLocalDate().atStartOfDay())
                            .averageRating(row[1] != null ? ((Number) row[1]).doubleValue() : null)
                            .reviewCount(((Number) row[2]).longValue())
                            .iosRating(row[3] != null ? ((Number) row[3]).doubleValue() : null)
                            .androidRating(row[4] != null ? ((Number) row[4]).doubleValue() : null)
                            .build())
                    .collect(Collectors.toList());
            builder.ratingTrend(trend);
        }

        return builder.build();
    }

    /**
     * Convert query results to rating distribution map.
     */
    private Map<Integer, Long> getRatingDistribution(List<Object[]> results) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        // Initialize 1-5 stars
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        results.forEach(row -> {
            Integer score = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            distribution.put(score, count);
        });

        return distribution;
    }

    /**
     * Calculate distribution percentages.
     */
    private Map<Integer, Double> calculateDistributionPercentage(Map<Integer, Long> distribution, Long total) {
        Map<Integer, Double> percentages = new LinkedHashMap<>();
        if (total == null || total == 0) {
            distribution.keySet().forEach(key -> percentages.put(key, 0.0));
            return percentages;
        }

        distribution.forEach((score, count) -> {
            percentages.put(score, count * 100.0 / total);
        });

        return percentages;
    }
}

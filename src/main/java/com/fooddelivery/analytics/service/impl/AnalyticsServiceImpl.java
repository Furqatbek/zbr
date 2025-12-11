package com.fooddelivery.analytics.service.impl;

import com.fooddelivery.analytics.dto.*;
import com.fooddelivery.analytics.model.ActivityEventType;
import com.fooddelivery.analytics.repository.*;
import com.fooddelivery.analytics.service.AnalyticsService;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the AnalyticsService with Redis caching.
 *
 * <p>Cache TTLs:</p>
 * <ul>
 *   <li>DAU/WAU/MAU: 5 minutes</li>
 *   <li>Order volume: 1 minute</li>
 *   <li>Funnel metrics: 30 seconds</li>
 *   <li>Churn metrics: 10 minutes</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ActivityLogRepository activityLogRepository;
    private final AnalyticsOrderRepository orderRepository;
    private final AnalyticsUserRepository userRepository;
    private final AnalyticsRestaurantRepository restaurantRepository;
    private final AnalyticsCourierRepository courierRepository;
    private final CacheManager cacheManager;

    // Cache names
    public static final String CACHE_DAU_WAU_MAU = "analytics:dau_wau_mau";
    public static final String CACHE_ORDER_VOLUME = "analytics:order_volume";
    public static final String CACHE_CONVERSION = "analytics:conversion";
    public static final String CACHE_AOV = "analytics:aov";
    public static final String CACHE_ACTIVATION = "analytics:activation";
    public static final String CACHE_CHURN = "analytics:churn";

    // ============ User Activity Metrics ============

    @Override
    @Cacheable(value = CACHE_DAU_WAU_MAU, key = "'dau'")
    public Long getDailyActiveUsers() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return activityLogRepository.countDistinctActiveUsersSince(startOfDay);
    }

    @Override
    @Cacheable(value = CACHE_DAU_WAU_MAU, key = "'wau'")
    public Long getWeeklyActiveUsers() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return activityLogRepository.countDistinctActiveUsersSince(sevenDaysAgo);
    }

    @Override
    @Cacheable(value = CACHE_DAU_WAU_MAU, key = "'mau'")
    public Long getMonthlyActiveUsers() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return activityLogRepository.countDistinctActiveUsersSince(thirtyDaysAgo);
    }

    @Override
    @Cacheable(value = CACHE_DAU_WAU_MAU, key = "'full'")
    public DauWauMauDto getUserActivityMetrics() {
        log.debug("Calculating DAU/WAU/MAU metrics");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);

        Long dau = activityLogRepository.countDistinctActiveUsersSince(startOfToday);
        Long wau = activityLogRepository.countDistinctActiveUsersSince(startOfWeek);
        Long mau = activityLogRepository.countDistinctActiveUsersSince(startOfMonth);

        Long newUsersToday = userRepository.countNewUsersSince(startOfToday);
        Long newUsersWeek = userRepository.countNewUsersSince(startOfWeek);
        Long newUsersMonth = userRepository.countNewUsersSince(startOfMonth);

        DauWauMauDto dto = DauWauMauDto.builder()
                .dailyActiveUsers(dau != null ? dau : 0L)
                .weeklyActiveUsers(wau != null ? wau : 0L)
                .monthlyActiveUsers(mau != null ? mau : 0L)
                .referenceDate(LocalDate.now())
                .newUsersToday(newUsersToday != null ? newUsersToday : 0L)
                .newUsersThisWeek(newUsersWeek != null ? newUsersWeek : 0L)
                .newUsersThisMonth(newUsersMonth != null ? newUsersMonth : 0L)
                .calculatedAt(now)
                .build();

        dto.calculateRatios();
        return dto;
    }

    // ============ Order Volume Metrics ============

    @Override
    @Cacheable(value = CACHE_ORDER_VOLUME, key = "'daily'")
    public Long getOrdersPerDay() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return orderRepository.countOrdersSince(startOfDay);
    }

    @Override
    @Cacheable(value = CACHE_ORDER_VOLUME, key = "'hourly'")
    public OrderVolumeDto getOrdersPerHour() {
        return getOrderVolumeMetrics();
    }

    @Override
    @Cacheable(value = CACHE_ORDER_VOLUME, key = "'full'")
    public OrderVolumeDto getOrderVolumeMetrics() {
        log.debug("Calculating order volume metrics");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);

        // Total orders
        Long totalToday = orderRepository.countOrdersSince(startOfToday);
        Long totalWeek = orderRepository.countOrdersSince(startOfWeek);
        Long totalMonth = orderRepository.countOrdersSince(startOfMonth);

        // Completed orders
        Long completedToday = orderRepository.countCompletedOrdersSince(startOfToday);
        Long cancelledToday = orderRepository.countCancelledOrdersSince(startOfToday);
        Long failedToday = (totalToday != null ? totalToday : 0L) -
                (completedToday != null ? completedToday : 0L) -
                (cancelledToday != null ? cancelledToday : 0L);

        // First vs repeat orders
        Long firstTimeToday = orderRepository.countFirstTimeOrdersSince(startOfToday);
        Long repeatToday = orderRepository.countRepeatOrdersSince(startOfToday);

        // Orders per hour
        List<Object[]> hourlyData = orderRepository.getOrdersPerHourBetween(startOfToday, endOfToday);
        Map<Integer, Long> ordersPerHour = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            ordersPerHour.put(i, 0L);
        }
        for (Object[] row : hourlyData) {
            Integer hour = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            ordersPerHour.put(hour, count);
        }

        // Daily trend
        List<Object[]> dailyData = orderRepository.getDailyOrderCountsBetween(startOfWeek, now);
        List<OrderVolumeDto.DailyOrderCount> dailyTrend = dailyData.stream()
                .map(row -> OrderVolumeDto.DailyOrderCount.builder()
                        .date(((Date) row[0]).toLocalDate())
                        .totalOrders(((Number) row[1]).longValue())
                        .completedOrders(((Number) row[2]).longValue())
                        .cancelledOrders(((Number) row[3]).longValue())
                        .build())
                .collect(Collectors.toList());

        OrderVolumeDto dto = OrderVolumeDto.builder()
                .totalOrdersToday(totalToday != null ? totalToday : 0L)
                .totalOrdersWeek(totalWeek != null ? totalWeek : 0L)
                .totalOrdersMonth(totalMonth != null ? totalMonth : 0L)
                .completedOrdersToday(completedToday != null ? completedToday : 0L)
                .cancelledOrdersToday(cancelledToday != null ? cancelledToday : 0L)
                .failedOrdersToday(failedToday > 0 ? failedToday : 0L)
                .firstTimeOrdersToday(firstTimeToday != null ? firstTimeToday : 0L)
                .repeatOrdersToday(repeatToday != null ? repeatToday : 0L)
                .ordersPerHour(ordersPerHour)
                .dailyTrend(dailyTrend)
                .referenceDate(LocalDate.now())
                .calculatedAt(now)
                .build();

        dto.calculateDerivedMetrics();
        return dto;
    }

    // ============ Conversion Funnel Metrics ============

    @Override
    @Cacheable(value = CACHE_CONVERSION, key = "'rate'")
    public Double getConversionRate() {
        ConversionRateDto metrics = getConversionMetrics();
        return metrics.getOverallConversionRate();
    }

    @Override
    @Cacheable(value = CACHE_CONVERSION, key = "'full'")
    public ConversionRateDto getConversionMetrics() {
        return getConversionMetrics(1); // Default to 1 day
    }

    @Override
    @Cacheable(value = CACHE_CONVERSION, key = "'period_' + #days")
    public ConversionRateDto getConversionMetrics(int days) {
        log.debug("Calculating conversion metrics for {} days", days);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(days);

        // Get funnel events
        List<ActivityEventType> funnelEvents = List.of(
                ActivityEventType.RESTAURANT_VIEW,
                ActivityEventType.ADD_TO_CART,
                ActivityEventType.CHECKOUT_START,
                ActivityEventType.PAYMENT_COMPLETED
        );

        List<Object[]> funnelData = activityLogRepository.countDistinctUsersByEventTypesSince(funnelEvents, since);

        Map<ActivityEventType, Long> eventCounts = new HashMap<>();
        for (Object[] row : funnelData) {
            ActivityEventType type = (ActivityEventType) row[0];
            Long count = ((Number) row[1]).longValue();
            eventCounts.put(type, count);
        }

        ConversionRateDto dto = ConversionRateDto.builder()
                .restaurantViews(eventCounts.getOrDefault(ActivityEventType.RESTAURANT_VIEW, 0L))
                .addToCartEvents(eventCounts.getOrDefault(ActivityEventType.ADD_TO_CART, 0L))
                .checkoutStartEvents(eventCounts.getOrDefault(ActivityEventType.CHECKOUT_START, 0L))
                .paymentCompletedEvents(eventCounts.getOrDefault(ActivityEventType.PAYMENT_COMPLETED, 0L))
                .periodDays(days)
                .referenceDate(LocalDate.now())
                .calculatedAt(now)
                .build();

        dto.calculateConversionRates();
        return dto;
    }

    // ============ AOV Metrics ============

    @Override
    @Cacheable(value = CACHE_AOV, key = "'today'")
    public BigDecimal getAverageOrderValue() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal aov = orderRepository.getAverageOrderValueSince(startOfDay);
        return aov != null ? aov.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    @Cacheable(value = CACHE_AOV, key = "'full'")
    public AovDto getAovMetrics() {
        log.debug("Calculating AOV metrics");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);

        // AOV by period
        BigDecimal aovToday = orderRepository.getAverageOrderValueSince(startOfToday);
        BigDecimal aovWeek = orderRepository.getAverageOrderValueSince(startOfWeek);
        BigDecimal aovMonth = orderRepository.getAverageOrderValueSince(startOfMonth);

        // Revenue by period
        BigDecimal revenueToday = orderRepository.getTotalRevenueSince(startOfToday);
        BigDecimal revenueWeek = orderRepository.getTotalRevenueSince(startOfWeek);
        BigDecimal revenueMonth = orderRepository.getTotalRevenueSince(startOfMonth);

        // Order counts
        Long completedToday = orderRepository.countCompletedOrdersSince(startOfToday);
        Long completedWeek = orderRepository.countCompletedOrdersSince(startOfWeek);
        Long completedMonth = orderRepository.countCompletedOrdersSince(startOfMonth);

        // Additional stats
        Object[] stats = orderRepository.getOrderValueStatsSince(startOfToday);
        BigDecimal minValue = stats[0] != null ? (BigDecimal) stats[0] : BigDecimal.ZERO;
        BigDecimal maxValue = stats[1] != null ? (BigDecimal) stats[1] : BigDecimal.ZERO;

        BigDecimal medianValue = orderRepository.getMedianOrderValueSince(startOfToday);
        Double avgItems = orderRepository.getAverageItemsPerOrderSince(startOfToday);
        BigDecimal avgDeliveryFee = orderRepository.getAverageDeliveryFeeSince(startOfToday);
        BigDecimal avgTip = orderRepository.getAverageTipAmountSince(startOfToday);

        // Daily trend
        List<Object[]> dailyData = orderRepository.getDailyAovTrendBetween(startOfWeek, now);
        List<AovDto.DailyAov> dailyTrend = dailyData.stream()
                .map(row -> AovDto.DailyAov.builder()
                        .date(((Date) row[0]).toLocalDate())
                        .aov(row[1] != null ? ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                        .totalRevenue(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO)
                        .orderCount(((Number) row[3]).longValue())
                        .build())
                .collect(Collectors.toList());

        return AovDto.builder()
                .aovToday(aovToday != null ? aovToday.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .aovWeek(aovWeek != null ? aovWeek.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .aovMonth(aovMonth != null ? aovMonth.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalRevenueToday(revenueToday != null ? revenueToday : BigDecimal.ZERO)
                .totalRevenueWeek(revenueWeek != null ? revenueWeek : BigDecimal.ZERO)
                .totalRevenueMonth(revenueMonth != null ? revenueMonth : BigDecimal.ZERO)
                .completedOrdersToday(completedToday != null ? completedToday : 0L)
                .completedOrdersWeek(completedWeek != null ? completedWeek : 0L)
                .completedOrdersMonth(completedMonth != null ? completedMonth : 0L)
                .medianOrderValueToday(medianValue != null ? medianValue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .minOrderValueToday(minValue.setScale(2, RoundingMode.HALF_UP))
                .maxOrderValueToday(maxValue.setScale(2, RoundingMode.HALF_UP))
                .averageItemsPerOrder(avgItems != null ? Math.round(avgItems * 100) / 100.0 : 0.0)
                .averageDeliveryFee(avgDeliveryFee != null ? avgDeliveryFee.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .averageTipAmount(avgTip != null ? avgTip.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .dailyTrend(dailyTrend)
                .referenceDate(LocalDate.now())
                .calculatedAt(now)
                .build();
    }

    // ============ User Activation Metrics ============

    @Override
    @Cacheable(value = CACHE_ACTIVATION, key = "'first_delivery'")
    public Long getFirstDeliveryCount() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return orderRepository.countFirstDeliveryCompletedSince(startOfToday);
    }

    @Override
    @Cacheable(value = CACHE_ACTIVATION, key = "'activation_time'")
    public Double getActivationTime() {
        LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30);
        Object[] stats = orderRepository.getActivationTimeStatsSince(startOfMonth);
        return stats[0] != null ? ((Number) stats[0]).doubleValue() : 0.0;
    }

    @Override
    @Cacheable(value = CACHE_ACTIVATION, key = "'referral_rate'")
    public Double getReferralUsageRate() {
        LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30);
        Long referralSignups = userRepository.countReferralSignupsSince(startOfMonth);
        Long totalNewUsers = userRepository.countNewUsersSince(startOfMonth);

        if (totalNewUsers == null || totalNewUsers == 0) return 0.0;
        if (referralSignups == null) referralSignups = 0L;

        return Math.round((double) referralSignups / totalNewUsers * 10000) / 100.0;
    }

    @Override
    @Cacheable(value = CACHE_ACTIVATION, key = "'full'")
    public ActivationMetricsDto getUserActivationMetrics() {
        log.debug("Calculating activation metrics");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);

        // First deliveries
        Long firstDeliveryToday = orderRepository.countFirstDeliveryCompletedSince(startOfToday);
        Long firstDeliveryWeek = orderRepository.countFirstDeliveryCompletedSince(startOfWeek);
        Long firstDeliveryMonth = orderRepository.countFirstDeliveryCompletedSince(startOfMonth);

        // New registrations
        Long newRegsToday = userRepository.countNewUsersSince(startOfToday);
        Long newRegsWeek = userRepository.countNewUsersSince(startOfWeek);
        Long newRegsMonth = userRepository.countNewUsersSince(startOfMonth);

        // Activation time stats
        Object[] timeStats = orderRepository.getActivationTimeStatsSince(startOfMonth);
        Double avgTime = timeStats[0] != null ? ((Number) timeStats[0]).doubleValue() : 0.0;
        Double minTime = timeStats[1] != null ? ((Number) timeStats[1]).doubleValue() : 0.0;
        Double maxTime = timeStats[2] != null ? ((Number) timeStats[2]).doubleValue() : 0.0;

        // Referral signups
        Long referralToday = userRepository.countReferralSignupsSince(startOfToday);
        Long referralWeek = userRepository.countReferralSignupsSince(startOfWeek);
        Long referralMonth = userRepository.countReferralSignupsSince(startOfMonth);

        // Activation time distribution
        List<Object[]> distributionData = userRepository.getActivationTimeDistribution(startOfMonth);
        long totalUsers = distributionData.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum();
        List<ActivationMetricsDto.ActivationTimeBucket> distribution = distributionData.stream()
                .map(row -> ActivationMetricsDto.ActivationTimeBucket.builder()
                        .bucket((String) row[0])
                        .userCount(((Number) row[1]).longValue())
                        .percentage(totalUsers > 0 ?
                                Math.round(((Number) row[1]).doubleValue() / totalUsers * 10000) / 100.0 : 0.0)
                        .build())
                .collect(Collectors.toList());

        ActivationMetricsDto dto = ActivationMetricsDto.builder()
                .firstDeliveryCompletedToday(firstDeliveryToday != null ? firstDeliveryToday : 0L)
                .firstDeliveryCompletedWeek(firstDeliveryWeek != null ? firstDeliveryWeek : 0L)
                .firstDeliveryCompletedMonth(firstDeliveryMonth != null ? firstDeliveryMonth : 0L)
                .newRegistrationsToday(newRegsToday != null ? newRegsToday : 0L)
                .newRegistrationsWeek(newRegsWeek != null ? newRegsWeek : 0L)
                .newRegistrationsMonth(newRegsMonth != null ? newRegsMonth : 0L)
                .averageActivationTimeHours(Math.round(avgTime * 100) / 100.0)
                .minActivationTimeHours(Math.round(minTime * 100) / 100.0)
                .maxActivationTimeHours(Math.round(maxTime * 100) / 100.0)
                .referralSignupsToday(referralToday != null ? referralToday : 0L)
                .referralSignupsWeek(referralWeek != null ? referralWeek : 0L)
                .referralSignupsMonth(referralMonth != null ? referralMonth : 0L)
                .activationTimeDistribution(distribution)
                .referenceDate(LocalDate.now())
                .calculatedAt(now)
                .build();

        dto.calculateRates();
        return dto;
    }

    // ============ Churn Metrics ============

    @Override
    @Cacheable(value = CACHE_CHURN, key = "'user_rate'")
    public Double getUserChurnRate() {
        ChurnDto metrics = getChurnMetrics();
        return metrics.getUserChurnRate();
    }

    @Override
    @Cacheable(value = CACHE_CHURN, key = "'restaurant_rate'")
    public Double getRestaurantChurnRate() {
        ChurnDto metrics = getChurnMetrics();
        return metrics.getRestaurantChurnRate();
    }

    @Override
    @Cacheable(value = CACHE_CHURN, key = "'courier_rate'")
    public Double getCourierChurnRate() {
        ChurnDto metrics = getChurnMetrics();
        return metrics.getCourierChurnRate();
    }

    @Override
    @Cacheable(value = CACHE_CHURN, key = "'full'")
    public ChurnDto getChurnMetrics() {
        log.debug("Calculating churn metrics");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime fourteenDaysAgo = now.minusDays(14);
        LocalDateTime fifteenDaysAgo = now.minusDays(15);
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        // User churn
        Long totalUsersWithOrders = orderRepository.countTotalUsersWithOrders();
        Long usersOrderedLast30Days = orderRepository.countUsersWithOrdersSince(thirtyDaysAgo);
        Long usersChurned = (totalUsersWithOrders != null ? totalUsersWithOrders : 0L) -
                (usersOrderedLast30Days != null ? usersOrderedLast30Days : 0L);
        Long usersAtRisk = orderRepository.countUsersAtRiskOfChurn(thirtyDaysAgo, fifteenDaysAgo);

        // Restaurant churn
        Long totalActiveRestaurants = restaurantRepository.countByStatus(RestaurantStatus.ACTIVE);
        Long restaurantsWithOrders = restaurantRepository.countRestaurantsWithOrdersSince(thirtyDaysAgo);
        Long restaurantsChurned = restaurantRepository.countRestaurantsWithNoOrdersSince(thirtyDaysAgo);
        Long restaurantsAtRisk = restaurantRepository.countRestaurantsAtRisk(thirtyDaysAgo, 5);

        // Courier churn
        Long totalVerifiedCouriers = courierRepository.countVerifiedCouriers();
        Long couriersActive = courierRepository.countActiveCouriersSince(fourteenDaysAgo);
        Long couriersInactive = courierRepository.countInactiveCouriersSince(fourteenDaysAgo);
        Long couriersAtRisk = courierRepository.countCouriersAtRisk(fourteenDaysAgo, 1, 3);

        // Retention cohorts
        List<Object[]> userCohortData = userRepository.getUserRetentionCohorts(now.minusMonths(6));
        List<ChurnDto.RetentionCohort> userCohorts = userCohortData.stream()
                .map(row -> {
                    long cohortSize = ((Number) row[1]).longValue();
                    long active30d = ((Number) row[2]).longValue();
                    long active60d = ((Number) row[3]).longValue();
                    long active90d = ((Number) row[4]).longValue();
                    return ChurnDto.RetentionCohort.builder()
                            .cohortMonth((String) row[0])
                            .cohortSize(cohortSize)
                            .stillActiveAfter30Days(active30d)
                            .stillActiveAfter60Days(active60d)
                            .stillActiveAfter90Days(active90d)
                            .retentionRate30Days(cohortSize > 0 ? Math.round(active30d * 10000.0 / cohortSize) / 100.0 : 0.0)
                            .retentionRate60Days(cohortSize > 0 ? Math.round(active60d * 10000.0 / cohortSize) / 100.0 : 0.0)
                            .retentionRate90Days(cohortSize > 0 ? Math.round(active90d * 10000.0 / cohortSize) / 100.0 : 0.0)
                            .build();
                })
                .collect(Collectors.toList());

        ChurnDto dto = ChurnDto.builder()
                .totalActiveUsers(totalUsersWithOrders != null ? totalUsersWithOrders : 0L)
                .usersOrderedLast30Days(usersOrderedLast30Days != null ? usersOrderedLast30Days : 0L)
                .usersChurnedLast30Days(usersChurned > 0 ? usersChurned : 0L)
                .usersAtRiskOfChurn(usersAtRisk != null ? usersAtRisk : 0L)
                .totalActiveRestaurants(totalActiveRestaurants != null ? totalActiveRestaurants : 0L)
                .restaurantsWithOrdersLast30Days(restaurantsWithOrders != null ? restaurantsWithOrders : 0L)
                .restaurantsChurnedLast30Days(restaurantsChurned != null ? restaurantsChurned : 0L)
                .restaurantsAtRisk(restaurantsAtRisk != null ? restaurantsAtRisk : 0L)
                .totalVerifiedCouriers(totalVerifiedCouriers != null ? totalVerifiedCouriers : 0L)
                .couriersActiveLast14Days(couriersActive != null ? couriersActive : 0L)
                .couriersChurnedLast14Days(couriersInactive != null ? couriersInactive : 0L)
                .couriersAtRisk(couriersAtRisk != null ? couriersAtRisk : 0L)
                .userRetentionCohorts(userCohorts)
                .referenceDate(LocalDate.now())
                .calculatedAt(now)
                .build();

        dto.calculateRates();
        return dto;
    }

    // ============ Cache Management ============

    @Override
    @CacheEvict(value = {CACHE_DAU_WAU_MAU, CACHE_ORDER_VOLUME, CACHE_CONVERSION,
            CACHE_AOV, CACHE_ACTIVATION, CACHE_CHURN}, allEntries = true)
    public void evictAllCaches() {
        log.info("Evicting all analytics caches");
    }

    @Override
    public void evictCache(String cacheName) {
        log.info("Evicting cache: {}", cacheName);
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}

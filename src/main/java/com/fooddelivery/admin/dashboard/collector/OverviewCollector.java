package com.fooddelivery.admin.dashboard.collector;

import com.fooddelivery.admin.dashboard.dto.DashboardFilterRequest;
import com.fooddelivery.admin.dashboard.dto.DashboardOverviewDto;
import com.fooddelivery.admin.dashboard.repository.DashboardCourierRepository;
import com.fooddelivery.admin.dashboard.repository.DashboardOrderRepository;
import com.fooddelivery.admin.dashboard.repository.DashboardRestaurantRepository;
import com.fooddelivery.admin.dashboard.repository.SystemHealthRepository;
import com.fooddelivery.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Collector for dashboard overview metrics.
 * Aggregates data from multiple sources into the overview DTO.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OverviewCollector {

    private final DashboardOrderRepository orderRepository;
    private final DashboardRestaurantRepository restaurantRepository;
    private final DashboardCourierRepository courierRepository;
    private final SystemHealthRepository systemHealthRepository;

    private static final int ACTIVE_COURIER_THRESHOLD_MINUTES = 3;
    private static final int ACTIVE_RESTAURANT_THRESHOLD_MINUTES = 30;

    /**
     * Collect all overview metrics for the dashboard using filter request.
     */
    public DashboardOverviewDto collectOverview(DashboardFilterRequest filter) {
        return collect(filter.getEffectiveStartDate(), filter.getEffectiveEndDate());
    }

    /**
     * Collect all overview metrics for the dashboard.
     */
    public DashboardOverviewDto collect(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Collecting dashboard overview metrics from {} to {}", startDate, endDate);

        LocalDateTime now = LocalDateTime.now();

        // Order metrics
        Long ordersToday = orderRepository.countByDateRange(startDate, endDate);
        Long ordersPending = orderRepository.countByStatusAndDateRange(OrderStatus.CREATED, startDate, endDate);
        Long ordersInPrep = orderRepository.countByStatusAndDateRange(OrderStatus.PREPARING, startDate, endDate);

        List<OrderStatus> activeStatuses = Arrays.asList(
                OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT
        );
        Long ordersInDelivery = orderRepository.countByStatusIn(activeStatuses);

        Long ordersCompleted = orderRepository.countByStatusAndDateRange(OrderStatus.COMPLETED, startDate, endDate);
        ordersCompleted += orderRepository.countByStatusAndDateRange(OrderStatus.DELIVERED, startDate, endDate);
        Long ordersCancelled = orderRepository.countByStatusAndDateRange(OrderStatus.CANCELLED, startDate, endDate);

        // Revenue metrics
        BigDecimal revenueToday = orderRepository.sumRevenueByDateRange(startDate, endDate);
        BigDecimal avgOrderValue = orderRepository.avgOrderValueByDateRange(startDate, endDate);
        BigDecimal totalTips = orderRepository.sumTipsByDateRange(startDate, endDate);
        BigDecimal totalDiscounts = orderRepository.sumDiscountsByDateRange(startDate, endDate);

        // Active entities
        LocalDateTime courierThreshold = now.minusMinutes(ACTIVE_COURIER_THRESHOLD_MINUTES);
        Long activeCouriers = courierRepository.countActiveCouriers(courierThreshold);
        Long totalCouriersOnline = courierRepository.countOnlineCouriers();
        Long availableCouriers = courierRepository.countAvailableCouriers();
        Long busyCouriers = courierRepository.countByStatus(com.fooddelivery.courier.entity.CourierStatus.BUSY);

        LocalDateTime restaurantThreshold = now.minusMinutes(ACTIVE_RESTAURANT_THRESHOLD_MINUTES);
        Long activeRestaurants = restaurantRepository.countActiveRestaurants(restaurantThreshold);
        Long totalRestaurantsOnline = restaurantRepository.countOnlineRestaurants();
        Long restaurantsAccepting = restaurantRepository.countAcceptingOrders();

        // Performance metrics
        Double avgDeliveryTime = orderRepository.avgDeliveryTimeByDateRange(startDate, endDate);
        Double avgPrepTime = orderRepository.avgPrepTimeByDateRange(startDate, endDate);
        Double avgAcceptanceTime = orderRepository.avgAcceptanceTimeByDateRange(startDate, endDate);

        // Calculate fulfillment rate
        Double fulfillmentRate = calculateFulfillmentRate(ordersCompleted, ordersToday);

        // Comparison with yesterday
        LocalDateTime yesterdayStart = startDate.minusDays(1);
        LocalDateTime yesterdayEnd = endDate.minusDays(1);
        DashboardOverviewDto.OrderComparisonDto comparison = collectComparison(
                ordersToday, revenueToday, yesterdayStart, yesterdayEnd
        );

        // System status
        DashboardOverviewDto.SystemStatusDto systemStatus = collectSystemStatus();

        return DashboardOverviewDto.builder()
                .ordersToday(ordersToday)
                .ordersPendingAcceptance(ordersPending)
                .ordersInPreparation(ordersInPrep)
                .ordersInDelivery(ordersInDelivery)
                .ordersCompletedToday(ordersCompleted)
                .ordersCancelledToday(ordersCancelled)
                .revenueToday(revenueToday)
                .averageOrderValue(avgOrderValue)
                .totalTipsToday(totalTips)
                .totalDiscountsToday(totalDiscounts)
                .activeCouriers(activeCouriers)
                .totalCouriersOnline(totalCouriersOnline)
                .availableCouriers(availableCouriers)
                .busyCouriers(busyCouriers)
                .activeRestaurants(activeRestaurants)
                .totalRestaurantsOnline(totalRestaurantsOnline)
                .restaurantsAcceptingOrders(restaurantsAccepting)
                .avgDeliveryTimeToday(avgDeliveryTime)
                .avgPrepTimeToday(avgPrepTime)
                .avgAcceptanceTimeToday(avgAcceptanceTime)
                .orderFulfillmentRate(fulfillmentRate)
                .comparison(comparison)
                .systemStatus(systemStatus)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private DashboardOverviewDto.OrderComparisonDto collectComparison(
            Long ordersToday, BigDecimal revenueToday,
            LocalDateTime yesterdayStart, LocalDateTime yesterdayEnd) {

        Long ordersYesterday = orderRepository.countByDateRange(yesterdayStart, yesterdayEnd);
        BigDecimal revenueYesterday = orderRepository.sumRevenueByDateRange(yesterdayStart, yesterdayEnd);

        Double orderChangePercent = calculatePercentChange(ordersYesterday, ordersToday);
        Double revenueChangePercent = calculatePercentChange(revenueYesterday, revenueToday);

        String trend = determineTrend(orderChangePercent);

        return DashboardOverviewDto.OrderComparisonDto.builder()
                .ordersYesterday(ordersYesterday)
                .revenueYesterday(revenueYesterday)
                .orderChangePercent(orderChangePercent)
                .revenueChangePercent(revenueChangePercent)
                .trend(trend)
                .build();
    }

    private DashboardOverviewDto.SystemStatusDto collectSystemStatus() {
        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);

        Double apiLatencyP50 = systemHealthRepository.avgApiLatencyP50(hourAgo);
        Double apiLatencyP90 = systemHealthRepository.avgApiLatencyP90(hourAgo);
        Double dbConnections = systemHealthRepository.avgDbConnections(hourAgo);
        Double redisLatency = systemHealthRepository.avgRedisLatency(hourAgo);

        // Get latest snapshot for current values
        var latestSnapshot = systemHealthRepository.findLatestSnapshot();

        Integer currentApiLatencyP50 = latestSnapshot.map(s -> s.getApiLatencyP50()).orElse(0);
        Integer currentApiLatencyP90 = latestSnapshot.map(s -> s.getApiLatencyP90()).orElse(0);
        Integer currentDbLoad = latestSnapshot.map(s -> s.getDbActiveConnections()).orElse(0);
        Integer currentDbIdle = latestSnapshot.map(s -> s.getDbIdleConnections()).orElse(0);
        Integer currentRedisLatency = latestSnapshot.map(s -> s.getRedisLatencyMs()).orElse(0);
        Long currentQueueBacklog = latestSnapshot.map(s -> s.getQueueBacklog()).orElse(0L);
        Double errorRate = latestSnapshot.map(s -> s.getErrorRatePercent()).orElse(0.0);
        Double cpuUsage = latestSnapshot.map(s -> s.getCpuUsagePercent()).orElse(0.0);
        Double memoryUsage = latestSnapshot.map(s -> s.getMemoryUsagePercent()).orElse(0.0);

        String overallStatus = determineOverallStatus(
                currentApiLatencyP90, currentDbLoad, currentRedisLatency, errorRate
        );

        return DashboardOverviewDto.SystemStatusDto.builder()
                .apiLatencyP50(currentApiLatencyP50)
                .apiLatencyP90(currentApiLatencyP90)
                .dbLoad(currentDbLoad)
                .dbIdleConnections(currentDbIdle)
                .redisLatency(currentRedisLatency)
                .queueBacklog(currentQueueBacklog)
                .errorRatePercent(errorRate)
                .cpuUsagePercent(cpuUsage)
                .memoryUsagePercent(memoryUsage)
                .overallStatus(overallStatus)
                .build();
    }

    private Double calculateFulfillmentRate(Long completed, Long total) {
        if (total == null || total == 0) return 100.0;
        return (completed.doubleValue() / total.doubleValue()) * 100;
    }

    private Double calculatePercentChange(Long oldValue, Long newValue) {
        if (oldValue == null || oldValue == 0) {
            return newValue != null && newValue > 0 ? 100.0 : 0.0;
        }
        return ((newValue.doubleValue() - oldValue.doubleValue()) / oldValue.doubleValue()) * 100;
    }

    private Double calculatePercentChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return newValue != null && newValue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return newValue.subtract(oldValue)
                .divide(oldValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private String determineTrend(Double percentChange) {
        if (percentChange == null) return "STABLE";
        if (percentChange > 5) return "UP";
        if (percentChange < -5) return "DOWN";
        return "STABLE";
    }

    private String determineOverallStatus(Integer apiLatency, Integer dbLoad, Integer redisLatency, Double errorRate) {
        // Critical thresholds
        if (apiLatency > 1000 || dbLoad > 100 || redisLatency > 100 || errorRate > 5) {
            return "CRITICAL";
        }
        // Degraded thresholds
        if (apiLatency > 500 || dbLoad > 50 || redisLatency > 50 || errorRate > 1) {
            return "DEGRADED";
        }
        return "HEALTHY";
    }
}

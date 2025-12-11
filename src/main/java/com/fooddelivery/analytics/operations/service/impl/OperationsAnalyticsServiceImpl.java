package com.fooddelivery.analytics.operations.service.impl;

import com.fooddelivery.analytics.operations.dto.*;
import com.fooddelivery.analytics.operations.model.CourierOrderEventType;
import com.fooddelivery.analytics.operations.model.RestaurantOrderEventType;
import com.fooddelivery.analytics.operations.repository.*;
import com.fooddelivery.analytics.operations.service.OperationsAnalyticsService;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.restaurant.entity.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of OperationsAnalyticsService.
 * Provides cached operational metrics for orders, restaurants, and couriers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OperationsAnalyticsServiceImpl implements OperationsAnalyticsService {

    // Cache names
    public static final String CACHE_ORDER_FULFILLMENT = "ops:order_fulfillment";
    public static final String CACHE_RESTAURANT_PERFORMANCE = "ops:restaurant_performance";
    public static final String CACHE_COURIER_PERFORMANCE = "ops:courier_performance";
    public static final String CACHE_DELIVERY_SUCCESS = "ops:delivery_success";
    public static final String CACHE_ETA_ACCURACY = "ops:eta_accuracy";
    public static final String CACHE_OPERATIONS_SUMMARY = "ops:summary";

    private final OperationsOrderRepository orderRepository;
    private final CourierLocationEventRepository locationEventRepository;
    private final CourierOrderEventRepository courierOrderEventRepository;
    private final CourierAvailabilityEventRepository availabilityEventRepository;
    private final RestaurantOrderEventRepository restaurantOrderEventRepository;
    private final RestaurantOnlineStatusRepository onlineStatusRepository;
    private final MenuUpdateHistoryRepository menuUpdateRepository;
    private final EtaHistoryRepository etaHistoryRepository;
    private final EntityManager entityManager;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(value = CACHE_ORDER_FULFILLMENT, key = "#orderId")
    public OrderFulfillmentMetricsDto getOrderFulfillmentMetrics(Long orderId) {
        log.debug("Calculating order fulfillment metrics for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        OrderFulfillmentMetricsDto dto = OrderFulfillmentMetricsDto.builder()
                .orderId(order.getId())
                .externalOrderNo(order.getExternalOrderNo())
                .restaurantId(order.getRestaurant().getId())
                .courierId(order.getCourier() != null ? order.getCourier().getId() : null)
                .orderStatus(order.getStatus().name())
                .placedAt(order.getCreatedAt())
                .acceptedAt(order.getAcceptedAt())
                .preparingAt(order.getPreparingAt())
                .readyAt(order.getReadyAt())
                .pickedUpAt(order.getPickedUpAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .estimatedPrepTimeMinutes(order.getEstimatedPrepTimeMinutes())
                .build();

        dto.calculateDurations();
        return dto;
    }

    @Override
    @Cacheable(value = CACHE_RESTAURANT_PERFORMANCE, key = "#restaurantId + '-' + #startDate + '-' + #endDate")
    public RestaurantPerformanceDto getRestaurantPerformanceMetrics(Long restaurantId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating restaurant performance metrics for restaurant: {} from {} to {}",
                restaurantId, startDate, endDate);

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        // Get restaurant info
        Restaurant restaurant = entityManager.find(Restaurant.class, restaurantId);
        if (restaurant == null) {
            throw new EntityNotFoundException("Restaurant not found: " + restaurantId);
        }

        RestaurantPerformanceDto dto = RestaurantPerformanceDto.builder()
                .restaurantId(restaurantId)
                .restaurantName(restaurant.getName())
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();

        // Order acceptance metrics from events
        Long received = restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                restaurantId, RestaurantOrderEventType.RECEIVED, startTime, endTime);
        Long accepted = restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                restaurantId, RestaurantOrderEventType.ACCEPTED, startTime, endTime);
        Long rejected = restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                restaurantId, RestaurantOrderEventType.REJECTED, startTime, endTime);
        Long ready = restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                restaurantId, RestaurantOrderEventType.READY, startTime, endTime);
        Long cancelled = restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                restaurantId, RestaurantOrderEventType.CANCELLED, startTime, endTime);

        dto.setTotalOrdersReceived(received != null ? received : 0L);
        dto.setOrdersAccepted(accepted != null ? accepted : 0L);
        dto.setOrdersRejected(rejected != null ? rejected : 0L);
        dto.setOrdersCompleted(ready != null ? ready : 0L);
        dto.setOrdersCancelledByRestaurant(cancelled != null ? cancelled : 0L);

        // Acceptance time from orders
        Double avgAcceptanceTime = orderRepository.getRestaurantAvgAcceptanceTime(restaurantId, startTime, endTime);
        dto.setAverageAcceptanceTimeMinutes(avgAcceptanceTime);

        // Preparation time metrics
        Double avgPrepTime = restaurantOrderEventRepository.getAveragePreparationTime(restaurantId, startTime, endTime);
        Double medianPrepTime = restaurantOrderEventRepository.getMedianPreparationTime(restaurantId, startTime, endTime);
        Double p90PrepTime = restaurantOrderEventRepository.getP90PreparationTime(restaurantId, startTime, endTime);
        Long onTimePrepCount = restaurantOrderEventRepository.countOrdersPreparedOnTime(restaurantId, startTime, endTime);

        dto.setAveragePreparationTimeMinutes(avgPrepTime);
        dto.setMedianPreparationTimeMinutes(medianPrepTime);
        dto.setP90PreparationTimeMinutes(p90PrepTime);
        dto.setOrdersPreparedOnTime(onTimePrepCount != null ? onTimePrepCount : 0L);

        // Online status metrics
        Long offlineMinutes = onlineStatusRepository.getTotalOfflineTimeMinutes(restaurantId, startTime, endTime);
        Integer offlineCount = onlineStatusRepository.countOfflineEvents(restaurantId, startTime, endTime);
        LocalDateTime lastOffline = onlineStatusRepository.getLastOfflineTime(restaurantId);

        dto.setTotalOfflineTimeMinutes(offlineMinutes != null ? offlineMinutes : 0L);
        dto.setOfflineCount(offlineCount != null ? offlineCount : 0);
        dto.setLastOfflineAt(lastOffline);

        // Calculate uptime percentage
        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        if (totalMinutes > 0 && offlineMinutes != null) {
            dto.setUptimePercentage(100.0 - (offlineMinutes * 100.0 / totalMinutes));
        } else {
            dto.setUptimePercentage(100.0);
        }

        // Menu update metrics
        Long menuUpdates = menuUpdateRepository.countByRestaurantIdAndTimeRange(restaurantId, startTime, endTime);
        LocalDateTime lastMenuUpdate = menuUpdateRepository.getLastMenuUpdateTime(restaurantId);
        Object[] menuSummary = menuUpdateRepository.getMenuUpdateSummary(restaurantId, startTime, endTime);

        dto.setTotalMenuUpdates(menuUpdates != null ? menuUpdates : 0L);
        dto.setLastMenuUpdateAt(lastMenuUpdate);

        if (menuSummary != null && menuSummary.length >= 5) {
            dto.setItemsAdded(((Number) menuSummary[1]).intValue());
            dto.setItemsRemoved(((Number) menuSummary[2]).intValue());
            dto.setPriceChanges(((Number) menuSummary[4]).intValue());
        }

        // Calculate updates per day/week
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > 0 && menuUpdates != null) {
            dto.setMenuUpdatesPerDay(menuUpdates.doubleValue() / days);
            dto.setMenuUpdatesPerWeek(menuUpdates.doubleValue() / days * 7);
        }

        // Orders by hour
        List<Object[]> hourlyOrders = restaurantOrderEventRepository.getOrdersByHour(restaurantId, startTime, endTime);
        Map<Integer, Long> ordersByHour = new HashMap<>();
        for (Object[] row : hourlyOrders) {
            int hour = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            ordersByHour.put(hour, count);
        }
        dto.setOrdersByHour(ordersByHour);

        // Daily metrics
        List<Object[]> dailyData = restaurantOrderEventRepository.getDailyMetrics(restaurantId, startTime, endTime);
        List<RestaurantPerformanceDto.DailyRestaurantMetrics> dailyMetrics = new ArrayList<>();
        for (Object[] row : dailyData) {
            dailyMetrics.add(RestaurantPerformanceDto.DailyRestaurantMetrics.builder()
                    .date(((Date) row[0]).toLocalDate().atStartOfDay())
                    .ordersReceived(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                    .ordersCompleted(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                    .avgPreparationTimeMinutes(row[6] != null ? ((Number) row[6]).doubleValue() : null)
                    .build());
        }
        dto.setDailyMetrics(dailyMetrics);

        dto.calculateDerivedMetrics();
        return dto;
    }

    @Override
    @Cacheable(value = CACHE_COURIER_PERFORMANCE, key = "#courierId + '-' + #startDate + '-' + #endDate")
    public CourierPerformanceDto getCourierPerformanceMetrics(Long courierId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating courier performance metrics for courier: {} from {} to {}",
                courierId, startDate, endDate);

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        // Get courier info
        Courier courier = entityManager.find(Courier.class, courierId);
        if (courier == null) {
            throw new EntityNotFoundException("Courier not found: " + courierId);
        }

        CourierPerformanceDto dto = CourierPerformanceDto.builder()
                .courierId(courierId)
                .courierName(courier.getUser() != null ? courier.getUser().getFirstName() + " " + courier.getUser().getLastName() : "Unknown")
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();

        // Job acceptance metrics
        Long offered = courierOrderEventRepository.countByCourierIdAndEventType(
                courierId, CourierOrderEventType.OFFERED, startTime, endTime);
        Long accepted = courierOrderEventRepository.countByCourierIdAndEventType(
                courierId, CourierOrderEventType.ACCEPTED, startTime, endTime);
        Long rejected = courierOrderEventRepository.countByCourierIdAndEventType(
                courierId, CourierOrderEventType.REJECTED, startTime, endTime);
        Long timedOut = courierOrderEventRepository.countByCourierIdAndEventType(
                courierId, CourierOrderEventType.TIMED_OUT, startTime, endTime);
        Long courierCancelled = courierOrderEventRepository.countByCourierIdAndEventType(
                courierId, CourierOrderEventType.CANCELLED, startTime, endTime);

        dto.setTotalOrdersOffered(offered != null ? offered : 0L);
        dto.setOrdersAccepted(accepted != null ? accepted : 0L);
        dto.setOrdersRejected(rejected != null ? rejected : 0L);
        dto.setOrdersTimedOut(timedOut != null ? timedOut : 0L);
        dto.setOrdersCancelledByCourier(courierCancelled != null ? courierCancelled : 0L);

        // Response time
        Double avgResponseTime = courierOrderEventRepository.getAverageResponseTime(courierId, startTime, endTime);
        dto.setAverageResponseTimeSeconds(avgResponseTime);

        // Delivery metrics from orders
        Long deliveriesCompleted = orderRepository.countCourierDeliveries(courierId, startTime, endTime);
        Object[] deliveryStats = orderRepository.getCourierDeliveryTimeStats(courierId, startTime, endTime);

        dto.setDeliveriesCompleted(deliveriesCompleted != null ? deliveriesCompleted : 0L);

        if (deliveryStats != null && deliveryStats[0] != null) {
            dto.setAverageDeliveryTimeMinutes(((Number) deliveryStats[0]).doubleValue());
            dto.setFastestDeliveryMinutes(deliveryStats[1] != null ? ((Number) deliveryStats[1]).longValue() : null);
            dto.setSlowestDeliveryMinutes(deliveryStats[2] != null ? ((Number) deliveryStats[2]).longValue() : null);
        }

        // Cancellation reasons
        List<Object[]> cancellationReasons = courierOrderEventRepository.getCancellationReasons(courierId, startTime, endTime);
        Map<String, Long> reasonsMap = new HashMap<>();
        for (Object[] row : cancellationReasons) {
            String reason = row[0] != null ? (String) row[0] : "Unknown";
            Long count = ((Number) row[1]).longValue();
            reasonsMap.put(reason, count);
        }
        dto.setCancellationReasons(reasonsMap);

        // Availability metrics
        Long activeMinutes = availabilityEventRepository.getTotalActiveTimeMinutes(courierId, startTime, endTime);
        Long availableMinutes = availabilityEventRepository.getTotalAvailableTimeMinutes(courierId, startTime, endTime);

        dto.setTotalActiveTimeMinutes(activeMinutes != null ? activeMinutes : 0L);
        dto.setTotalAvailableTimeMinutes(availableMinutes != null ? availableMinutes : 0L);

        // Calculate average active hours per day
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > 0 && activeMinutes != null) {
            dto.setAverageActiveHoursPerDay(activeMinutes / 60.0 / days);
        }

        // Availability by hour
        List<Object[]> hourlyAvailability = availabilityEventRepository.getAvailabilityByHour(courierId, startTime, endTime);
        Map<Integer, Long> availabilityByHour = new HashMap<>();
        for (Object[] row : hourlyAvailability) {
            int hour = ((Number) row[0]).intValue();
            long minutes = ((Number) row[1]).longValue();
            availabilityByHour.put(hour, minutes);
        }
        dto.setAvailabilityByHour(availabilityByHour);

        // Location update metrics
        Long locationUpdates = locationEventRepository.countByCourierIdAndTimeRange(courierId, startTime, endTime);
        dto.setTotalLocationUpdates(locationUpdates != null ? locationUpdates : 0L);

        // Earnings from courier entity
        dto.setTotalEarnings(courier.getTotalEarnings());
        dto.setAverageRating(courier.getAverageRating());
        dto.setTotalRatings(courier.getTotalRatings());

        // Daily metrics
        List<Object[]> dailyEvents = courierOrderEventRepository.getDailyEventCounts(courierId, startTime, endTime);
        List<CourierPerformanceDto.DailyCourierMetrics> dailyMetrics = new ArrayList<>();
        for (Object[] row : dailyEvents) {
            dailyMetrics.add(CourierPerformanceDto.DailyCourierMetrics.builder()
                    .date(((Date) row[0]).toLocalDate().atStartOfDay())
                    .ordersOffered(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                    .ordersAccepted(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                    .build());
        }
        dto.setDailyMetrics(dailyMetrics);

        dto.calculateDerivedMetrics();
        return dto;
    }

    @Override
    @Cacheable(value = CACHE_DELIVERY_SUCCESS, key = "#startDate + '-' + #endDate")
    public DeliverySuccessRateDto getDeliverySuccessRate(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating delivery success rate from {} to {}", startDate, endDate);

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        DeliverySuccessRateDto dto = DeliverySuccessRateDto.builder()
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();

        // Basic counts
        Long totalOrders = orderRepository.countTotalOrders(startTime, endTime);
        Long successfulDeliveries = orderRepository.countSuccessfulDeliveries(startTime, endTime);
        Long majorDelays = orderRepository.countOrdersWithMajorDelays(startTime, endTime);
        Long onTimeDeliveries = orderRepository.countOnTimeDeliveries(startTime, endTime);

        dto.setTotalOrders(totalOrders != null ? totalOrders : 0L);
        dto.setSuccessfulDeliveries(successfulDeliveries != null ? successfulDeliveries : 0L);
        dto.setOrdersWithMajorDelays(majorDelays != null ? majorDelays : 0L);
        dto.setOnTimeDeliveries(onTimeDeliveries != null ? onTimeDeliveries : 0L);

        // Cancellation breakdown by pattern
        Long cancelledByCustomer = orderRepository.countCancelledByReasonPattern("%customer%", startTime, endTime);
        Long cancelledByRestaurant = orderRepository.countCancelledByReasonPattern("%restaurant%", startTime, endTime);
        Long cancelledByCourier = orderRepository.countCancelledByReasonPattern("%courier%", startTime, endTime);

        dto.setCancelledByCustomer(cancelledByCustomer != null ? cancelledByCustomer : 0L);
        dto.setCancelledByRestaurant(cancelledByRestaurant != null ? cancelledByRestaurant : 0L);
        dto.setCancelledByCourier(cancelledByCourier != null ? cancelledByCourier : 0L);

        // Cancellation reasons
        List<Object[]> cancellationReasons = orderRepository.getCancellationReasons(startTime, endTime);
        Map<String, Long> reasonsMap = new HashMap<>();
        for (Object[] row : cancellationReasons) {
            String reason = row[0] != null ? (String) row[0] : "Unknown";
            Long count = ((Number) row[1]).longValue();
            reasonsMap.put(reason, count);
        }
        dto.setCancellationReasons(reasonsMap);

        // Success rate by hour
        List<Object[]> hourlyRates = orderRepository.getSuccessRateByHour(startTime, endTime);
        Map<Integer, Double> rateByHour = new HashMap<>();
        for (Object[] row : hourlyRates) {
            int hour = ((Number) row[0]).intValue();
            double rate = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            rateByHour.put(hour, rate);
        }
        dto.setSuccessRateByHour(rateByHour);

        // Success rate by day of week
        List<Object[]> dailyRates = orderRepository.getSuccessRateByDayOfWeek(startTime, endTime);
        Map<Integer, Double> rateByDay = new HashMap<>();
        for (Object[] row : dailyRates) {
            int dow = ((Number) row[0]).intValue();
            double rate = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            rateByDay.put(dow, rate);
        }
        dto.setSuccessRateByDayOfWeek(rateByDay);

        // Success rate by restaurant
        List<Object[]> restaurantRates = orderRepository.getSuccessRateByRestaurant(startTime, endTime);
        List<DeliverySuccessRateDto.RestaurantSuccessRate> byRestaurant = new ArrayList<>();
        for (Object[] row : restaurantRates) {
            byRestaurant.add(DeliverySuccessRateDto.RestaurantSuccessRate.builder()
                    .restaurantId(((Number) row[0]).longValue())
                    .restaurantName((String) row[1])
                    .totalOrders(((Number) row[2]).longValue())
                    .successfulOrders(((Number) row[3]).longValue())
                    .successRate(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                    .build());
        }
        dto.setByRestaurant(byRestaurant);

        // Daily metrics
        List<Object[]> dailyMetricsData = orderRepository.getDailySuccessMetrics(startTime, endTime);
        List<DeliverySuccessRateDto.DailySuccessMetrics> dailyMetrics = new ArrayList<>();
        for (Object[] row : dailyMetricsData) {
            long total = ((Number) row[1]).longValue();
            long successful = ((Number) row[2]).longValue();
            dailyMetrics.add(DeliverySuccessRateDto.DailySuccessMetrics.builder()
                    .date(((Date) row[0]).toLocalDate().atStartOfDay())
                    .totalOrders(total)
                    .successfulOrders(successful)
                    .successRate(total > 0 ? (successful * 100.0 / total) : 0.0)
                    .cancellations(((Number) row[3]).longValue())
                    .majorDelays(((Number) row[4]).longValue())
                    .build());
        }
        dto.setDailyMetrics(dailyMetrics);

        dto.calculateDerivedMetrics();
        return dto;
    }

    @Override
    @Cacheable(value = CACHE_ETA_ACCURACY, key = "#startDate + '-' + #endDate")
    public EtaAccuracyDto getEtaAccuracyMetrics(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating ETA accuracy metrics from {} to {}", startDate, endDate);

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        EtaAccuracyDto dto = EtaAccuracyDto.builder()
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();

        // Overall stats
        Object[] overallStats = etaHistoryRepository.getOverallEtaStats(startTime, endTime);
        if (overallStats != null && overallStats[2] != null) {
            dto.setAverageEtaErrorMinutes(overallStats[0] != null ? ((Number) overallStats[0]).doubleValue() : null);
            dto.setAverageAbsoluteErrorMinutes(overallStats[1] != null ? ((Number) overallStats[1]).doubleValue() : null);
            dto.setTotalDeliveries(((Number) overallStats[2]).longValue());
        }

        // Percentile metrics
        Double medianError = etaHistoryRepository.getMedianAbsoluteError(startTime, endTime);
        Double p90Error = etaHistoryRepository.getP90AbsoluteError(startTime, endTime);
        dto.setMedianAbsoluteErrorMinutes(medianError);
        dto.setP90AbsoluteErrorMinutes(p90Error);

        // Accuracy buckets
        Long within5 = etaHistoryRepository.countWithinMinutes(5, startTime, endTime);
        Long within10 = etaHistoryRepository.countWithinMinutes(10, startTime, endTime);
        Long within15 = etaHistoryRepository.countWithinMinutes(15, startTime, endTime);

        dto.setWithin5Minutes(within5 != null ? within5 : 0L);
        dto.setWithin10Minutes(within10 != null ? within10 : 0L);
        dto.setWithin15Minutes(within15 != null ? within15 : 0L);

        // Over/under estimation
        Object[] overUnderStats = etaHistoryRepository.getOverUnderEstimationStats(startTime, endTime);
        if (overUnderStats != null) {
            dto.setOverEstimationCount(overUnderStats[0] != null ? ((Number) overUnderStats[0]).longValue() : 0L);
            dto.setUnderEstimationCount(overUnderStats[1] != null ? ((Number) overUnderStats[1]).longValue() : 0L);
            dto.setAverageOverEstimationMinutes(overUnderStats[2] != null ? ((Number) overUnderStats[2]).doubleValue() : null);
            dto.setAverageUnderEstimationMinutes(overUnderStats[3] != null ? ((Number) overUnderStats[3]).doubleValue() : null);
        }

        // Error by hour
        List<Object[]> hourlyError = etaHistoryRepository.getErrorByHour(startTime, endTime);
        Map<Integer, Double> errorByHour = new HashMap<>();
        for (Object[] row : hourlyError) {
            int hour = ((Number) row[0]).intValue();
            double avgError = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            errorByHour.put(hour, avgError);
        }
        dto.setErrorByHour(errorByHour);

        // Error by day of week
        List<Object[]> dailyError = etaHistoryRepository.getErrorByDayOfWeek(startTime, endTime);
        Map<Integer, Double> errorByDay = new HashMap<>();
        for (Object[] row : dailyError) {
            int dow = ((Number) row[0]).intValue();
            double avgError = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            errorByDay.put(dow, avgError);
        }
        dto.setErrorByDayOfWeek(errorByDay);

        // Phase accuracy
        Object[] phaseStats = etaHistoryRepository.getPhaseAccuracy(startTime, endTime);
        if (phaseStats != null) {
            if (phaseStats[0] != null || phaseStats[1] != null) {
                Double avgEstPrep = phaseStats[0] != null ? ((Number) phaseStats[0]).doubleValue() : null;
                Double avgActPrep = phaseStats[1] != null ? ((Number) phaseStats[1]).doubleValue() : null;
                dto.setPreparationAccuracy(EtaAccuracyDto.PhaseAccuracy.builder()
                        .averageEstimatedMinutes(avgEstPrep)
                        .averageActualMinutes(avgActPrep)
                        .averageErrorMinutes(avgEstPrep != null && avgActPrep != null ? avgActPrep - avgEstPrep : null)
                        .build());
            }
            if (phaseStats[2] != null || phaseStats[3] != null) {
                Double avgEstPickup = phaseStats[2] != null ? ((Number) phaseStats[2]).doubleValue() : null;
                Double avgActPickup = phaseStats[3] != null ? ((Number) phaseStats[3]).doubleValue() : null;
                dto.setPickupAccuracy(EtaAccuracyDto.PhaseAccuracy.builder()
                        .averageEstimatedMinutes(avgEstPickup)
                        .averageActualMinutes(avgActPickup)
                        .averageErrorMinutes(avgEstPickup != null && avgActPickup != null ? avgActPickup - avgEstPickup : null)
                        .build());
            }
            if (phaseStats[4] != null || phaseStats[5] != null) {
                Double avgEstDelivery = phaseStats[4] != null ? ((Number) phaseStats[4]).doubleValue() : null;
                Double avgActDelivery = phaseStats[5] != null ? ((Number) phaseStats[5]).doubleValue() : null;
                dto.setDeliveryAccuracy(EtaAccuracyDto.PhaseAccuracy.builder()
                        .averageEstimatedMinutes(avgEstDelivery)
                        .averageActualMinutes(avgActDelivery)
                        .averageErrorMinutes(avgEstDelivery != null && avgActDelivery != null ? avgActDelivery - avgEstDelivery : null)
                        .build());
            }
        }

        // Accuracy by restaurant
        List<Object[]> restaurantAccuracy = etaHistoryRepository.getAccuracyByRestaurant(startTime, endTime);
        List<EtaAccuracyDto.RestaurantEtaAccuracy> byRestaurant = new ArrayList<>();
        for (Object[] row : restaurantAccuracy) {
            byRestaurant.add(EtaAccuracyDto.RestaurantEtaAccuracy.builder()
                    .restaurantId(((Number) row[0]).longValue())
                    .totalDeliveries(((Number) row[1]).longValue())
                    .averageErrorMinutes(row[2] != null ? ((Number) row[2]).doubleValue() : null)
                    .accuracyRate5Min(row[3] != null ? ((Number) row[3]).doubleValue() : null)
                    .build());
        }
        dto.setByRestaurant(byRestaurant);

        // Daily metrics
        List<Object[]> dailyMetricsData = etaHistoryRepository.getDailyMetrics(startTime, endTime);
        List<EtaAccuracyDto.DailyEtaMetrics> dailyMetrics = new ArrayList<>();
        for (Object[] row : dailyMetricsData) {
            dailyMetrics.add(EtaAccuracyDto.DailyEtaMetrics.builder()
                    .date(((Date) row[0]).toLocalDate().atStartOfDay())
                    .totalDeliveries(((Number) row[1]).longValue())
                    .avgErrorMinutes(row[2] != null ? ((Number) row[2]).doubleValue() : null)
                    .avgAbsErrorMinutes(row[3] != null ? ((Number) row[3]).doubleValue() : null)
                    .within5Min(((Number) row[4]).longValue())
                    .lateDeliveries(((Number) row[5]).longValue())
                    .earlyDeliveries(((Number) row[6]).longValue())
                    .build());
        }
        dto.setDailyMetrics(dailyMetrics);

        dto.calculateDerivedMetrics();
        return dto;
    }

    @Override
    @Cacheable(value = CACHE_OPERATIONS_SUMMARY, key = "#startDate + '-' + #endDate")
    public OperationsSummaryDto getOperationsSummary(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating operations summary from {} to {}", startDate, endDate);

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        OperationsSummaryDto dto = OperationsSummaryDto.builder()
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();

        // Fulfillment times
        dto.setAvgAcceptanceTimeMinutes(orderRepository.getAverageAcceptanceTimeMinutes(startTime, endTime));
        dto.setAvgPreparationTimeMinutes(orderRepository.getAveragePreparationTimeMinutes(startTime, endTime));
        dto.setAvgDeliveryTimeMinutes(orderRepository.getAverageDeliveryTimeMinutes(startTime, endTime));
        dto.setAvgFulfillmentTimeMinutes(orderRepository.getAverageTotalFulfillmentTimeMinutes(startTime, endTime));

        // Success metrics
        Long totalOrders = orderRepository.countTotalOrders(startTime, endTime);
        Long successfulDeliveries = orderRepository.countSuccessfulDeliveries(startTime, endTime);
        Long onTimeDeliveries = orderRepository.countOnTimeDeliveries(startTime, endTime);

        dto.setTotalOrders(totalOrders != null ? totalOrders : 0L);
        dto.setTotalDeliveriesCompleted(successfulDeliveries != null ? successfulDeliveries : 0L);

        if (totalOrders != null && totalOrders > 0) {
            if (successfulDeliveries != null) {
                dto.setDeliverySuccessRate(successfulDeliveries * 100.0 / totalOrders);
            }
            if (onTimeDeliveries != null) {
                dto.setOnTimeDeliveryRate(onTimeDeliveries * 100.0 / totalOrders);
            }
            dto.setCancellationRate(100.0 - (successfulDeliveries != null ? successfulDeliveries * 100.0 / totalOrders : 0.0));
        }

        // ETA accuracy
        Object[] etaStats = etaHistoryRepository.getOverallEtaStats(startTime, endTime);
        if (etaStats != null && etaStats[1] != null) {
            dto.setAvgEtaErrorMinutes(((Number) etaStats[1]).doubleValue());
        }
        Long within5 = etaHistoryRepository.countWithinMinutes(5, startTime, endTime);
        Long etaTotal = etaStats != null && etaStats[2] != null ? ((Number) etaStats[2]).longValue() : 0L;
        if (etaTotal > 0 && within5 != null) {
            dto.setEtaAccuracyRate(within5 * 100.0 / etaTotal);
        }

        // Restaurant/courier counts
        Long activeCouriers = availabilityEventRepository.countActiveCouriers(startTime, endTime);
        dto.setActiveCouriers(activeCouriers != null ? activeCouriers : 0L);

        // Average active hours
        Double avgActiveHours = availabilityEventRepository.getAverageActiveHoursPerDay(startTime, endTime);
        if (avgActiveHours != null) {
            dto.setAvgCourierUtilization(avgActiveHours / 24.0 * 100.0);
        }

        // Calculate orders per hour
        long hours = java.time.Duration.between(startTime, endTime).toHours();
        if (hours > 0 && totalOrders != null) {
            dto.setOrdersPerHour(totalOrders.doubleValue() / hours);
        }

        return dto;
    }

    @Override
    @CacheEvict(value = {CACHE_ORDER_FULFILLMENT, CACHE_RESTAURANT_PERFORMANCE, CACHE_COURIER_PERFORMANCE,
            CACHE_DELIVERY_SUCCESS, CACHE_ETA_ACCURACY, CACHE_OPERATIONS_SUMMARY}, allEntries = true)
    public void evictAllCaches() {
        log.info("Evicting all operations analytics caches");
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

package com.fooddelivery.admin.dashboard.collector;

import com.fooddelivery.admin.dashboard.dto.CourierMetricsDto;
import com.fooddelivery.admin.dashboard.dto.CourierMetricsDto.*;
import com.fooddelivery.admin.dashboard.dto.DashboardFilterRequest;
import com.fooddelivery.admin.dashboard.repository.DashboardCourierRepository;
import com.fooddelivery.admin.dashboard.repository.DashboardOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for courier metrics and performance data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourierCollector {

    private final DashboardCourierRepository courierRepository;
    private final DashboardOrderRepository orderRepository;

    // Active courier threshold - last ping within 5 minutes
    private static final int ACTIVE_PING_THRESHOLD_MINUTES = 5;

    /**
     * Collect comprehensive courier metrics.
     */
    public CourierMetricsDto collectCourierMetrics(DashboardFilterRequest filter) {
        log.debug("Collecting courier metrics with filter: {}", filter);

        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        LocalDateTime activeThreshold = LocalDateTime.now().minusMinutes(ACTIVE_PING_THRESHOLD_MINUTES);
        Pageable pageable = createPageable(filter);

        // Get counts
        Long totalCouriers = courierRepository.countTotalCouriers();
        Long activeCouriers = courierRepository.countActiveCouriers(activeThreshold);
        Long onDeliveryCouriers = courierRepository.countCouriersOnDelivery();
        Long availableCouriers = courierRepository.countAvailableCouriers(activeThreshold);
        Long offlineCouriers = totalCouriers - activeCouriers;

        // Get performance summary
        CourierPerformanceSummaryDto performanceSummary = collectPerformanceSummary(startDate, endDate);

        // Get courier details
        List<CourierDetailDto> courierDetails = collectCourierDetails(filter, pageable, activeThreshold);

        // Get courier locations
        List<CourierLocationDto> courierLocations = collectCourierLocations(activeThreshold);

        // Get status breakdown
        Map<String, Long> statusBreakdown = collectStatusBreakdown(activeThreshold);

        // Get availability heatmap data
        Map<String, Map<Integer, Long>> availabilityHeatmap = collectAvailabilityHeatmap(startDate, endDate);

        // Get top performers
        List<CourierDetailDto> topPerformers = collectTopPerformers(startDate, endDate, 10);

        // Get vehicle type distribution
        Map<String, Long> vehicleDistribution = collectVehicleDistribution();

        return CourierMetricsDto.builder()
                .totalCouriers(totalCouriers)
                .activeCouriers(activeCouriers)
                .onDeliveryCouriers(onDeliveryCouriers)
                .availableCouriers(availableCouriers)
                .offlineCouriers(offlineCouriers)
                .activePercentage(calculatePercentage(activeCouriers, totalCouriers))
                .utilizationRate(calculatePercentage(onDeliveryCouriers, activeCouriers))
                .performanceSummary(performanceSummary)
                .couriers(courierDetails)
                .courierLocations(courierLocations)
                .statusBreakdown(statusBreakdown)
                .availabilityHeatmap(availabilityHeatmap)
                .topPerformers(topPerformers)
                .vehicleDistribution(vehicleDistribution)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Collect performance summary across all couriers.
     */
    private CourierPerformanceSummaryDto collectPerformanceSummary(LocalDateTime startDate, LocalDateTime endDate) {
        Double avgDeliveryTime = courierRepository.avgDeliveryTime();
        Double avgRating = courierRepository.avgCourierRating();
        Long totalDeliveries = courierRepository.countTotalDeliveries();
        Long onTimeDeliveries = courierRepository.countOnTimeDeliveries();
        Double onTimeRate = totalDeliveries > 0
                ? (onTimeDeliveries.doubleValue() / totalDeliveries) * 100
                : 100.0;

        Double avgDeliveriesPerCourier = courierRepository.avgDeliveriesPerCourier();
        Double avgDistancePerDelivery = courierRepository.avgDistancePerDelivery();

        return CourierPerformanceSummaryDto.builder()
                .avgDeliveryTimeMinutes(roundToTwoDecimals(avgDeliveryTime))
                .avgRating(toBigDecimal(avgRating))
                .totalDeliveriesToday(totalDeliveries)
                .onTimeDeliveryRate(roundToTwoDecimals(onTimeRate))
                .avgDeliveriesPerCourier(roundToTwoDecimals(avgDeliveriesPerCourier))
                .avgDistancePerDeliveryKm(roundToTwoDecimals(avgDistancePerDelivery))
                .build();
    }

    /**
     * Collect detailed courier information.
     */
    private List<CourierDetailDto> collectCourierDetails(DashboardFilterRequest filter,
                                                          Pageable pageable,
                                                          LocalDateTime activeThreshold) {
        List<Object[]> courierData;

        if (filter.getCourierIds() != null && !filter.getCourierIds().isEmpty()) {
            courierData = courierRepository.findCourierDetailsFiltered(
                    filter.getCourierIds(), activeThreshold, pageable);
        } else {
            courierData = courierRepository.findAllCourierDetails(activeThreshold, pageable);
        }

        return courierData.stream()
                .map(this::mapToCourierDetail)
                .collect(Collectors.toList());
    }

    /**
     * Map raw query result to CourierDetailDto.
     * Query returns: id, status, averageRating, totalDeliveries, vehicleType,
     *                currentLat, currentLng, locationUpdatedAt, currentOrderCount, isActive
     */
    private CourierDetailDto mapToCourierDetail(Object[] row) {
        int idx = 0;
        Long courierId = ((Number) row[idx++]).longValue();
        String status = row[idx] != null ? row[idx].toString() : "UNKNOWN";
        idx++;
        Double rating = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
        idx++;
        Integer totalDeliveries = row[idx] != null ? ((Number) row[idx]).intValue() : 0;
        idx++;
        String vehicleType = row[idx] != null ? row[idx].toString() : "Unknown";
        idx++;
        Double latitude = row[idx] != null ? ((Number) row[idx]).doubleValue() : null;
        idx++;
        Double longitude = row[idx] != null ? ((Number) row[idx]).doubleValue() : null;
        idx++;
        LocalDateTime lastPingAt = row[idx] != null ? (LocalDateTime) row[idx] : null;
        idx++;
        Integer currentOrderCount = row[idx] != null ? ((Number) row[idx]).intValue() : 0;
        idx++;
        Boolean isActive = row[idx] != null ? (Boolean) row[idx] : false;

        return CourierDetailDto.builder()
                .courierId(courierId)
                .name("Courier " + courierId) // Name not available from entity
                .status(status)
                .isActive(isActive)
                .rating(roundToTwoDecimals(rating))
                .deliveriesToday((long) totalDeliveries)
                .avgDeliveryTimeMinutes(0.0) // Not tracked on entity
                .onTimeDeliveryRate(100.0) // Not tracked on entity
                .vehicleType(vehicleType)
                .currentLatitude(latitude)
                .currentLongitude(longitude)
                .lastLocationPingAt(lastPingAt)
                .currentOrderId(currentOrderCount > 0 ? (long) currentOrderCount : null)
                .performanceScore(calculatePerformanceScore(rating, 100.0, 0.0))
                .build();
    }

    /**
     * Calculate performance score for courier.
     */
    private Double calculatePerformanceScore(Double rating, Double onTimeRate, Double avgDeliveryTime) {
        // Rating contributes 35% (normalized from 5-point scale)
        double ratingScore = (rating / 5.0) * 35;

        // On-time rate contributes 35%
        double onTimeScore = (onTimeRate / 100.0) * 35;

        // Delivery time contributes 30% (lower is better, assuming 45 min is baseline)
        double timeScore = Math.max(0, (1 - (avgDeliveryTime / 60.0))) * 30;

        return roundToTwoDecimals(ratingScore + onTimeScore + timeScore);
    }

    /**
     * Collect current courier locations.
     * Query returns: id, currentLat, currentLng, status, currentOrderCount, locationUpdatedAt
     */
    private List<CourierLocationDto> collectCourierLocations(LocalDateTime activeThreshold) {
        List<Object[]> locationData = courierRepository.findActiveCourierLocations(activeThreshold);

        return locationData.stream()
                .map(row -> {
                    int idx = 0;
                    Long courierId = ((Number) row[idx++]).longValue();
                    Double latitude = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
                    idx++;
                    Double longitude = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
                    idx++;
                    String status = row[idx] != null ? row[idx].toString() : "UNKNOWN";
                    idx++;
                    Integer currentOrderCount = row[idx] != null ? ((Number) row[idx]).intValue() : 0;
                    idx++;
                    LocalDateTime lastPingAt = row[idx] != null ? (LocalDateTime) row[idx] : null;

                    return CourierLocationDto.builder()
                            .courierId(courierId)
                            .courierName("Courier " + courierId)
                            .latitude(latitude)
                            .longitude(longitude)
                            .status(status)
                            .currentOrderId(currentOrderCount > 0 ? (long) currentOrderCount : null)
                            .lastPingAt(lastPingAt)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Collect status breakdown.
     */
    private Map<String, Long> collectStatusBreakdown(LocalDateTime activeThreshold) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        breakdown.put("AVAILABLE", courierRepository.countAvailableCouriers(activeThreshold));
        breakdown.put("ON_DELIVERY", courierRepository.countCouriersOnDelivery());
        breakdown.put("RETURNING", courierRepository.countCouriersReturning());
        breakdown.put("ON_BREAK", courierRepository.countCouriersOnBreak());
        breakdown.put("OFFLINE", courierRepository.countOfflineCouriers(activeThreshold));
        return breakdown;
    }

    /**
     * Collect availability heatmap data.
     * Returns a map of day -> hour -> count of available couriers.
     */
    private Map<String, Map<Integer, Long>> collectAvailabilityHeatmap(LocalDateTime startDate,
                                                                        LocalDateTime endDate) {
        List<Object[]> heatmapData = courierRepository.getCourierAvailabilityByHour(startDate, endDate);

        Map<String, Map<Integer, Long>> heatmap = new LinkedHashMap<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        // Initialize with zeros
        for (String day : days) {
            Map<Integer, Long> hourMap = new LinkedHashMap<>();
            for (int hour = 0; hour < 24; hour++) {
                hourMap.put(hour, 0L);
            }
            heatmap.put(day, hourMap);
        }

        // Fill in actual data
        for (Object[] row : heatmapData) {
            Integer dayOfWeek = ((Number) row[0]).intValue();
            Integer hour = ((Number) row[1]).intValue();
            Long count = ((Number) row[2]).longValue();

            String dayName = days[Math.min(dayOfWeek - 1, 6)];
            heatmap.get(dayName).put(hour, count);
        }

        return heatmap;
    }

    /**
     * Collect top performing couriers.
     */
    private List<CourierDetailDto> collectTopPerformers(LocalDateTime startDate,
                                                         LocalDateTime endDate, int limit) {
        LocalDateTime activeThreshold = LocalDateTime.now().minusMinutes(ACTIVE_PING_THRESHOLD_MINUTES);
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> topPerformers = courierRepository.findTopPerformingCouriers(activeThreshold, pageable);

        return topPerformers.stream()
                .map(this::mapToCourierDetail)
                .collect(Collectors.toList());
    }

    /**
     * Collect vehicle type distribution.
     */
    private Map<String, Long> collectVehicleDistribution() {
        List<Object[]> vehicleData = courierRepository.countByVehicleType();
        Map<String, Long> distribution = new LinkedHashMap<>();

        for (Object[] row : vehicleData) {
            String vehicleType = row[0] != null ? row[0].toString() : "Other";
            Long count = ((Number) row[1]).longValue();
            distribution.put(vehicleType, count);
        }

        return distribution;
    }

    /**
     * Create pageable from filter.
     */
    private Pageable createPageable(DashboardFilterRequest filter) {
        int page = filter.getPageNumber();
        int size = filter.getPageSize();
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "id";
        String sortDir = filter.getSortDirection() != null ? filter.getSortDirection() : "ASC";

        Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return PageRequest.of(page, size, sort);
    }

    /**
     * Calculate percentage.
     */
    private Double calculatePercentage(Long part, Long total) {
        if (total == null || total == 0) {
            return 0.0;
        }
        return roundToTwoDecimals((part.doubleValue() / total) * 100);
    }

    /**
     * Round to two decimal places.
     */
    private Double roundToTwoDecimals(Double value) {
        if (value == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Convert Double to BigDecimal with two decimal places.
     */
    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}

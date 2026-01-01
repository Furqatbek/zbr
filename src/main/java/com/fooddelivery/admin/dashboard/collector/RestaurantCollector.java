package com.fooddelivery.admin.dashboard.collector;

import com.fooddelivery.admin.dashboard.dto.DashboardFilterRequest;
import com.fooddelivery.admin.dashboard.dto.RestaurantMetricsDto;
import com.fooddelivery.admin.dashboard.dto.RestaurantMetricsDto.*;
import com.fooddelivery.admin.dashboard.repository.DashboardRestaurantRepository;
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
 * Collector for restaurant metrics and performance data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantCollector {

    private final DashboardRestaurantRepository restaurantRepository;
    private final DashboardOrderRepository orderRepository;

    /**
     * Collect comprehensive restaurant metrics.
     */
    public RestaurantMetricsDto collectRestaurantMetrics(DashboardFilterRequest filter) {
        log.debug("Collecting restaurant metrics with filter: {}", filter);

        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        Pageable pageable = createPageable(filter);

        // Get counts
        Long totalRestaurants = restaurantRepository.countTotalRestaurants();
        Long onlineRestaurants = restaurantRepository.countOnlineRestaurants();
        Long offlineRestaurants = totalRestaurants - onlineRestaurants;
        Long busyRestaurants = restaurantRepository.countBusyRestaurants();

        // Get performance summary
        RestaurantPerformanceSummaryDto performanceSummary = collectPerformanceSummary(startDate, endDate);

        // Get restaurant details
        List<RestaurantDetailDto> restaurantDetails = collectRestaurantDetails(filter, pageable);

        // Get status breakdown
        Map<String, Long> statusBreakdown = collectStatusBreakdown();

        // Get cuisine distribution
        Map<String, Long> cuisineDistribution = collectCuisineDistribution();

        // Get top performers
        List<RestaurantDetailDto> topPerformers = collectTopPerformers(startDate, endDate, 10);

        // Get underperformers (restaurants needing attention)
        List<RestaurantDetailDto> underperformers = collectUnderperformers(startDate, endDate, 10);

        // Get geographic distribution
        Map<String, Long> geographicDistribution = collectGeographicDistribution();

        return RestaurantMetricsDto.builder()
                .totalRestaurants(totalRestaurants)
                .onlineRestaurants(onlineRestaurants)
                .offlineRestaurants(offlineRestaurants)
                .busyRestaurants(busyRestaurants)
                .onlinePercentage(calculatePercentage(onlineRestaurants, totalRestaurants))
                .performanceSummary(performanceSummary)
                .restaurants(restaurantDetails)
                .statusBreakdown(statusBreakdown)
                .cuisineDistribution(cuisineDistribution)
                .topPerformers(topPerformers)
                .underperformers(underperformers)
                .geographicDistribution(geographicDistribution)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Collect performance summary across all restaurants.
     */
    private RestaurantPerformanceSummaryDto collectPerformanceSummary(LocalDateTime startDate, LocalDateTime endDate) {
        Double avgPrepTime = restaurantRepository.avgPreparationTime(startDate, endDate);
        Double avgAcceptanceLatency = restaurantRepository.avgOrderAcceptanceLatency(startDate, endDate);
        Double avgRating = restaurantRepository.avgRestaurantRating();
        Double avgAcceptanceRate = restaurantRepository.avgOrderAcceptanceRate(startDate, endDate);
        Long totalOrdersProcessed = orderRepository.countOrdersToday(startDate, endDate);

        // Get rejection rate
        Long rejectedOrders = orderRepository.countRejectedOrders(startDate, endDate);
        Double rejectionRate = totalOrdersProcessed > 0
                ? (rejectedOrders.doubleValue() / totalOrdersProcessed) * 100
                : 0.0;

        return RestaurantPerformanceSummaryDto.builder()
                .avgPreparationTimeMinutes(roundToTwoDecimals(avgPrepTime))
                .avgAcceptanceLatencySeconds(roundToTwoDecimals(avgAcceptanceLatency))
                .avgRating(roundToTwoDecimals(avgRating))
                .avgAcceptanceRate(roundToTwoDecimals(avgAcceptanceRate))
                .totalOrdersProcessed(totalOrdersProcessed)
                .rejectionRate(roundToTwoDecimals(rejectionRate))
                .build();
    }

    /**
     * Collect detailed restaurant information with performance metrics.
     */
    private List<RestaurantDetailDto> collectRestaurantDetails(DashboardFilterRequest filter, Pageable pageable) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();

        List<Object[]> restaurantData;

        if (filter.getRestaurantIds() != null && !filter.getRestaurantIds().isEmpty()) {
            restaurantData = restaurantRepository.findRestaurantDetailsFiltered(
                    filter.getRestaurantIds(), startDate, endDate, pageable);
        } else {
            restaurantData = restaurantRepository.findAllRestaurantDetails(startDate, endDate, pageable);
        }

        return restaurantData.stream()
                .map(this::mapToRestaurantDetail)
                .collect(Collectors.toList());
    }

    /**
     * Map raw query result to RestaurantDetailDto.
     */
    private RestaurantDetailDto mapToRestaurantDetail(Object[] row) {
        int idx = 0;
        Long restaurantId = ((Number) row[idx++]).longValue();
        String name = (String) row[idx++];
        String status = (String) row[idx++];
        Double rating = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
        idx++;
        Long totalOrders = row[idx] != null ? ((Number) row[idx]).longValue() : 0L;
        idx++;
        Double avgPrepTime = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
        idx++;
        Double acceptanceLatency = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
        idx++;
        Long acceptedOrders = row[idx] != null ? ((Number) row[idx]).longValue() : 0L;
        idx++;
        Long rejectedOrders = row[idx] != null ? ((Number) row[idx]).longValue() : 0L;
        idx++;
        String cuisineType = row[idx] != null ? (String) row[idx] : "Unknown";
        idx++;
        String city = row[idx] != null ? (String) row[idx] : "Unknown";
        idx++;
        LocalDateTime lastOrderAt = row[idx] != null ? (LocalDateTime) row[idx] : null;
        idx++;
        Boolean isOnline = row[idx] != null ? (Boolean) row[idx] : false;

        Double acceptanceRate = (acceptedOrders + rejectedOrders) > 0
                ? (acceptedOrders.doubleValue() / (acceptedOrders + rejectedOrders)) * 100
                : 100.0;

        String statusDisplay = determineRestaurantStatus(isOnline, status);

        return RestaurantDetailDto.builder()
                .restaurantId(restaurantId)
                .name(name)
                .status(statusDisplay)
                .isOnline(isOnline)
                .rating(roundToTwoDecimals(rating))
                .totalOrdersToday(totalOrders)
                .avgPreparationTimeMinutes(roundToTwoDecimals(avgPrepTime))
                .orderAcceptanceLatencySeconds(roundToTwoDecimals(acceptanceLatency))
                .acceptanceRate(roundToTwoDecimals(acceptanceRate))
                .rejectedOrdersToday(rejectedOrders)
                .cuisineType(cuisineType)
                .city(city)
                .lastOrderAt(lastOrderAt)
                .performanceScore(calculatePerformanceScore(rating, acceptanceRate, avgPrepTime))
                .build();
    }

    /**
     * Determine restaurant display status.
     */
    private String determineRestaurantStatus(Boolean isOnline, String status) {
        if (!isOnline) {
            return "OFFLINE";
        }
        if ("BUSY".equalsIgnoreCase(status)) {
            return "BUSY";
        }
        return "ONLINE";
    }

    /**
     * Calculate performance score based on multiple factors.
     * Score is 0-100 scale.
     */
    private Double calculatePerformanceScore(Double rating, Double acceptanceRate, Double avgPrepTime) {
        // Rating contributes 40% (normalized from 5-point scale)
        double ratingScore = (rating / 5.0) * 40;

        // Acceptance rate contributes 30%
        double acceptanceScore = (acceptanceRate / 100.0) * 30;

        // Prep time contributes 30% (lower is better, assuming 30 min is baseline)
        double prepScore = Math.max(0, (1 - (avgPrepTime / 60.0))) * 30;

        return roundToTwoDecimals(ratingScore + acceptanceScore + prepScore);
    }

    /**
     * Collect status breakdown.
     */
    private Map<String, Long> collectStatusBreakdown() {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        breakdown.put("ONLINE", restaurantRepository.countOnlineRestaurants());
        breakdown.put("OFFLINE", restaurantRepository.countOfflineRestaurants());
        breakdown.put("BUSY", restaurantRepository.countBusyRestaurants());
        breakdown.put("TEMPORARILY_CLOSED", restaurantRepository.countTemporarilyClosedRestaurants());
        return breakdown;
    }

    /**
     * Collect cuisine type distribution.
     */
    private Map<String, Long> collectCuisineDistribution() {
        List<Object[]> cuisineData = restaurantRepository.countByCuisineType();
        Map<String, Long> distribution = new LinkedHashMap<>();

        for (Object[] row : cuisineData) {
            String cuisine = row[0] != null ? (String) row[0] : "Other";
            Long count = ((Number) row[1]).longValue();
            distribution.put(cuisine, count);
        }

        return distribution;
    }

    /**
     * Collect top performing restaurants.
     */
    private List<RestaurantDetailDto> collectTopPerformers(LocalDateTime startDate,
                                                           LocalDateTime endDate, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> topPerformers = restaurantRepository.findTopPerformingRestaurants(
                startDate, endDate, pageable);

        return topPerformers.stream()
                .map(this::mapToRestaurantDetail)
                .collect(Collectors.toList());
    }

    /**
     * Collect underperforming restaurants that need attention.
     */
    private List<RestaurantDetailDto> collectUnderperformers(LocalDateTime startDate,
                                                              LocalDateTime endDate, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> underperformers = restaurantRepository.findUnderperformingRestaurants(
                startDate, endDate, 3.0, 70.0, pageable); // rating < 3.0 or acceptance < 70%

        return underperformers.stream()
                .map(this::mapToRestaurantDetail)
                .collect(Collectors.toList());
    }

    /**
     * Collect geographic distribution of restaurants.
     */
    private Map<String, Long> collectGeographicDistribution() {
        List<Object[]> geoData = restaurantRepository.countByCity();
        Map<String, Long> distribution = new LinkedHashMap<>();

        for (Object[] row : geoData) {
            String city = row[0] != null ? (String) row[0] : "Unknown";
            Long count = ((Number) row[1]).longValue();
            distribution.put(city, count);
        }

        return distribution;
    }

    /**
     * Create pageable from filter.
     */
    private Pageable createPageable(DashboardFilterRequest filter) {
        int page = filter.getPageNumber();
        int size = filter.getPageSize();
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "name";
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
}

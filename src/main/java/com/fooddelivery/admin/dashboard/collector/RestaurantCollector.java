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
        Double avgPrepTime = restaurantRepository.avgPreparationTime();
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
                .avgRating(toBigDecimal(avgRating))
                .avgAcceptanceRate(roundToTwoDecimals(avgAcceptanceRate))
                .totalOrdersProcessed(totalOrdersProcessed)
                .rejectionRate(roundToTwoDecimals(rejectionRate))
                .build();
    }

    /**
     * Convert Double to BigDecimal.
     */
    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Collect detailed restaurant information with performance metrics.
     */
    private List<RestaurantDetailDto> collectRestaurantDetails(DashboardFilterRequest filter, Pageable pageable) {
        List<Object[]> restaurantData;

        if (filter.getRestaurantIds() != null && !filter.getRestaurantIds().isEmpty()) {
            restaurantData = restaurantRepository.findRestaurantDetailsFiltered(
                    filter.getRestaurantIds(), pageable);
        } else {
            restaurantData = restaurantRepository.findAllRestaurantDetails(pageable);
        }

        return restaurantData.stream()
                .map(this::mapToRestaurantDetail)
                .collect(Collectors.toList());
    }

    /**
     * Map raw query result to RestaurantDetailDto.
     * Query returns: id, name, status, isOpen, addressLine1, city, totalOrders, averageRating, totalRatings, averagePrepTimeMinutes
     */
    private RestaurantDetailDto mapToRestaurantDetail(Object[] row) {
        int idx = 0;
        Long restaurantId = ((Number) row[idx++]).longValue();
        String name = (String) row[idx++];
        String status = row[idx] != null ? row[idx].toString() : "UNKNOWN";
        idx++;
        Boolean isOnline = row[idx] != null ? (Boolean) row[idx] : false;
        idx++;
        String address = row[idx] != null ? (String) row[idx] : "";
        idx++;
        String city = row[idx] != null ? (String) row[idx] : "Unknown";
        idx++;
        Long totalOrders = row[idx] != null ? ((Number) row[idx]).longValue() : 0L;
        idx++;
        Double rating = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
        idx++;
        Integer totalRatings = row[idx] != null ? ((Number) row[idx]).intValue() : 0;
        idx++;
        Double avgPrepTime = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;

        String statusDisplay = determineRestaurantStatus(isOnline, status);

        return RestaurantDetailDto.builder()
                .restaurantId(restaurantId)
                .name(name)
                .status(statusDisplay)
                .isOnline(isOnline)
                .address(address)
                .city(city)
                .rating(roundToTwoDecimals(rating))
                .totalRatings(totalRatings)
                .totalOrdersToday(totalOrders)
                .avgPreparationTimeMinutes(roundToTwoDecimals(avgPrepTime))
                .acceptanceRate(100.0) // Default since we don't have this data
                .performanceScore(calculatePerformanceScore(rating, 100.0, avgPrepTime))
                .build();
    }

    /**
     * Map Restaurant entity to RestaurantDetailDto.
     */
    private RestaurantDetailDto mapRestaurantToDetail(com.fooddelivery.restaurant.entity.Restaurant restaurant) {
        Double rating = restaurant.getAverageRating() != null
                ? restaurant.getAverageRating().doubleValue() : 0.0;
        Double avgPrepTime = restaurant.getAveragePrepTimeMinutes() != null
                ? restaurant.getAveragePrepTimeMinutes().doubleValue() : 0.0;
        Long totalOrders = restaurant.getTotalOrders() != null
                ? restaurant.getTotalOrders() : 0L;
        Boolean isOnline = restaurant.getIsOpen() != null ? restaurant.getIsOpen() : false;
        String status = restaurant.getStatus() != null ? restaurant.getStatus().name() : "UNKNOWN";

        String statusDisplay = determineRestaurantStatus(isOnline, status);

        return RestaurantDetailDto.builder()
                .restaurantId(restaurant.getId())
                .name(restaurant.getName())
                .status(statusDisplay)
                .isOnline(isOnline)
                .rating(roundToTwoDecimals(rating))
                .totalOrdersToday(totalOrders)
                .avgPreparationTimeMinutes(roundToTwoDecimals(avgPrepTime))
                .cuisineType(null) // Restaurant entity doesn't have cuisineType field
                .city(restaurant.getCity())
                .performanceScore(calculatePerformanceScore(rating, 100.0, avgPrepTime))
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
     * Collect status distribution (formerly cuisine type distribution).
     */
    private Map<String, Long> collectCuisineDistribution() {
        // Return status distribution since cuisineType doesn't exist in the entity
        List<Object[]> statusData = restaurantRepository.countByStatusGrouped();
        Map<String, Long> distribution = new LinkedHashMap<>();

        for (Object[] row : statusData) {
            String status = row[0] != null ? row[0].toString() : "Other";
            Long count = ((Number) row[1]).longValue();
            distribution.put(status, count);
        }

        return distribution;
    }

    /**
     * Collect top performing restaurants.
     */
    private List<RestaurantDetailDto> collectTopPerformers(LocalDateTime startDate,
                                                           LocalDateTime endDate, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<com.fooddelivery.restaurant.entity.Restaurant> topPerformers = restaurantRepository.findTopPerformingRestaurants(pageable);

        return topPerformers.stream()
                .map(this::mapRestaurantToDetail)
                .collect(Collectors.toList());
    }

    /**
     * Collect underperforming restaurants that need attention.
     */
    private List<RestaurantDetailDto> collectUnderperformers(LocalDateTime startDate,
                                                              LocalDateTime endDate, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<com.fooddelivery.restaurant.entity.Restaurant> underperformers = restaurantRepository.findUnderperformingRestaurants(
                3.0, 70.0, pageable); // rating < 3.0 or prepTime > 70min

        return underperformers.stream()
                .map(this::mapRestaurantToDetail)
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

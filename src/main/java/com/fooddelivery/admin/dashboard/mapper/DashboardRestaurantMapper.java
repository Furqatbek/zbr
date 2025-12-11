package com.fooddelivery.admin.dashboard.mapper;

import com.fooddelivery.admin.dashboard.dto.RestaurantMetricsDto.RestaurantDetailDto;
import com.fooddelivery.admin.dashboard.util.DashboardMetricsCalculator;
import org.mapstruct.*;

import java.time.LocalDateTime;

/**
 * MapStruct mapper for restaurant-related dashboard DTOs.
 */
@Mapper(componentModel = "spring", imports = {DashboardMetricsCalculator.class})
public interface DashboardRestaurantMapper {

    /**
     * Map to RestaurantDetailDto.
     */
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "isOnline", source = "isOnline")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "totalOrdersToday", source = "totalOrders")
    @Mapping(target = "avgPreparationTimeMinutes", source = "avgPrepTime")
    @Mapping(target = "orderAcceptanceLatencySeconds", source = "acceptanceLatency")
    @Mapping(target = "acceptanceRate", source = "acceptanceRate")
    @Mapping(target = "rejectedOrdersToday", source = "rejectedOrders")
    @Mapping(target = "cuisineType", source = "cuisineType")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "lastOrderAt", source = "lastOrderAt")
    @Mapping(target = "performanceScore", expression = "java(calculatePerformanceScore(rating, acceptanceRate, avgPrepTime))")
    RestaurantDetailDto toRestaurantDetail(
            Long restaurantId,
            String name,
            String status,
            Boolean isOnline,
            Double rating,
            Long totalOrders,
            Double avgPrepTime,
            Double acceptanceLatency,
            Double acceptanceRate,
            Long rejectedOrders,
            String cuisineType,
            String city,
            LocalDateTime lastOrderAt
    );

    /**
     * Calculate restaurant performance score.
     * Score is 0-100 scale based on:
     * - Rating (40%): normalized from 5-point scale
     * - Acceptance rate (30%): percentage of orders accepted
     * - Prep time (30%): lower is better, normalized against 60min baseline
     */
    default Double calculatePerformanceScore(Double rating, Double acceptanceRate, Double avgPrepTime) {
        if (rating == null) rating = 0.0;
        if (acceptanceRate == null) acceptanceRate = 0.0;
        if (avgPrepTime == null) avgPrepTime = 0.0;

        // Rating contributes 40% (normalized from 5-point scale)
        double ratingScore = (rating / 5.0) * 40;

        // Acceptance rate contributes 30%
        double acceptanceScore = (acceptanceRate / 100.0) * 30;

        // Prep time contributes 30% (lower is better, assuming 60 min is baseline)
        double prepScore = Math.max(0, (1 - (avgPrepTime / 60.0))) * 30;

        return DashboardMetricsCalculator.roundToTwoDecimals(ratingScore + acceptanceScore + prepScore);
    }

    /**
     * Determine restaurant display status.
     */
    default String determineStatus(Boolean isOnline, String status) {
        if (isOnline == null || !isOnline) {
            return "OFFLINE";
        }
        if ("BUSY".equalsIgnoreCase(status)) {
            return "BUSY";
        }
        return "ONLINE";
    }

    /**
     * Round to two decimal places.
     */
    default Double roundValue(Double value) {
        return DashboardMetricsCalculator.roundToTwoDecimals(value);
    }
}

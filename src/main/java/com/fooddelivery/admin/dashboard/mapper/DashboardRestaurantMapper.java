package com.fooddelivery.admin.dashboard.mapper;

import com.fooddelivery.admin.dashboard.dto.RestaurantMetricsDto.RestaurantDetailDto;
import com.fooddelivery.admin.dashboard.util.DashboardMetricsCalculator;
import org.mapstruct.*;

import java.math.BigDecimal;

/**
 * MapStruct mapper for restaurant-related dashboard DTOs.
 * Uses correct property names from RestaurantMetricsDto inner classes.
 */
@Mapper(componentModel = "spring", imports = {DashboardMetricsCalculator.class})
public interface DashboardRestaurantMapper {

    /**
     * Map to RestaurantDetailDto.
     * Property names match RestaurantDetailDto fields.
     */
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "isOpen", source = "isOpen")
    @Mapping(target = "avgRating", source = "rating")
    @Mapping(target = "ordersToday", source = "totalOrders")
    @Mapping(target = "avgPrepTime", source = "avgPrepTime")
    @Mapping(target = "avgAcceptanceLatency", source = "acceptanceLatency")
    @Mapping(target = "acceptanceRate", source = "acceptanceRate")
    @Mapping(target = "ordersRejected", source = "rejectedOrders")
    @Mapping(target = "address", source = "address")
    RestaurantDetailDto toRestaurantDetail(
            Long restaurantId,
            String name,
            String status,
            Boolean isOpen,
            BigDecimal rating,
            Long totalOrders,
            Double avgPrepTime,
            Double acceptanceLatency,
            Double acceptanceRate,
            Long rejectedOrders,
            String address
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
    default String determineStatus(Boolean isOpen, String status) {
        if (isOpen == null || !isOpen) {
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

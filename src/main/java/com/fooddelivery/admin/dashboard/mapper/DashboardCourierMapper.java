package com.fooddelivery.admin.dashboard.mapper;

import com.fooddelivery.admin.dashboard.dto.CourierMetricsDto.CourierDetailDto;
import com.fooddelivery.admin.dashboard.dto.CourierMetricsDto.CourierLocationDto;
import com.fooddelivery.admin.dashboard.util.DashboardMetricsCalculator;
import org.mapstruct.*;

import java.time.LocalDateTime;

/**
 * MapStruct mapper for courier-related dashboard DTOs.
 */
@Mapper(componentModel = "spring", imports = {DashboardMetricsCalculator.class})
public interface DashboardCourierMapper {

    /**
     * Map to CourierDetailDto.
     */
    @Mapping(target = "courierId", source = "courierId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "deliveriesToday", source = "deliveriesToday")
    @Mapping(target = "avgDeliveryTimeToday", source = "avgDeliveryTime")
    @Mapping(target = "vehicleType", source = "vehicleType")
    @Mapping(target = "currentLat", source = "latitude")
    @Mapping(target = "currentLng", source = "longitude")
    @Mapping(target = "locationUpdatedAt", source = "lastPingAt")
    @Mapping(target = "avgRating", source = "rating")
    @Mapping(target = "acceptanceRate", source = "onTimeRate")
    CourierDetailDto toCourierDetail(
            Long courierId,
            String name,
            String status,
            Long deliveriesToday,
            Double avgDeliveryTime,
            Double onTimeRate,
            String vehicleType,
            Double latitude,
            Double longitude,
            LocalDateTime lastPingAt,
            Double rating
    );

    /**
     * Map to CourierLocationDto.
     */
    @Mapping(target = "courierId", source = "courierId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "latitude", source = "latitude")
    @Mapping(target = "longitude", source = "longitude")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "activeOrders", source = "activeOrders")
    @Mapping(target = "lastPingAt", source = "lastPingAt")
    CourierLocationDto toCourierLocation(
            Long courierId,
            String name,
            Double latitude,
            Double longitude,
            String status,
            Integer activeOrders,
            LocalDateTime lastPingAt
    );

    /**
     * Calculate courier performance score.
     * Score is 0-100 scale based on:
     * - Rating (35%): normalized from 5-point scale
     * - On-time rate (35%): percentage of on-time deliveries
     * - Delivery time (30%): lower is better, normalized against 60min baseline
     */
    default Double calculatePerformanceScore(Double rating, Double onTimeRate, Double avgDeliveryTime) {
        if (rating == null) rating = 0.0;
        if (onTimeRate == null) onTimeRate = 0.0;
        if (avgDeliveryTime == null) avgDeliveryTime = 0.0;

        // Rating contributes 35% (normalized from 5-point scale)
        double ratingScore = (rating / 5.0) * 35;

        // On-time rate contributes 35%
        double onTimeScore = (onTimeRate / 100.0) * 35;

        // Delivery time contributes 30% (lower is better, assuming 60 min is baseline)
        double timeScore = Math.max(0, (1 - (avgDeliveryTime / 60.0))) * 30;

        return DashboardMetricsCalculator.roundToTwoDecimals(ratingScore + onTimeScore + timeScore);
    }

    /**
     * Determine if courier is active based on last ping time.
     */
    default Boolean isActive(LocalDateTime lastPingAt, int thresholdMinutes) {
        if (lastPingAt == null) {
            return false;
        }
        return lastPingAt.plusMinutes(thresholdMinutes).isAfter(LocalDateTime.now());
    }

    /**
     * Round to two decimal places.
     */
    default Double roundValue(Double value) {
        return DashboardMetricsCalculator.roundToTwoDecimals(value);
    }
}

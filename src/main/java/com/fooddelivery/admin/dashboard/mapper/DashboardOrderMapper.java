package com.fooddelivery.admin.dashboard.mapper;

import com.fooddelivery.admin.dashboard.dto.ActiveOrdersDto.ActiveOrderItemDto;
import com.fooddelivery.admin.dashboard.dto.StuckOrdersDto.StuckOrderItemDto;
import com.fooddelivery.admin.dashboard.dto.CanceledOrdersDto.CanceledOrderItemDto;
import com.fooddelivery.admin.dashboard.dto.RejectedOrdersDto.RejectedOrderItemDto;
import com.fooddelivery.admin.dashboard.util.DashboardMetricsCalculator;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * MapStruct mapper for order-related dashboard DTOs.
 */
@Mapper(componentModel = "spring", imports = {ChronoUnit.class, LocalDateTime.class, DashboardMetricsCalculator.class})
public interface DashboardOrderMapper {

    /**
     * Map raw query result to ActiveOrderItemDto.
     */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "orderNumber", source = "orderNumber")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "restaurantName", source = "restaurantName")
    @Mapping(target = "courierId", source = "courierId")
    @Mapping(target = "courierName", source = "courierName")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "orderTotal", source = "orderTotal")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "estimatedDeliveryAt", source = "estimatedDeliveryAt")
    @Mapping(target = "deliveryAddress", source = "deliveryAddress")
    ActiveOrderItemDto toActiveOrderItem(
            Long orderId,
            String orderNumber,
            Long customerId,
            String customerName,
            Long restaurantId,
            String restaurantName,
            Long courierId,
            String courierName,
            String status,
            BigDecimal orderTotal,
            LocalDateTime createdAt,
            LocalDateTime estimatedDeliveryAt,
            String deliveryAddress
    );

    /**
     * Map to StuckOrderItemDto with calculated fields.
     */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "orderNumber", source = "orderNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "minutesInCurrentStatus", source = "minutesInStatus")
    @Mapping(target = "thresholdMinutes", source = "threshold")
    @Mapping(target = "orderTotal", source = "orderTotal")
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "restaurantName", source = "restaurantName")
    @Mapping(target = "courierId", source = "courierId")
    @Mapping(target = "courierName", source = "courierName")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "statusChangedAt", source = "statusChangedAt")
    StuckOrderItemDto toStuckOrderItem(
            Long orderId,
            String orderNumber,
            String status,
            Long minutesInStatus,
            Integer threshold,
            BigDecimal orderTotal,
            Long restaurantId,
            String restaurantName,
            Long courierId,
            String courierName,
            LocalDateTime createdAt,
            LocalDateTime statusChangedAt
    );

    /**
     * Map to CanceledOrderItemDto.
     */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "orderNumber", source = "orderNumber")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "restaurantName", source = "restaurantName")
    @Mapping(target = "orderTotal", source = "orderTotal")
    @Mapping(target = "cancelReason", source = "cancelReason")
    @Mapping(target = "cancelledBy", source = "cancelledBy")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "cancelledAt", source = "cancelledAt")
    @Mapping(target = "statusAtCancellation", source = "statusAtCancellation")
    @Mapping(target = "refundAmount", source = "refundAmount")
    @Mapping(target = "refundStatus", source = "refundStatus")
    CanceledOrderItemDto toCanceledOrderItem(
            Long orderId,
            String orderNumber,
            Long customerId,
            String customerName,
            Long restaurantId,
            String restaurantName,
            BigDecimal orderTotal,
            String cancelReason,
            String cancelledBy,
            LocalDateTime createdAt,
            LocalDateTime cancelledAt,
            String statusAtCancellation,
            BigDecimal refundAmount,
            String refundStatus
    );

    /**
     * Map to RejectedOrderItemDto.
     */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "orderNumber", source = "orderNumber")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "restaurantName", source = "restaurantName")
    @Mapping(target = "orderTotal", source = "orderTotal")
    @Mapping(target = "rejectionReason", source = "rejectionReason")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "rejectedAt", source = "rejectedAt")
    @Mapping(target = "wasReassigned", source = "wasReassigned")
    @Mapping(target = "reassignedToRestaurantId", source = "reassignedToRestaurantId")
    RejectedOrderItemDto toRejectedOrderItem(
            Long orderId,
            String orderNumber,
            Long customerId,
            String customerName,
            Long restaurantId,
            String restaurantName,
            BigDecimal orderTotal,
            String rejectionReason,
            LocalDateTime createdAt,
            LocalDateTime rejectedAt,
            Boolean wasReassigned,
            Long reassignedToRestaurantId
    );

    /**
     * Calculate time elapsed since order creation.
     */
    default Long calculateElapsedMinutes(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0L;
        }
        return ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
    }

    /**
     * Format duration to human-readable string.
     */
    default String formatDuration(Long minutes) {
        return DashboardMetricsCalculator.formatDuration(minutes);
    }
}

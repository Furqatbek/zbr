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
 * Uses correct property names from order DTO inner classes.
 */
@Mapper(componentModel = "spring", imports = {ChronoUnit.class, LocalDateTime.class, DashboardMetricsCalculator.class})
public interface DashboardOrderMapper {

    /**
     * Map raw query result to ActiveOrderItemDto.
     * Property names match ActiveOrderItemDto fields.
     */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "externalOrderNo", source = "orderNumber")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "restaurantName", source = "restaurantName")
    @Mapping(target = "courierId", source = "courierId")
    @Mapping(target = "courierName", source = "courierName")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "total", source = "orderTotal")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "estimatedDeliveryTime", source = "estimatedDeliveryAt")
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
     * Property names match StuckOrderItemDto fields.
     */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "externalOrderNo", source = "orderNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "minutesStuck", source = "minutesInStatus")
    @Mapping(target = "expectedTimeMinutes", source = "threshold")
    @Mapping(target = "total", source = "orderTotal")
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "restaurantName", source = "restaurantName")
    @Mapping(target = "courierId", source = "courierId")
    @Mapping(target = "courierName", source = "courierName")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "lastStatusChange", source = "statusChangedAt")
    StuckOrderItemDto toStuckOrderItem(
            Long orderId,
            String orderNumber,
            String status,
            Long minutesInStatus,
            Long threshold,
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
     * Property names match CanceledOrderItemDto fields.
     */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "externalOrderNo", source = "orderNumber")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "restaurantName", source = "restaurantName")
    @Mapping(target = "total", source = "orderTotal")
    @Mapping(target = "cancellationReason", source = "cancelReason")
    @Mapping(target = "canceledBy", source = "cancelledBy")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "canceledAt", source = "cancelledAt")
    @Mapping(target = "statusWhenCanceled", source = "statusAtCancellation")
    @Mapping(target = "refundAmount", source = "refundAmount")
    @Mapping(target = "wasRefunded", source = "wasRefunded")
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
            Boolean wasRefunded
    );

    /**
     * Map to RejectedOrderItemDto.
     * Property names match RejectedOrderItemDto fields.
     */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "externalOrderNo", source = "orderNumber")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "restaurantName", source = "restaurantName")
    @Mapping(target = "total", source = "orderTotal")
    @Mapping(target = "rejectionReason", source = "rejectionReason")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "rejectedAt", source = "rejectedAt")
    @Mapping(target = "wasReassigned", source = "wasReassigned")
    @Mapping(target = "reassignedToId", source = "reassignedToId")
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
            Long reassignedToId
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

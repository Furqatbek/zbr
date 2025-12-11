package com.fooddelivery.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for active orders panel.
 * Lists all orders in active states (PENDING, ACCEPTED, PREPARING, PICKED_UP, IN_TRANSIT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveOrdersDto {

    private Long totalActiveOrders;
    private Long pendingOrders;
    private Long acceptedOrders;
    private Long preparingOrders;
    private Long readyOrders;
    private Long pickedUpOrders;
    private Long inTransitOrders;

    private List<ActiveOrderItemDto> orders;

    // Pagination info
    private Integer currentPage;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;

    // Summary
    private BigDecimal totalActiveOrdersValue;
    private Double avgWaitTimeMinutes;

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveOrderItemDto {
        private Long orderId;
        private String externalOrderNo;
        private String status;
        private String orderType;

        // Customer info
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private String deliveryAddress;

        // Restaurant info
        private Long restaurantId;
        private String restaurantName;

        // Courier info
        private Long courierId;
        private String courierName;
        private String courierPhone;

        // Timing
        private LocalDateTime createdAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime preparingAt;
        private LocalDateTime readyAt;
        private LocalDateTime pickedUpAt;
        private LocalDateTime estimatedDeliveryTime;
        private Long waitTimeMinutes; // Time since creation

        // Financials
        private BigDecimal subtotal;
        private BigDecimal deliveryFee;
        private BigDecimal discount;
        private BigDecimal total;

        // Flags
        private Boolean isDelayed;
        private Boolean isStuck;
        private Boolean requiresAttention;
        private String attentionReason;
    }
}

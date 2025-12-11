package com.fooddelivery.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for canceled orders panel.
 * Orders that were cancelled by customers, system, or operators.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanceledOrdersDto {

    private Long totalCanceledOrders;

    // By canceller
    private Long canceledByCustomer;
    private Long canceledByRestaurant;
    private Long canceledByCourier;
    private Long canceledBySystem;
    private Long canceledByAdmin;

    // By stage when cancelled
    private Long canceledWhilePending;
    private Long canceledWhileAccepted;
    private Long canceledWhilePreparing;
    private Long canceledWhileReady;
    private Long canceledWhileInTransit;

    // Time-based
    private Long canceledToday;
    private Long canceledThisWeek;
    private Long canceledThisMonth;

    // Financial impact
    private BigDecimal totalValueCanceled;
    private BigDecimal refundsIssued;
    private BigDecimal potentialRevenueLost;
    private Double cancellationRate; // percentage of all orders

    // Cancellation reasons breakdown
    private Map<String, Long> cancellationReasonBreakdown;

    // Hourly distribution (for pattern detection)
    private Map<Integer, Long> cancelsByHour;

    private List<CanceledOrderItemDto> orders;

    // Pagination
    private Integer currentPage;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CanceledOrderItemDto {
        private Long orderId;
        private String externalOrderNo;
        private String statusWhenCanceled;
        private String canceledBy; // CUSTOMER, RESTAURANT, COURIER, SYSTEM, ADMIN

        // Restaurant info
        private Long restaurantId;
        private String restaurantName;

        // Customer info
        private Long customerId;
        private String customerName;
        private String customerPhone;

        // Courier info (if assigned)
        private Long courierId;
        private String courierName;

        // Cancellation details
        private String cancellationReason;
        private LocalDateTime canceledAt;
        private Long orderAgeMinutes; // Time from creation to cancellation

        // Order details
        private BigDecimal subtotal;
        private BigDecimal total;
        private Integer itemCount;
        private LocalDateTime createdAt;

        // Refund info
        private Boolean wasRefunded;
        private BigDecimal refundAmount;
        private LocalDateTime refundedAt;

        // Impact assessment
        private Boolean foodWasPrepared;
        private BigDecimal estimatedFoodWaste;
    }
}

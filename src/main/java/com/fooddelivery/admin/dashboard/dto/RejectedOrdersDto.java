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
 * DTO for rejected orders panel.
 * Orders that were rejected by restaurants or couriers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectedOrdersDto {

    private Long totalRejectedOrders;
    private Long rejectedByRestaurant;
    private Long rejectedByCourier;

    // Time-based breakdown
    private Long rejectedToday;
    private Long rejectedThisWeek;
    private Long rejectedThisMonth;

    // Financial impact
    private BigDecimal totalValueLost;
    private BigDecimal avgRejectedOrderValue;

    // Rejection reasons breakdown
    private Map<String, Long> rejectionReasonBreakdown;

    // Top rejecting entities
    private List<RejectorSummaryDto> topRejectingRestaurants;
    private List<RejectorSummaryDto> topRejectingCouriers;

    private List<RejectedOrderItemDto> orders;

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
    public static class RejectedOrderItemDto {
        private Long orderId;
        private String externalOrderNo;
        private String rejectedBy; // RESTAURANT, COURIER

        // Restaurant info
        private Long restaurantId;
        private String restaurantName;

        // Customer info
        private Long customerId;
        private String customerName;
        private String customerPhone;

        // Rejection details
        private String rejectionReason;
        private LocalDateTime rejectedAt;
        private Long timeToRejectionMinutes; // Time from order creation to rejection

        // Order details
        private BigDecimal total;
        private Integer itemCount;
        private LocalDateTime createdAt;

        // Was order reassigned?
        private Boolean wasReassigned;
        private Long reassignedToId;
        private String reassignedToName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectorSummaryDto {
        private Long entityId;
        private String entityName;
        private String entityType; // RESTAURANT, COURIER
        private Long rejectionCount;
        private Double rejectionRate;
        private BigDecimal totalValueRejected;
        private List<String> topRejectionReasons;
    }
}

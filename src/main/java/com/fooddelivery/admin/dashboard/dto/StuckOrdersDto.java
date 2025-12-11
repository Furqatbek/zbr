package com.fooddelivery.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for stuck orders panel.
 * Orders that have exceeded expected time thresholds for their current stage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StuckOrdersDto {

    private Long totalStuckOrders;
    private Long stuckPending;      // > X minutes without acceptance
    private Long stuckAccepted;     // > X minutes without preparation start
    private Long stuckPreparing;    // > X minutes without ready status
    private Long stuckReady;        // > X minutes without pickup
    private Long stuckInTransit;    // > X minutes without delivery

    private List<StuckOrderItemDto> orders;

    // Thresholds used (in minutes)
    private StuckThresholdsDto thresholds;

    // Impact metrics
    private BigDecimal totalValueAtRisk;
    private Long customersAffected;

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StuckOrderItemDto {
        private Long orderId;
        private String externalOrderNo;
        private String status;
        private String stuckStage;

        // Restaurant info
        private Long restaurantId;
        private String restaurantName;
        private String restaurantPhone;

        // Courier info (if assigned)
        private Long courierId;
        private String courierName;
        private String courierPhone;
        private BigDecimal courierLat;
        private BigDecimal courierLng;

        // Customer info
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private String deliveryAddress;

        // Timing
        private LocalDateTime createdAt;
        private LocalDateTime lastStatusChange;
        private Long minutesStuck;
        private Long expectedTimeMinutes;

        // Financials
        private BigDecimal total;

        // Priority
        private String priority; // LOW, MEDIUM, HIGH, CRITICAL
        private String suggestedAction;

        // Additional context
        private Integer previousStuckCount; // How many times this order got stuck
        private Boolean isRepeatCustomer;
        private Double customerLifetimeValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StuckThresholdsDto {
        private Integer pendingThresholdMinutes;
        private Integer acceptedThresholdMinutes;
        private Integer preparingThresholdMinutes;
        private Integer readyThresholdMinutes;
        private Integer inTransitThresholdMinutes;
    }
}

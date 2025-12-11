package com.fooddelivery.analytics.operations.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for Order Fulfillment Time (OFT) metrics.
 * Tracks end-to-end order lifecycle: placedAt → acceptedAt → preparedAt → pickedUpAt → deliveredAt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Order fulfillment time metrics")
public class OrderFulfillmentMetricsDto implements Serializable {

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "External order number")
    private String externalOrderNo;

    @Schema(description = "Restaurant ID")
    private Long restaurantId;

    @Schema(description = "Courier ID")
    private Long courierId;

    @Schema(description = "Order status")
    private String orderStatus;

    // Timestamps
    @Schema(description = "When order was placed")
    private LocalDateTime placedAt;

    @Schema(description = "When restaurant accepted the order")
    private LocalDateTime acceptedAt;

    @Schema(description = "When preparation started")
    private LocalDateTime preparingAt;

    @Schema(description = "When order was ready for pickup")
    private LocalDateTime readyAt;

    @Schema(description = "When courier picked up the order")
    private LocalDateTime pickedUpAt;

    @Schema(description = "When order was delivered")
    private LocalDateTime deliveredAt;

    @Schema(description = "When order was cancelled (if applicable)")
    private LocalDateTime cancelledAt;

    // Time durations in minutes
    @Schema(description = "Total fulfillment time in minutes (placedAt → deliveredAt)", example = "41")
    private Long totalFulfillmentTimeMinutes;

    @Schema(description = "Time to accept order in minutes (placedAt → acceptedAt)", example = "3")
    private Long acceptanceTimeMinutes;

    @Schema(description = "Preparation time in minutes (acceptedAt → readyAt)", example = "15")
    private Long preparationTimeMinutes;

    @Schema(description = "Wait time for courier in minutes (readyAt → pickedUpAt)", example = "8")
    private Long pickupWaitTimeMinutes;

    @Schema(description = "Delivery time in minutes (pickedUpAt → deliveredAt)", example = "15")
    private Long deliveryTimeMinutes;

    // ETA comparison
    @Schema(description = "Estimated delivery time that was shown to customer")
    private LocalDateTime estimatedDeliveryTime;

    @Schema(description = "Estimated prep time in minutes")
    private Integer estimatedPrepTimeMinutes;

    @Schema(description = "Whether delivery was on time (within 5 min of estimate)")
    private Boolean onTime;

    @Schema(description = "Difference from estimate in minutes (positive = late)")
    private Long minutesFromEstimate;

    /**
     * Calculate all time durations from timestamps.
     */
    public void calculateDurations() {
        if (placedAt != null && deliveredAt != null) {
            this.totalFulfillmentTimeMinutes = java.time.Duration.between(placedAt, deliveredAt).toMinutes();
        }

        if (placedAt != null && acceptedAt != null) {
            this.acceptanceTimeMinutes = java.time.Duration.between(placedAt, acceptedAt).toMinutes();
        }

        if (acceptedAt != null && readyAt != null) {
            this.preparationTimeMinutes = java.time.Duration.between(acceptedAt, readyAt).toMinutes();
        }

        if (readyAt != null && pickedUpAt != null) {
            this.pickupWaitTimeMinutes = java.time.Duration.between(readyAt, pickedUpAt).toMinutes();
        }

        if (pickedUpAt != null && deliveredAt != null) {
            this.deliveryTimeMinutes = java.time.Duration.between(pickedUpAt, deliveredAt).toMinutes();
        }

        // ETA accuracy
        if (estimatedDeliveryTime != null && deliveredAt != null) {
            this.minutesFromEstimate = java.time.Duration.between(estimatedDeliveryTime, deliveredAt).toMinutes();
            this.onTime = Math.abs(this.minutesFromEstimate) <= 5;
        }
    }
}

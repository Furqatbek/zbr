package com.fooddelivery.analytics.operations.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for a summary of all operations metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Operations analytics summary")
public class OperationsSummaryDto implements Serializable {

    @Schema(description = "Analysis period start")
    private LocalDateTime periodStart;

    @Schema(description = "Analysis period end")
    private LocalDateTime periodEnd;

    // Order fulfillment summary
    @Schema(description = "Average total fulfillment time (minutes)")
    private Double avgFulfillmentTimeMinutes;

    @Schema(description = "Average acceptance time (minutes)")
    private Double avgAcceptanceTimeMinutes;

    @Schema(description = "Average preparation time (minutes)")
    private Double avgPreparationTimeMinutes;

    @Schema(description = "Average delivery time (minutes)")
    private Double avgDeliveryTimeMinutes;

    // Success metrics
    @Schema(description = "Overall delivery success rate (%)")
    private Double deliverySuccessRate;

    @Schema(description = "On-time delivery rate (%)")
    private Double onTimeDeliveryRate;

    @Schema(description = "Cancellation rate (%)")
    private Double cancellationRate;

    // ETA accuracy
    @Schema(description = "Average ETA error (minutes)")
    private Double avgEtaErrorMinutes;

    @Schema(description = "ETA accuracy rate (within 5 min) %")
    private Double etaAccuracyRate;

    // Restaurant metrics
    @Schema(description = "Active restaurants")
    private Long activeRestaurants;

    @Schema(description = "Average restaurant acceptance rate (%)")
    private Double avgRestaurantAcceptanceRate;

    @Schema(description = "Average restaurant uptime (%)")
    private Double avgRestaurantUptime;

    // Courier metrics
    @Schema(description = "Active couriers")
    private Long activeCouriers;

    @Schema(description = "Average courier acceptance rate (%)")
    private Double avgCourierAcceptanceRate;

    @Schema(description = "Average courier utilization (%)")
    private Double avgCourierUtilization;

    // Volume metrics
    @Schema(description = "Total orders in period")
    private Long totalOrders;

    @Schema(description = "Total deliveries completed")
    private Long totalDeliveriesCompleted;

    @Schema(description = "Orders per hour (average)")
    private Double ordersPerHour;

    @Schema(description = "Peak hour")
    private Integer peakHour;

    @Schema(description = "Orders during peak hour")
    private Long peakHourOrders;
}

package com.fooddelivery.order.dto;

import com.fooddelivery.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating order status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update order status request")
public class UpdateOrderStatusRequest {

    @Schema(description = "New order status", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Status is required")
    private OrderStatus status;

    @Schema(description = "Estimated preparation time in minutes (when accepting)")
    private Integer estimatedPrepTimeMinutes;

    @Schema(description = "Notes/reason for status change")
    private String notes;
}

package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for cancelling an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cancel order request")
public class CancelOrderRequest {

    @Schema(description = "Cancellation reason", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    @Schema(description = "Request refund")
    @Builder.Default
    private Boolean requestRefund = true;
}

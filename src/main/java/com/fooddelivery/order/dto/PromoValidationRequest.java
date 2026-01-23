package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for validating a promo code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Promo code validation request")
public class PromoValidationRequest {

    @NotBlank(message = "Promo code is required")
    @Schema(description = "The promo code to validate")
    private String promoCode;

    @NotNull(message = "Restaurant ID is required")
    @Schema(description = "Restaurant ID for the order")
    private Long restaurantId;

    @NotNull(message = "Subtotal is required")
    @Schema(description = "Order subtotal before discount")
    private BigDecimal subtotal;
}

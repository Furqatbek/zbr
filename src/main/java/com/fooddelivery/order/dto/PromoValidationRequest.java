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

    /**
     * The delivery fee this order would carry, if known.
     *
     * <p>Only a FREE_DELIVERY code needs it, and only it can say what such a code
     * is worth: the discount IS the fee, which varies with distance. Without it
     * this endpoint would answer "valid, you save 0" for a code that waives a
     * real 8 000 — a saving of zero displayed for a working promotion.
     *
     * <p>Optional, so existing callers are unaffected. Omit it and a
     * FREE_DELIVERY code comes back with no discountAmount at all, which is the
     * honest answer to "how much?" when nobody has said what delivery costs.
     */
    @Schema(description = "Delivery fee for this order, so a FREE_DELIVERY code can be priced",
            example = "8000.00")
    private BigDecimal deliveryFee;
}

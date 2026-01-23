package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for promo code validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Promo code validation response")
public class PromoValidationResponse {

    @Schema(description = "Whether the promo code is valid")
    private boolean valid;

    @Schema(description = "Promo code")
    private String promoCode;

    @Schema(description = "Discount type (PERCENTAGE or FIXED)")
    private String discountType;

    @Schema(description = "Discount value (percentage 0-100 or fixed amount)")
    private BigDecimal discountValue;

    @Schema(description = "Calculated discount amount")
    private BigDecimal discountAmount;

    @Schema(description = "New total after discount")
    private BigDecimal newTotal;

    @Schema(description = "Minimum order amount required")
    private BigDecimal minimumOrder;

    @Schema(description = "Maximum discount amount (for percentage discounts)")
    private BigDecimal maxDiscount;

    @Schema(description = "Promo code expiry date")
    private LocalDateTime expiresAt;

    @Schema(description = "Error message if invalid")
    private String errorMessage;
}

package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What this customer is currently owed, so the app never has to guess.
 *
 * <p>The customer app could infer first-order eligibility from an empty order
 * list — one line of code — and deliberately did not, because the fee
 * calculation would still have charged them: the home screen promising "delivery
 * is on us" and the checkout charging for delivery two screens later. They were
 * right to refuse, and this endpoint exists so they never have to.
 *
 * <p>Both shapes the app said it accepts are present: the flat booleans and the
 * nested object carry the same values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Promotions currently available to the signed-in customer")
public class MyPromotionsDto {

    @Schema(description = "True when the server will waive delivery on this customer's next "
            + "delivery order. Never inferred — this is the server saying so.")
    private Boolean firstOrderFreeDeliveryEligible;

    @Schema(description = "True when a free delivery has already been spent")
    private Boolean firstOrderFreeDeliveryUsed;

    @Schema(description = "The same two values nested, for clients reading this shape")
    private FirstOrderFreeDelivery firstOrderFreeDelivery;

    /**
     * How many are waiting, which is not always one: a referrer earns one per
     * person they bring, on top of their own welcome credit.
     */
    @Schema(description = "Free deliveries this customer has waiting", example = "1")
    private Long freeDeliveriesAvailable;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "First-order free delivery")
    public static class FirstOrderFreeDelivery {
        private Boolean eligible;
        private Boolean used;
    }
}

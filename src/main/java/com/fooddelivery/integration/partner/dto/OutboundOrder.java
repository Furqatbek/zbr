package com.fooddelivery.integration.partner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * An order as we send it to a partner's POS.
 *
 * <p>The field names are Restos's, not ours — they published the body they
 * parse, backed by a test in their repository that posts it and passes, so this
 * is a contract rather than a guess. Our earlier draft used our own names and
 * they were kind enough to send a mapping instead of a rejection.
 *
 * <p>Extra fields are ignored rather than refused on their side, so the ones
 * they do not read ({@code subtotal}, {@code deliveryFee}, item names and
 * prices) stay: they make the ticket readable and cost nothing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutboundOrder {

    /** The venue in the PARTNER's numbering, from the venue grant. */
    private Long restaurantId;

    /**
     * Our order reference, {@code FD-YYYYMMDD-XXXXXX}, and their idempotency
     * key. Assigned once at creation and never changed, so a retry after a
     * timeout carries the same value and they collapse it onto the order they
     * already have rather than printing a second ticket.
     */
    private String externalOrderId;

    private String orderType;

    /**
     * {@code PREPAID} or {@code CASH}. Required, and they refuse to guess it —
     * rightly: it decides whether the venue hands over food already paid for or
     * money still to collect.
     */
    private String paymentMode;

    private Customer customer;
    private Delivery delivery;

    private List<Item> items;

    /**
     * The amount that reconciles between the two companies for this ticket:
     * the food at their published prices, plus the delivery fee.
     *
     * <p>NOT what the customer pays — that carries a tax line and a tip they
     * cannot see — and not the food alone, since the venue is owed the delivery
     * fee too. It is deliberately the only figure both sides can compute from
     * the same inputs, their menu, which is what makes a check on it mean
     * something. They answer 409 when it disagrees.
     */
    private BigDecimal expectedTotal;

    /** Not read by Restos. Kept because it makes the ticket legible. */
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Customer {
        private String name;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Delivery {
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String instructions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {

        /** Their product id, from what their menu import stamped on the item. */
        private Long productId;

        /**
         * Their variant id, required for a dish sold by size.
         *
         * <p>Omitting it on a product that has variants is refused with
         * {@code 422 VARIANT_REQUIRED} rather than charged at the base price —
         * which is the behaviour we want, because the alternative is a Large
         * billed as a Regular and cooked Large with nothing on the ticket to
         * show it.
         */
        private Long variantId;

        private Integer quantity;
        private String specialInstructions;

        /** Not read by them. For the ticket. */
        private String name;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}

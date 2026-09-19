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
 * <p><strong>Field names are provisional.</strong> Restos have told us the
 * endpoint and its idempotency semantics but not the body schema, so this is
 * our proposal rather than their contract. It is one class and one mapper, so
 * renaming to match theirs is cheap — but do not assume it is already right.
 *
 * <p>Nulls are omitted. A partner parsing this should not have to distinguish
 * "no delivery address because it is a collection order" from a null they were
 * sent on purpose.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutboundOrder {

    /**
     * Our order reference, {@code FD-YYYYMMDD-XXXXXX}. This is the idempotency
     * key: it is assigned once at creation and never changes, so a retry after
     * a timeout carries the same value and the partner collapses it onto the
     * order they already have rather than printing a second ticket.
     */
    private String externalOrderId;

    /** The venue in the PARTNER's numbering, from the venue grant. */
    private String venueId;

    private String orderType;

    private List<Item> items;

    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal total;

    private String customerName;
    private String customerPhone;

    private String deliveryAddress;
    private String deliveryInstructions;

    /** Free text the customer attached to the whole order. */
    private String notes;

    /** ISO-8601 UTC, e.g. {@code 2026-09-19T10:02:11Z}. */
    private String placedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {

        /**
         * The product id in the PARTNER's system, taken from what their own
         * menu import stamped on the item. Sending our id would be meaningless
         * to their kitchen — and an order whose lines they cannot resolve is
         * one they refuse outright.
         */
        private String productId;

        /** For a human reading the ticket, not for matching. */
        private String name;

        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        private String notes;
    }
}

package com.fooddelivery.order.entity;

/**
 * Whether the food is already paid for when it reaches the customer.
 *
 * <p>Distinct from {@link PaymentStatus}, which says how far a payment has got.
 * This says what KIND of payment to expect, and it has to be known when an
 * order is created rather than when money moves — an integrated POS is handed
 * the ticket immediately and needs to know whether the venue is giving away
 * food already paid for or money still to collect. Restos refuse to guess it,
 * and they are right to: guessing eventually costs somebody a meal.
 */
public enum PaymentMode {

    /** Paid, or to be paid, before delivery. */
    PREPAID,

    /** The courier collects on the doorstep. */
    CASH
}

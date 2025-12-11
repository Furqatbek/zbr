package com.fooddelivery.analytics.financial.model;

/**
 * Status of a payment (payout).
 */
public enum PaymentStatus {
    /**
     * Payment is pending processing.
     */
    PENDING,

    /**
     * Payment is being processed.
     */
    PROCESSING,

    /**
     * Payment completed successfully.
     */
    COMPLETED,

    /**
     * Payment failed.
     */
    FAILED,

    /**
     * Payment was cancelled.
     */
    CANCELLED,

    /**
     * Payment is held (dispute, review).
     */
    HELD,

    /**
     * Payment was refunded/reversed.
     */
    REVERSED
}

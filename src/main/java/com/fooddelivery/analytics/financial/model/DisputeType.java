package com.fooddelivery.analytics.financial.model;

/**
 * Types of payout disputes.
 */
public enum DisputeType {
    /**
     * Missing order in payout.
     */
    MISSING_ORDER,

    /**
     * Incorrect commission calculation.
     */
    INCORRECT_COMMISSION,

    /**
     * Wrong delivery fee.
     */
    INCORRECT_DELIVERY_FEE,

    /**
     * Unauthorized deduction.
     */
    UNAUTHORIZED_DEDUCTION,

    /**
     * Refund dispute.
     */
    REFUND_DISPUTE,

    /**
     * Late payment.
     */
    LATE_PAYMENT,

    /**
     * Technical error.
     */
    TECHNICAL_ERROR,

    /**
     * Other.
     */
    OTHER
}

package com.fooddelivery.analytics.financial.model;

/**
 * Types of courier payments.
 */
public enum CourierPaymentType {
    /**
     * Standard delivery payment.
     */
    DELIVERY,

    /**
     * Bonus payment (performance, milestone, etc.).
     */
    BONUS,

    /**
     * Incentive payment (promotions, challenges).
     */
    INCENTIVE,

    /**
     * Reimbursement (expenses, equipment).
     */
    REIMBURSEMENT,

    /**
     * Adjustment (correction, dispute resolution).
     */
    ADJUSTMENT
}

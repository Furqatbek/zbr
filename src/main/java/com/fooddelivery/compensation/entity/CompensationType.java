package com.fooddelivery.compensation.entity;

/**
 * Types of compensation that can be issued to customers.
 */
public enum CompensationType {
    /**
     * Credit applied to customer account for future orders.
     */
    CREDIT,

    /**
     * Full or partial refund to original payment method.
     */
    REFUND,

    /**
     * Free delivery on next order.
     */
    FREE_DELIVERY,

    /**
     * Credit plus free delivery on next order.
     */
    CREDIT_AND_FREE_DELIVERY,

    /**
     * Full refund plus additional credit.
     */
    REFUND_AND_CREDIT
}

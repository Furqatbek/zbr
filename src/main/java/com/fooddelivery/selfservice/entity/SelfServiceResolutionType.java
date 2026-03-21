package com.fooddelivery.selfservice.entity;

/**
 * Types of self-service resolutions available to customers.
 */
public enum SelfServiceResolutionType {
    /**
     * Auto-refund for small amounts (< $10).
     */
    AUTO_REFUND_SMALL,

    /**
     * Report and refund missing items.
     */
    MISSING_ITEMS,

    /**
     * Cancel order before preparation starts.
     */
    CANCEL_PRE_PREP,

    /**
     * Credit for late delivery.
     */
    LATE_DELIVERY_CREDIT,

    /**
     * Real-time order tracking.
     */
    ORDER_TRACKING
}

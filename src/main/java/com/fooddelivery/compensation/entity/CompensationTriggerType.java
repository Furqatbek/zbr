package com.fooddelivery.compensation.entity;

/**
 * Events that can trigger automatic compensation.
 */
public enum CompensationTriggerType {
    /**
     * Order delivered later than estimated.
     */
    LATE_DELIVERY,

    /**
     * Items missing from the delivered order.
     */
    MISSING_ITEMS,

    /**
     * Order cancelled after restaurant started preparation.
     */
    CANCELLED_AFTER_PREP,

    /**
     * Restaurant rejected the order after customer paid.
     */
    RESTAURANT_REJECTION,

    /**
     * Wrong items delivered.
     */
    WRONG_ITEMS,

    /**
     * Food quality issues reported.
     */
    QUALITY_ISSUE,

    /**
     * Order never delivered.
     */
    ORDER_NOT_DELIVERED,

    /**
     * Manual compensation by support agent.
     */
    MANUAL
}

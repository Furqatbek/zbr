package com.fooddelivery.order.entity;

/**
 * Types of delivery issues that can be reported by couriers.
 */
public enum DeliveryIssueType {
    /**
     * Customer was not available at delivery location.
     */
    CUSTOMER_UNAVAILABLE,

    /**
     * Delivery address was incorrect or incomplete.
     */
    WRONG_ADDRESS,

    /**
     * Restaurant delayed the order preparation.
     */
    RESTAURANT_DELAY,

    /**
     * Courier was involved in an accident.
     */
    ACCIDENT,

    /**
     * Courier experienced vehicle problems.
     */
    VEHICLE_ISSUE,

    /**
     * Other issue not covered by specific types.
     */
    OTHER
}

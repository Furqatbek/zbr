package com.fooddelivery.restaurant.entity;

/**
 * Enumeration of restaurant statuses.
 */
public enum RestaurantStatus {
    /**
     * Restaurant is pending approval.
     */
    PENDING,

    /**
     * Restaurant is active and can receive orders.
     */
    ACTIVE,

    /**
     * Restaurant is temporarily suspended.
     */
    SUSPENDED,

    /**
     * Restaurant is closed permanently.
     */
    CLOSED,

    /**
     * Restaurant application was rejected.
     */
    REJECTED
}

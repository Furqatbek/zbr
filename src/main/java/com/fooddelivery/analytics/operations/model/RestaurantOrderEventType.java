package com.fooddelivery.analytics.operations.model;

/**
 * Types of restaurant order events for tracking restaurant performance.
 */
public enum RestaurantOrderEventType {
    /**
     * Order was received by restaurant.
     */
    RECEIVED,

    /**
     * Restaurant accepted the order.
     */
    ACCEPTED,

    /**
     * Restaurant rejected the order.
     */
    REJECTED,

    /**
     * Restaurant started preparing the order.
     */
    PREPARING_STARTED,

    /**
     * Order preparation completed (ready for pickup).
     */
    READY,

    /**
     * Restaurant cancelled the order.
     */
    CANCELLED,

    /**
     * Restaurant went offline during order.
     */
    WENT_OFFLINE,

    /**
     * Restaurant requested more preparation time.
     */
    PREP_TIME_EXTENDED;

    /**
     * Check if this event indicates order progress.
     */
    public boolean isProgressEvent() {
        return this == ACCEPTED || this == PREPARING_STARTED || this == READY;
    }

    /**
     * Check if this event indicates a problem.
     */
    public boolean isProblemEvent() {
        return this == REJECTED || this == CANCELLED || this == WENT_OFFLINE;
    }
}

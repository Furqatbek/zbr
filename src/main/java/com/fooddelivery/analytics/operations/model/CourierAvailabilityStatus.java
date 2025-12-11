package com.fooddelivery.analytics.operations.model;

/**
 * Courier availability status for tracking active time.
 */
public enum CourierAvailabilityStatus {
    /**
     * Courier is online and available for orders.
     */
    AVAILABLE,

    /**
     * Courier is online but busy with current orders.
     */
    BUSY,

    /**
     * Courier is offline.
     */
    OFFLINE,

    /**
     * Courier is on a break.
     */
    ON_BREAK,

    /**
     * Courier is returning to service area.
     */
    RETURNING;

    /**
     * Check if this status counts as active (working) time.
     */
    public boolean isActiveStatus() {
        return this == AVAILABLE || this == BUSY || this == RETURNING;
    }

    /**
     * Check if courier can receive orders.
     */
    public boolean canReceiveOrders() {
        return this == AVAILABLE;
    }
}

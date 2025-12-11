package com.fooddelivery.courier.entity;

/**
 * Courier status enumeration.
 */
public enum CourierStatus {
    /**
     * Courier is offline and not accepting orders.
     */
    OFFLINE,

    /**
     * Courier is available and ready to accept orders.
     */
    AVAILABLE,

    /**
     * Courier is busy with maximum concurrent orders.
     */
    BUSY,

    /**
     * Courier is on a break.
     */
    ON_BREAK,

    /**
     * Courier account is suspended.
     */
    SUSPENDED,

    /**
     * Courier account is pending approval.
     */
    PENDING_APPROVAL
}

package com.fooddelivery.courier.reassignment.entity;

/**
 * Who initiated the reassignment.
 */
public enum ReassignmentInitiator {
    /**
     * Automatic system reassignment.
     */
    SYSTEM,

    /**
     * Courier requested reassignment.
     */
    COURIER,

    /**
     * Admin manually reassigned.
     */
    ADMIN,

    /**
     * Customer requested reassignment.
     */
    CUSTOMER
}

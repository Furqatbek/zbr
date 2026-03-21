package com.fooddelivery.courier.reassignment.entity;

/**
 * Reasons for reassigning an order to a different courier.
 */
public enum ReassignmentReason {
    /**
     * Courier has been unresponsive for extended period.
     */
    COURIER_UNRESPONSIVE(true),

    /**
     * Courier reported an accident.
     */
    COURIER_ACCIDENT(true),

    /**
     * Courier has vehicle problems.
     */
    VEHICLE_ISSUE(true),

    /**
     * ETA significantly exceeds original estimate.
     */
    ETA_EXCEEDED(false),

    /**
     * Courier manually requested reassignment.
     */
    COURIER_REQUEST(false),

    /**
     * Admin manually reassigned the order.
     */
    ADMIN_OVERRIDE(false),

    /**
     * Customer requested different courier.
     */
    CUSTOMER_REQUEST(false),

    /**
     * Other reason.
     */
    OTHER(false);

    private final boolean requiresCourierOffline;

    ReassignmentReason(boolean requiresCourierOffline) {
        this.requiresCourierOffline = requiresCourierOffline;
    }

    /**
     * Whether this reason should set the courier to offline status.
     */
    public boolean requiresCourierOffline() {
        return requiresCourierOffline;
    }
}

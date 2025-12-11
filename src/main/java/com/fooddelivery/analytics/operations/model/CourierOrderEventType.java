package com.fooddelivery.analytics.operations.model;

/**
 * Types of courier order events for tracking job offers and responses.
 */
public enum CourierOrderEventType {
    /**
     * Order was offered to courier.
     */
    OFFERED,

    /**
     * Courier accepted the order.
     */
    ACCEPTED,

    /**
     * Courier rejected the order.
     */
    REJECTED,

    /**
     * Offer timed out without response.
     */
    TIMED_OUT,

    /**
     * Courier cancelled after accepting.
     */
    CANCELLED,

    /**
     * Courier picked up the order.
     */
    PICKED_UP,

    /**
     * Courier delivered the order.
     */
    DELIVERED,

    /**
     * Delivery was reassigned to another courier.
     */
    REASSIGNED;

    /**
     * Check if this event represents a positive response.
     */
    public boolean isPositiveResponse() {
        return this == ACCEPTED || this == PICKED_UP || this == DELIVERED;
    }

    /**
     * Check if this event represents a negative response.
     */
    public boolean isNegativeResponse() {
        return this == REJECTED || this == TIMED_OUT || this == CANCELLED;
    }

    /**
     * Check if this is an offer event.
     */
    public boolean isOfferEvent() {
        return this == OFFERED;
    }
}

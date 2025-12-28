package com.fooddelivery.order.entity;

/**
 * Order type enumeration.
 */
public enum OrderType {
    /**
     * Delivery to customer address.
     */
    DELIVERY,

    /**
     * Customer picks up from restaurant.
     */
    PICKUP,

    /**
     * Customer picks up from restaurant (alias for PICKUP).
     */
    TAKEAWAY,

    /**
     * Dine-in at restaurant (hall service).
     */
    DINE_IN
}

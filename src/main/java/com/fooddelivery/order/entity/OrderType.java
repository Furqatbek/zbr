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
    TAKEAWAY,

    /**
     * Customer picks up from restaurant (alias for TAKEAWAY).
     */
    PICKUP,

    /**
     * Dine-in at restaurant (hall service).
     */
    DINE_IN
}

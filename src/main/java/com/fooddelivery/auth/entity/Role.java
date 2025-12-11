package com.fooddelivery.auth.entity;

/**
 * Enumeration of user roles in the system.
 */
public enum Role {
    /**
     * System administrator with full access.
     */
    ADMIN,

    /**
     * Platform operator with management access.
     */
    PLATFORM,

    /**
     * Restaurant owner who can manage their restaurants.
     */
    RESTAURANT_OWNER,

    /**
     * Restaurant staff who can manage orders and menu.
     */
    RESTAURANT_STAFF,

    /**
     * Delivery courier who delivers orders.
     */
    COURIER,

    /**
     * Consumer who places orders.
     */
    CONSUMER
}

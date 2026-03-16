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
     * Operations manager with access to operational dashboards.
     */
    OPERATIONS_MANAGER,

    /**
     * Support manager with access to support dashboards.
     */
    SUPPORT_MANAGER,

    /**
     * Support agent handling customer inquiries.
     */
    SUPPORT_AGENT,

    /**
     * Finance manager with access to financial dashboards.
     */
    FINANCE_MANAGER,

    /**
     * Fleet manager overseeing couriers.
     */
    FLEET_MANAGER,

    /**
     * Restaurant manager overseeing restaurants.
     */
    RESTAURANT_MANAGER,

    /**
     * System role for internal operations.
     */
    SYSTEM,

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

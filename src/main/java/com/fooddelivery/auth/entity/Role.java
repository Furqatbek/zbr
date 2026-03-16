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
     * Fraud analyst with access to fraud detection dashboards.
     */
    FRAUD_ANALYST,

    /**
     * Payment analyst with access to payment analytics.
     */
    PAYMENT_ANALYST,

    /**
     * Marketing analyst with access to marketing analytics.
     */
    MARKETING_ANALYST,

    /**
     * Security analyst with access to security logs and analytics.
     */
    SECURITY_ANALYST,

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

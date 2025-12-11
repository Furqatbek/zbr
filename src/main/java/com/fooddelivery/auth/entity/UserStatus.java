package com.fooddelivery.auth.entity;

/**
 * Enumeration of user statuses.
 */
public enum UserStatus {
    /**
     * User is active and can use the system.
     */
    ACTIVE,

    /**
     * User is inactive (soft disabled).
     */
    INACTIVE,

    /**
     * User is suspended (temporary ban).
     */
    SUSPENDED,

    /**
     * User is banned (permanent).
     */
    BANNED,

    /**
     * User registration pending verification.
     */
    PENDING_VERIFICATION,

    /**
     * User account is deleted (soft delete).
     */
    DELETED
}

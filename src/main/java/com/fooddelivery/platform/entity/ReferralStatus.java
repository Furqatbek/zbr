package com.fooddelivery.platform.entity;

/**
 * Referral status enumeration.
 */
public enum ReferralStatus {
    /**
     * Referral code created, awaiting use.
     */
    PENDING,

    /**
     * Referral used, awaiting reward criteria.
     */
    USED,

    /**
     * Referral completed and rewards distributed.
     */
    COMPLETED,

    /**
     * Referral expired.
     */
    EXPIRED,

    /**
     * Referral cancelled.
     */
    CANCELLED
}

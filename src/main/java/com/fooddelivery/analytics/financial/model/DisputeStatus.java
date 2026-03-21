package com.fooddelivery.analytics.financial.model;

/**
 * Status of a payout dispute.
 */
public enum DisputeStatus {
    /**
     * Dispute is open and under review.
     */
    OPEN,

    /**
     * Under investigation.
     */
    INVESTIGATING,

    /**
     * Resolved in favor of claimant.
     */
    RESOLVED_FOR_CLAIMANT,

    /**
     * Resolved in favor of platform.
     */
    RESOLVED_FOR_PLATFORM,

    /**
     * Partially resolved (compromise).
     */
    PARTIALLY_RESOLVED,

    /**
     * Closed without resolution.
     */
    CLOSED,

    /**
     * Escalated to higher authority.
     */
    ESCALATED,

    /**
     * Related to an accident during delivery.
     */
    ACCIDENT
}

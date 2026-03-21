package com.fooddelivery.compensation.entity;

/**
 * Status of a compensation request/log.
 */
public enum CompensationStatus {
    /**
     * Compensation pending processing.
     */
    PENDING,

    /**
     * Compensation approved and being processed.
     */
    APPROVED,

    /**
     * Compensation successfully applied.
     */
    COMPLETED,

    /**
     * Compensation rejected.
     */
    REJECTED,

    /**
     * Compensation processing failed.
     */
    FAILED
}

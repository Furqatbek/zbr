package com.fooddelivery.order.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Payment status enumeration with state machine transitions.
 */
public enum PaymentStatus {
    /**
     * Payment pending / not initiated.
     */
    PENDING,

    /**
     * Payment in progress (e.g., waiting for confirmation).
     */
    PROCESSING,

    /**
     * Payment confirmed and successful.
     */
    CONFIRMED,

    /**
     * Payment failed.
     */
    FAILED,

    /**
     * Payment refunded (partial or full).
     */
    REFUNDED,

    /**
     * Payment cancelled before completion.
     */
    CANCELLED;

    // Define valid transitions
    private static final Set<PaymentStatus> PENDING_TRANSITIONS = EnumSet.of(PROCESSING, CONFIRMED, FAILED, CANCELLED);
    private static final Set<PaymentStatus> PROCESSING_TRANSITIONS = EnumSet.of(CONFIRMED, FAILED, CANCELLED);
    private static final Set<PaymentStatus> CONFIRMED_TRANSITIONS = EnumSet.of(REFUNDED);
    private static final Set<PaymentStatus> FAILED_TRANSITIONS = EnumSet.of(PENDING, CANCELLED);
    private static final Set<PaymentStatus> REFUNDED_TRANSITIONS = EnumSet.noneOf(PaymentStatus.class);
    private static final Set<PaymentStatus> CANCELLED_TRANSITIONS = EnumSet.noneOf(PaymentStatus.class);

    /**
     * Check if transition to the given status is valid.
     */
    public boolean canTransitionTo(PaymentStatus newStatus) {
        return getAllowedTransitions().contains(newStatus);
    }

    /**
     * Get all allowed transitions from current status.
     */
    public Set<PaymentStatus> getAllowedTransitions() {
        return switch (this) {
            case PENDING -> PENDING_TRANSITIONS;
            case PROCESSING -> PROCESSING_TRANSITIONS;
            case CONFIRMED -> CONFIRMED_TRANSITIONS;
            case FAILED -> FAILED_TRANSITIONS;
            case REFUNDED -> REFUNDED_TRANSITIONS;
            case CANCELLED -> CANCELLED_TRANSITIONS;
        };
    }

    /**
     * Check if payment is successful.
     */
    public boolean isSuccessful() {
        return this == CONFIRMED;
    }

    /**
     * Check if payment is terminal (no more transitions possible).
     */
    public boolean isTerminal() {
        return this == REFUNDED || this == CANCELLED;
    }

    /**
     * Check if payment can be refunded.
     */
    public boolean isRefundable() {
        return this == CONFIRMED;
    }
}

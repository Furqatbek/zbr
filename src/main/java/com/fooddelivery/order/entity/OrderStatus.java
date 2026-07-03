package com.fooddelivery.order.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order status enumeration with state machine transitions.
 */
public enum OrderStatus {
    /**
     * Order created, awaiting payment or restaurant acceptance.
     */
    CREATED,

    /**
     * Order accepted by restaurant.
     */
    ACCEPTED,

    /**
     * Order is being prepared.
     */
    PREPARING,

    /**
     * Order is ready for pickup/delivery.
     */
    READY,

    /**
     * Courier has been assigned/accepted the order.
     */
    COURIER_ASSIGNED,

    /**
     * Order picked up by courier (for delivery orders).
     */
    PICKED_UP,

    /**
     * Order is in transit to customer.
     */
    IN_TRANSIT,

    /**
     * Order delivered to customer.
     */
    DELIVERED,

    /**
     * Order completed and closed.
     */
    COMPLETED,

    /**
     * Order was cancelled.
     */
    CANCELLED,

    /**
     * Order was refunded.
     */
    REFUNDED;

    // Define valid transitions for each state
    private static final Set<OrderStatus> CREATED_TRANSITIONS = EnumSet.of(ACCEPTED, PREPARING, CANCELLED);
    private static final Set<OrderStatus> ACCEPTED_TRANSITIONS = EnumSet.of(PREPARING, READY, COURIER_ASSIGNED, CANCELLED);
    private static final Set<OrderStatus> PREPARING_TRANSITIONS = EnumSet.of(READY, COURIER_ASSIGNED, CANCELLED);
    private static final Set<OrderStatus> READY_TRANSITIONS = EnumSet.of(COURIER_ASSIGNED, DELIVERED, COMPLETED, CANCELLED);
    // A courier may accept before the kitchen finishes (COURIER_ASSIGNED with readyAt == null).
    // The restaurant must still be able to advance the order to PREPARING/READY, otherwise the
    // order deadlocks (courier can't pick up until readyAt is set). Hence PREPARING and READY here.
    private static final Set<OrderStatus> COURIER_ASSIGNED_TRANSITIONS = EnumSet.of(PREPARING, READY, PICKED_UP, CANCELLED);
    private static final Set<OrderStatus> PICKED_UP_TRANSITIONS = EnumSet.of(IN_TRANSIT, DELIVERED);
    private static final Set<OrderStatus> IN_TRANSIT_TRANSITIONS = EnumSet.of(DELIVERED);
    private static final Set<OrderStatus> DELIVERED_TRANSITIONS = EnumSet.of(COMPLETED, REFUNDED);
    private static final Set<OrderStatus> COMPLETED_TRANSITIONS = EnumSet.of(REFUNDED);
    private static final Set<OrderStatus> CANCELLED_TRANSITIONS = EnumSet.of(REFUNDED);
    private static final Set<OrderStatus> REFUNDED_TRANSITIONS = EnumSet.noneOf(OrderStatus.class);

    /**
     * Check if transition to the given status is valid.
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return getAllowedTransitions().contains(newStatus);
    }

    /**
     * Get all allowed transitions from current status.
     */
    public Set<OrderStatus> getAllowedTransitions() {
        return switch (this) {
            case CREATED -> CREATED_TRANSITIONS;
            case ACCEPTED -> ACCEPTED_TRANSITIONS;
            case PREPARING -> PREPARING_TRANSITIONS;
            case READY -> READY_TRANSITIONS;
            case COURIER_ASSIGNED -> COURIER_ASSIGNED_TRANSITIONS;
            case PICKED_UP -> PICKED_UP_TRANSITIONS;
            case IN_TRANSIT -> IN_TRANSIT_TRANSITIONS;
            case DELIVERED -> DELIVERED_TRANSITIONS;
            case COMPLETED -> COMPLETED_TRANSITIONS;
            case CANCELLED -> CANCELLED_TRANSITIONS;
            case REFUNDED -> REFUNDED_TRANSITIONS;
        };
    }

    /**
     * Check if order can be cancelled in current status.
     */
    public boolean isCancellable() {
        return this == CREATED || this == ACCEPTED || this == PREPARING || this == READY || this == COURIER_ASSIGNED;
    }

    /**
     * Check if order is in a terminal state.
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED;
    }

    /**
     * Check if order is active (not terminal).
     */
    public boolean isActive() {
        return !isTerminal();
    }

    /**
     * Check if order requires courier assignment.
     */
    public boolean requiresCourier() {
        return this == READY || this == COURIER_ASSIGNED || this == PICKED_UP || this == IN_TRANSIT;
    }
}

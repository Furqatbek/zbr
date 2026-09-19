package com.fooddelivery.integration.partner.dto;

import com.fooddelivery.order.entity.OrderStatus;

/**
 * The states a partner may report, which is a much smaller set than ours.
 *
 * <p>We asked Restos for three — accepted, declined and ready — because those
 * are the only ones that change what our customer sees or what our courier
 * does. Accepting their whole internal vocabulary would mean accepting states
 * we ignore today and might start depending on by accident tomorrow.
 *
 * <p>Kept as its own enum rather than reusing {@link OrderStatus} so their
 * vocabulary and ours can move independently — and so {@code DECLINED}, which
 * has no equivalent on our side, has somewhere to live.
 */
public enum PartnerOrderStatus {

    /** The venue has taken the order and will cook it. */
    ACCEPTED(OrderStatus.ACCEPTED),

    /** Cooking has started. Optional — not every partner reports it. */
    PREPARING(OrderStatus.PREPARING),

    /** The food is made and waiting for collection. */
    READY(OrderStatus.READY),

    /**
     * The venue will not take this order. We have no DECLINED state — an order
     * nobody will cook is cancelled, and the distinction between "cancelled"
     * and "refused" lives in the cancellation reason rather than in a status
     * the rest of the platform would have to learn.
     */
    DECLINED(OrderStatus.CANCELLED);

    private final OrderStatus mapped;

    PartnerOrderStatus(OrderStatus mapped) {
        this.mapped = mapped;
    }

    public OrderStatus toOrderStatus() {
        return mapped;
    }

    public boolean isDecline() {
        return this == DECLINED;
    }
}

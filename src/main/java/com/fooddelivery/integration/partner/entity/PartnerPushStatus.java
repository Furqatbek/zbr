package com.fooddelivery.integration.partner.entity;

/**
 * How far an order got on its way to a partner's kitchen.
 *
 * <p>The distinction that matters is {@link #FAILED} versus {@link #REJECTED}:
 * one is worth retrying and the other never will be. Retrying a rejection
 * forever hides a real problem behind a queue that looks busy.
 */
public enum PartnerPushStatus {

    /** Queued or being retried. Nothing has printed yet. */
    PENDING,

    /** The partner accepted it. The ticket is in their kitchen. */
    DELIVERED,

    /**
     * Transient failure — a timeout, a 5xx, a connection refused. Retried.
     */
    FAILED,

    /**
     * The partner refused it and will refuse it again: an unknown product, a
     * venue they do not recognise, a credential they reject. Not retried; it
     * needs a person.
     */
    REJECTED;

    public boolean isTerminal() {
        return this == DELIVERED || this == REJECTED;
    }
}

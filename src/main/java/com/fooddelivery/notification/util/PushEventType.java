package com.fooddelivery.notification.util;

import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.model.NotificationType;

/**
 * The value clients read as {@code data.type} to decide what to do.
 *
 * <p>This used to be the literal string {@code "notification"} on every push,
 * because nothing ever set it. The vendor app ignores any type outside the
 * three it knows, so in practice it discarded every push the platform sent.
 *
 * <p>The vendor app deliberately works from a THREE-value vocabulary rather
 * than the full NotificationType enum: a new order must raise a looping alarm,
 * a cancellation must interrupt cooking, and everything else is a silent
 * refresh. Collapsing here rather than in the app means adding a status to the
 * backend never silently stops reaching a vendor — an unrecognised type is
 * ignored by the client, so a default of ORDER_UPDATED is the safe one.
 *
 * <p>Other audiences get the specific NotificationType name, which is more
 * useful for deep-linking and costs them nothing.
 */
public final class PushEventType {

    /** Raise the new-order alarm. */
    public static final String NEW_ORDER_RECEIVED = "NEW_ORDER_RECEIVED";
    /** Interrupt: stop cooking, the order is gone. */
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    /** Silent refresh of the list. */
    public static final String ORDER_UPDATED = "ORDER_UPDATED";

    private PushEventType() {
    }

    public static String forRole(NotificationRole role, NotificationType type) {
        if (type == null) {
            return ORDER_UPDATED;
        }
        if (role != NotificationRole.RESTAURANT) {
            return type.name();
        }

        return switch (type) {
            case NEW_ORDER_RECEIVED, ORDER_CREATED -> NEW_ORDER_RECEIVED;
            // Refunds reach the vendor as a cancellation: the kitchen's question
            // is only ever "does this order still need cooking".
            case ORDER_CANCELLED, ORDER_REJECTED, PAYMENT_REFUNDED -> ORDER_CANCELLED;
            // Deliberately the default rather than an exhaustive list: a status
            // added later reaches the vendor as a refresh instead of being
            // dropped as an unrecognised type.
            default -> ORDER_UPDATED;
        };
    }
}

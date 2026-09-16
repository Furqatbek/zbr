package com.fooddelivery.notification.util;

import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.model.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a client reads as {@code data.type}.
 *
 * <p>This was the literal string "notification" on every push because nothing
 * ever set it. The vendor app ignores any type outside the three it knows, so
 * it discarded every push the platform sent — a shipped client receiving
 * nothing, with no error anywhere to show for it.
 */
@DisplayName("PushEventType")
class PushEventTypeTest {

    @Nested
    @DisplayName("for the vendor app")
    class ForVendor {

        private String type(NotificationType type) {
            return PushEventType.forRole(NotificationRole.RESTAURANT, type);
        }

        @Test
        @DisplayName("a new order raises the alarm")
        void newOrder() {
            assertThat(type(NotificationType.NEW_ORDER_RECEIVED)).isEqualTo("NEW_ORDER_RECEIVED");
            assertThat(type(NotificationType.ORDER_CREATED)).isEqualTo("NEW_ORDER_RECEIVED");
        }

        @Test
        @DisplayName("anything that kills the order stops the cooking")
        void cancellation() {
            // The kitchen's only question is whether this still needs cooking,
            // so a refund and a rejection read the same as a cancellation.
            assertThat(type(NotificationType.ORDER_CANCELLED)).isEqualTo("ORDER_CANCELLED");
            assertThat(type(NotificationType.ORDER_REJECTED)).isEqualTo("ORDER_CANCELLED");
            assertThat(type(NotificationType.PAYMENT_REFUNDED)).isEqualTo("ORDER_CANCELLED");
        }

        @Test
        @DisplayName("progress updates are a silent refresh")
        void progress() {
            assertThat(type(NotificationType.COURIER_ASSIGNED)).isEqualTo("ORDER_UPDATED");
            assertThat(type(NotificationType.ORDER_PICKED_UP)).isEqualTo("ORDER_UPDATED");
            assertThat(type(NotificationType.ORDER_DELIVERED)).isEqualTo("ORDER_UPDATED");
        }

        @Test
        @DisplayName("an unmapped type degrades to a refresh, never to silence")
        void unmappedDegradesSafely() {
            // The client ignores types it does not know, so a status added to
            // the backend later must not fall through as its own enum name —
            // that would silently stop reaching vendors.
            assertThat(type(NotificationType.ORDER_DELAYED)).isEqualTo("ORDER_UPDATED");
            assertThat(type(NotificationType.SYSTEM_MAINTENANCE)).isEqualTo("ORDER_UPDATED");
        }

        @Test
        @DisplayName("only ever one of the three the app understands")
        void neverAnythingElse() {
            for (NotificationType t : NotificationType.values()) {
                assertThat(type(t))
                        .as("vendor type for %s", t)
                        .isIn("NEW_ORDER_RECEIVED", "ORDER_CANCELLED", "ORDER_UPDATED");
            }
        }
    }

    @Nested
    @DisplayName("for every other audience")
    class ForOthers {

        @Test
        @DisplayName("the specific type is passed through")
        void specificType() {
            assertThat(PushEventType.forRole(NotificationRole.CONSUMER, NotificationType.ORDER_PICKED_UP))
                    .isEqualTo("ORDER_PICKED_UP");
            assertThat(PushEventType.forRole(NotificationRole.COURIER, NotificationType.NEW_DELIVERY_AVAILABLE))
                    .isEqualTo("NEW_DELIVERY_AVAILABLE");
        }

        @Test
        @DisplayName("a null role is not treated as the vendor")
        void nullRole() {
            assertThat(PushEventType.forRole(null, NotificationType.ORDER_DELIVERED))
                    .isEqualTo("ORDER_DELIVERED");
        }
    }

    @Test
    @DisplayName("a null type never produces a null data field")
    void nullType() {
        assertThat(PushEventType.forRole(NotificationRole.RESTAURANT, null)).isEqualTo("ORDER_UPDATED");
        assertThat(PushEventType.forRole(NotificationRole.CONSUMER, null)).isEqualTo("ORDER_UPDATED");
    }
}

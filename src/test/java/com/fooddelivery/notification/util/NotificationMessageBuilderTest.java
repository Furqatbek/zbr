package com.fooddelivery.notification.util;

import com.fooddelivery.notification.dto.OrderNotificationRequest;
import com.fooddelivery.notification.model.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Notification text as a customer in Tashkent actually receives it.
 *
 * <p>Two things are pinned here and both fail silently otherwise. The strings
 * are Russian, which means the sources carry Cyrillic and the build must be
 * UTF-8 — a platform-default charset mangles every literal without failing the
 * compile. And amounts are som: this formatter used to be Locale.US, so fifteen
 * thousand som was announced to customers as "$15,000.00".
 */
@DisplayName("Notification text")
class NotificationMessageBuilderTest {

    private OrderNotificationRequest request(NotificationType type) {
        OrderNotificationRequest request = new OrderNotificationRequest();
        request.setOrderId(77L);
        request.setOrderNumber("ORD-2026-000077");
        request.setEventType(type);
        request.setRestaurantName("Osh Markazi");
        request.setCourierName("Asad Karimov");
        request.setCustomerName("Dilnoza Rahimova");
        request.setOrderTotal(new BigDecimal("15000"));
        return request;
    }

    @Nested
    @DisplayName("is Russian")
    class IsRussian {

        @Test
        @DisplayName("customer messages")
        void customer() {
            Map<String, String> m = NotificationMessageBuilder.buildCustomerMessage(
                    request(NotificationType.ORDER_DELIVERED));

            assertThat(m.get("title")).isEqualTo("Заказ доставлен");
            assertThat(m.get("message")).contains("ORD-2026-000077", "Приятного аппетита");
        }

        @Test
        @DisplayName("courier messages")
        void courier() {
            Map<String, String> m = NotificationMessageBuilder.buildCourierMessage(
                    request(NotificationType.NEW_DELIVERY_AVAILABLE));

            assertThat(m.get("title")).isEqualTo("Новый заказ на доставку");
            assertThat(m.get("message")).contains("Osh Markazi");
        }

        @Test
        @DisplayName("restaurant messages")
        void restaurant() {
            Map<String, String> m = NotificationMessageBuilder.buildRestaurantMessage(
                    request(NotificationType.NEW_ORDER_RECEIVED));

            assertThat(m.get("title")).isEqualTo("Новый заказ!");
            assertThat(m.get("message")).contains("Dilnoza Rahimova");
        }

        @Test
        @DisplayName("an unhandled event type never leaks the English enum label")
        void fallbackIsRussian() {
            // The fallback used to be NotificationType.getDisplayName(), an
            // English machine label that is also exposed through the API — so a
            // new event type would quietly show a customer a raw enum name.
            Map<String, String> m = NotificationMessageBuilder.buildCustomerMessage(
                    request(NotificationType.ORDER_DELAYED));

            assertThat(m.get("title")).isEqualTo("Обновление заказа");
            assertThat(m.get("message")).doesNotContainIgnoringCase("order");
        }
    }

    @Nested
    @DisplayName("money is som")
    class Money {

        @Test
        @DisplayName("formatted as som, not dollars")
        void formatsAsSom() {
            Map<String, String> m = NotificationMessageBuilder.buildCustomerMessage(
                    request(NotificationType.PAYMENT_RECEIVED));

            assertThat(m.get("message")).contains("сум").doesNotContain("$");
            // 15000 means fifteen thousand som — grouped, and no minor unit.
            // The group separator is U+00A0, a NON-BREAKING space, which Java's
            // \\s does not match. Worth knowing beyond this test: clients that
            // split or compare these strings on a plain space will not find it.
            assertThat(m.get("message")).containsPattern("15[\\s\\u00A0]000 сум");
        }

        @Test
        @DisplayName("a null amount does not print null or a dollar sign")
        void nullAmount() {
            OrderNotificationRequest r = request(NotificationType.PAYMENT_REFUNDED);
            r.setOrderTotal(null);

            assertThat(NotificationMessageBuilder.buildCustomerMessage(r).get("message"))
                    .contains("0 сум")
                    .doesNotContain("null", "$");
        }
    }

    @Test
    @DisplayName("an order with no external number falls back to its id")
    void ordersWithoutANumber() {
        OrderNotificationRequest r = request(NotificationType.ORDER_CANCELLED);
        r.setOrderNumber(null);

        assertThat(NotificationMessageBuilder.buildCustomerMessage(r).get("message"))
                .contains("#77");
    }

    @Test
    @DisplayName("a cancellation without a reason does not trail an empty clause")
    void cancellationWithoutReason() {
        OrderNotificationRequest r = request(NotificationType.ORDER_CANCELLED);
        r.setCancellationReason(null);

        assertThat(NotificationMessageBuilder.buildCustomerMessage(r).get("message"))
                .endsWith("отменён.")
                .doesNotContain("Причина");
    }
}

package com.fooddelivery.notification.util;

import com.fooddelivery.notification.dto.OrderNotificationRequest;
import com.fooddelivery.notification.model.NotificationType;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Every notification title and body the platform sends, in Russian.
 *
 * <p>These strings are read by customers, restaurant staff and couriers in
 * Uzbekistan, so they are the product's voice and not developer-facing text.
 * {@code NotificationType.getDisplayName()} is deliberately NOT used here for
 * fallbacks: it is an English machine label exposed through the API and the
 * notification reference endpoint, and leaking it into a push would show a
 * customer a raw enum name.
 *
 * <p>Not localised per user: the platform serves one market and has no language
 * preference on the account. If that changes, these switches are the seam —
 * take a locale, and resolve through a message bundle keyed by NotificationType.
 */
@UtilityClass
public class NotificationMessageBuilder {

    /**
     * Build customer notification for order event.
     */
    public static Map<String, String> buildCustomerMessage(OrderNotificationRequest request) {
        Map<String, String> result = new HashMap<>();
        String orderRef = request.getOrderNumber() != null ? request.getOrderNumber() : "#" + request.getOrderId();

        switch (request.getEventType()) {
            case ORDER_CREATED -> {
                result.put("title", "Заказ оформлен");
                result.put("message", String.format("Ваш заказ %s оформлен. Мы сообщим, когда ресторан его подтвердит.", orderRef));
            }
            case ORDER_CONFIRMED -> {
                result.put("title", "Заказ подтверждён");
                result.put("message", String.format("Ресторан «%s» подтвердил ваш заказ %s.", request.getRestaurantName(), orderRef));
            }
            case ORDER_ACCEPTED -> {
                result.put("title", "Ресторан принял заказ");
                result.put("message", String.format("«%s» принял ваш заказ %s и скоро начнёт готовить.", request.getRestaurantName(), orderRef));
            }
            case ORDER_REJECTED -> {
                result.put("title", "Заказ не может быть выполнен");
                result.put("message", String.format("К сожалению, «%s» не может выполнить ваш заказ %s. Причина: %s",
                        request.getRestaurantName(), orderRef, request.getRejectionReason()));
            }
            case ORDER_PREPARING -> {
                result.put("title", "Заказ готовится");
                result.put("message", String.format("«%s» готовит ваш заказ %s.", request.getRestaurantName(), orderRef));
            }
            case ORDER_READY -> {
                result.put("title", "Заказ готов");
                result.put("message", String.format("Ваш заказ %s готов и ждёт курьера.", orderRef));
            }
            case COURIER_ASSIGNED -> {
                result.put("title", "Курьер назначен");
                result.put("message", String.format("%s едет за вашим заказом %s.", request.getCourierName(), orderRef));
            }
            case ORDER_PICKED_UP -> {
                result.put("title", "Курьер забрал заказ");
                result.put("message", String.format("Курьер %s забрал ваш заказ %s и уже в пути!", request.getCourierName(), orderRef));
            }
            case ORDER_IN_TRANSIT -> {
                String eta = request.getEstimatedDeliveryMinutes() != null
                        ? String.format(" Примерное время в пути: %d мин.", request.getEstimatedDeliveryMinutes())
                        : "";
                result.put("title", "Заказ в пути");
                result.put("message", String.format("Ваш заказ %s едет к вам.%s", orderRef, eta));
            }
            case ORDER_ARRIVING -> {
                result.put("title", "Курьер почти на месте");
                result.put("message", String.format("%s почти у вас с заказом %s.", request.getCourierName(), orderRef));
            }
            case ORDER_DELIVERED -> {
                result.put("title", "Заказ доставлен");
                result.put("message", String.format("Ваш заказ %s доставлен. Приятного аппетита!", orderRef));
            }
            case ORDER_CANCELLED -> {
                result.put("title", "Заказ отменён");
                result.put("message", String.format("Ваш заказ %s отменён.%s", orderRef,
                        request.getCancellationReason() != null ? " Причина: " + request.getCancellationReason() : ""));
            }
            case PAYMENT_RECEIVED -> {
                result.put("title", "Оплата прошла");
                result.put("message", String.format("Оплата %s за заказ %s получена.",
                        formatCurrency(request.getOrderTotal()), orderRef));
            }
            case PAYMENT_FAILED -> {
                result.put("title", "Оплата не прошла");
                result.put("message", String.format("Не удалось оплатить заказ %s. Попробуйте ещё раз или выберите другой способ оплаты.", orderRef));
            }
            case PAYMENT_REFUNDED -> {
                result.put("title", "Возврат оформлен");
                result.put("message", String.format("Возврат %s за заказ %s выполнен.",
                        formatCurrency(request.getOrderTotal()), orderRef));
            }
            default -> {
                result.put("title", "Обновление заказа");
                result.put("message", String.format("Есть обновление по заказу %s.", orderRef));
            }
        }

        return result;
    }

    /**
     * Build restaurant notification for order event.
     */
    public static Map<String, String> buildRestaurantMessage(OrderNotificationRequest request) {
        Map<String, String> result = new HashMap<>();
        String orderRef = request.getOrderNumber() != null ? request.getOrderNumber() : "#" + request.getOrderId();

        switch (request.getEventType()) {
            case NEW_ORDER_RECEIVED, ORDER_CREATED -> {
                result.put("title", "Новый заказ!");
                result.put("message", String.format("Новый заказ %s от %s. Сумма: %s. Подтвердите заказ.",
                        orderRef, request.getCustomerName(), formatCurrency(request.getOrderTotal())));
            }
            case ORDER_ACCEPTED -> {
                result.put("title", "Заказ принят");
                result.put("message", String.format("Вы приняли заказ %s. Начните приготовление.", orderRef));
            }
            case COURIER_ASSIGNED -> {
                result.put("title", "Курьер назначен");
                result.put("message", String.format("Курьер %s назначен на заказ %s.", request.getCourierName(), orderRef));
            }
            case COURIER_ARRIVED_RESTAURANT -> {
                result.put("title", "Курьер прибыл");
                result.put("message", String.format("Курьер %s прибыл за заказом %s.", request.getCourierName(), orderRef));
            }
            case ORDER_PICKED_UP -> {
                result.put("title", "Заказ забран");
                result.put("message", String.format("Курьер забрал заказ %s.", orderRef));
            }
            case ORDER_DELIVERED -> {
                result.put("title", "Заказ доставлен");
                result.put("message", String.format("Заказ %s доставлен клиенту.", orderRef));
            }
            case ORDER_CANCELLED -> {
                result.put("title", "Заказ отменён");
                result.put("message", String.format("Заказ %s отменён (%s).%s",
                        orderRef, request.getCancelledBy(),
                        request.getCancellationReason() != null ? " Причина: " + request.getCancellationReason() : ""));
            }
            case PAYOUT_COMPLETED -> {
                result.put("title", "Выплата получена");
                result.put("message", String.format("Выплата %s зачислена на ваш счёт.",
                        formatCurrency(request.getOrderTotal())));
            }
            default -> {
                result.put("title", "Обновление заказа");
                result.put("message", String.format("Есть обновление по заказу %s.", orderRef));
            }
        }

        return result;
    }

    /**
     * Build courier notification for delivery event.
     */
    public static Map<String, String> buildCourierMessage(OrderNotificationRequest request) {
        Map<String, String> result = new HashMap<>();
        String orderRef = request.getOrderNumber() != null ? request.getOrderNumber() : "#" + request.getOrderId();

        switch (request.getEventType()) {
            case NEW_DELIVERY_AVAILABLE, COURIER_ASSIGNED -> {
                result.put("title", "Новый заказ на доставку");
                result.put("message", String.format("Новая доставка из «%s». Заказ %s. Заберите и доставьте клиенту.",
                        request.getRestaurantName(), orderRef));
            }
            case ORDER_READY -> {
                result.put("title", "Заказ готов к выдаче");
                result.put("message", String.format("Заказ %s готов в «%s». Выезжайте!", orderRef, request.getRestaurantName()));
            }
            case ORDER_PICKED_UP -> {
                result.put("title", "Получение подтверждено");
                result.put("message", String.format("Вы забрали заказ %s. Везите его клиенту.", orderRef));
            }
            case ORDER_DELIVERED -> {
                result.put("title", "Доставка завершена");
                result.put("message", String.format("Заказ %s успешно доставлен. Отличная работа!", orderRef));
            }
            case COURIER_REASSIGNED -> {
                result.put("title", "Доставка переназначена");
                result.put("message", String.format("Заказ %s передан другому курьеру.", orderRef));
            }
            case ORDER_CANCELLED -> {
                result.put("title", "Доставка отменена");
                result.put("message", String.format("Заказ %s отменён.%s", orderRef,
                        request.getCancellationReason() != null ? " Причина: " + request.getCancellationReason() : ""));
            }
            case PAYOUT_COMPLETED -> {
                result.put("title", "Выплата зачислена");
                result.put("message", String.format("Ваш заработок %s зачислен.",
                        formatCurrency(request.getOrderTotal())));
            }
            default -> {
                result.put("title", "Обновление доставки");
                result.put("message", String.format("Есть обновление по доставке %s.", orderRef));
            }
        }

        return result;
    }

    /**
     * Build admin notification for event.
     */
    public static Map<String, String> buildAdminMessage(OrderNotificationRequest request) {
        Map<String, String> result = new HashMap<>();
        String orderRef = request.getOrderNumber() != null ? request.getOrderNumber() : "#" + request.getOrderId();

        switch (request.getEventType()) {
            case ORDER_CANCELLED -> {
                result.put("title", "Отмена заказа");
                result.put("message", String.format("Заказ %s отменён (%s). Ресторан: %s, клиент: %s. Причина: %s",
                        orderRef, request.getCancelledBy(), request.getRestaurantName(), request.getCustomerName(),
                        request.getCancellationReason() != null ? request.getCancellationReason() : "не указана"));
            }
            case ORDER_REJECTED -> {
                result.put("title", "Отклонение заказа");
                result.put("message", String.format("Заказ %s отклонён рестораном «%s». Причина: %s",
                        orderRef, request.getRestaurantName(), request.getRejectionReason()));
            }
            case PAYMENT_FAILED -> {
                result.put("title", "Сбой оплаты");
                result.put("message", String.format("Не прошла оплата заказа %s. Сумма: %s",
                        orderRef, formatCurrency(request.getOrderTotal())));
            }
            case FRAUD_ALERT -> {
                result.put("title", "Подозрение на мошенничество");
                result.put("message", String.format("По заказу %s обнаружена подозрительная активность.", orderRef));
            }
            default -> {
                result.put("title", "Системное уведомление");
                result.put("message", String.format("Событие по заказу %s.", orderRef));
            }
        }

        return result;
    }

    /**
     * Get icon for notification type.
     */
    public static String getIcon(NotificationType type) {
        return switch (type.getDefaultCategory()) {
            case ORDER -> NotificationConstants.ICON_ORDER;
            case DELIVERY -> NotificationConstants.ICON_DELIVERY;
            case FINANCE -> NotificationConstants.ICON_PAYMENT;
            case SUPPORT -> NotificationConstants.ICON_SUPPORT;
            case PROMOTION -> NotificationConstants.ICON_PROMO;
            case ACCOUNT -> NotificationConstants.ICON_ACCOUNT;
            case ALERT -> NotificationConstants.ICON_ALERT;
            case SYSTEM -> NotificationConstants.ICON_SYSTEM;
            default -> NotificationConstants.ICON_INFO;
        };
    }

    /**
     * Get action URL for order notification.
     */
    public static String getActionUrl(NotificationType type, Long orderId, Long relatedEntityId) {
        if (orderId != null) {
            return NotificationConstants.ACTION_ORDER_DETAIL.replace("{orderId}", orderId.toString());
        }
        return null;
    }

    /**
     * Money, as a customer in Tashkent reads it: "15 000 сум".
     *
     * <p>This used to be a US currency formatter, so every amount in every
     * notification was rendered as dollars — "$15,000.00" for fifteen thousand
     * som, off by a factor of twelve thousand and in the wrong currency. Amounts
     * are stored as plain decimals where 15000 means 15 000 som, and som has no
     * minor unit in practice, so fractions are dropped rather than shown.
     *
     * <p>Built per call on purpose. NumberFormat is NOT thread-safe, and the
     * push consumer now runs 3-10 concurrent threads through here; a shared
     * instance would interleave and corrupt amounts under load. Formatting a
     * number is far cheaper than the push it accompanies.
     */
    private static String formatCurrency(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru"));
        format.setMaximumFractionDigits(0);
        return format.format(amount != null ? amount : BigDecimal.ZERO) + " сум";
    }
}

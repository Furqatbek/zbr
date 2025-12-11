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
 * Builder for notification messages based on event types.
 */
@UtilityClass
public class NotificationMessageBuilder {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    /**
     * Build customer notification for order event.
     */
    public static Map<String, String> buildCustomerMessage(OrderNotificationRequest request) {
        Map<String, String> result = new HashMap<>();
        String orderRef = request.getOrderNumber() != null ? request.getOrderNumber() : "#" + request.getOrderId();

        switch (request.getEventType()) {
            case ORDER_CREATED -> {
                result.put("title", "Order Placed Successfully");
                result.put("message", String.format("Your order %s has been placed. We'll notify you when the restaurant confirms.", orderRef));
            }
            case ORDER_CONFIRMED -> {
                result.put("title", "Order Confirmed");
                result.put("message", String.format("Great news! Your order %s has been confirmed by %s.", orderRef, request.getRestaurantName()));
            }
            case ORDER_ACCEPTED -> {
                result.put("title", "Restaurant Accepted Your Order");
                result.put("message", String.format("%s has accepted your order %s and will start preparing it soon.", request.getRestaurantName(), orderRef));
            }
            case ORDER_REJECTED -> {
                result.put("title", "Order Could Not Be Fulfilled");
                result.put("message", String.format("Unfortunately, %s couldn't fulfill your order %s. Reason: %s",
                        request.getRestaurantName(), orderRef, request.getRejectionReason()));
            }
            case ORDER_PREPARING -> {
                result.put("title", "Your Order is Being Prepared");
                result.put("message", String.format("%s is now preparing your order %s.", request.getRestaurantName(), orderRef));
            }
            case ORDER_READY -> {
                result.put("title", "Order Ready for Pickup");
                result.put("message", String.format("Your order %s is ready and waiting for a courier.", orderRef));
            }
            case COURIER_ASSIGNED -> {
                result.put("title", "Courier Assigned");
                result.put("message", String.format("%s is on the way to pick up your order %s.", request.getCourierName(), orderRef));
            }
            case ORDER_PICKED_UP -> {
                result.put("title", "Order Picked Up");
                result.put("message", String.format("Your order %s has been picked up by %s and is on the way!", orderRef, request.getCourierName()));
            }
            case ORDER_IN_TRANSIT -> {
                String eta = request.getEstimatedDeliveryMinutes() != null
                        ? String.format(" ETA: %d minutes.", request.getEstimatedDeliveryMinutes())
                        : "";
                result.put("title", "Order On Its Way");
                result.put("message", String.format("Your order %s is on its way to you.%s", orderRef, eta));
            }
            case ORDER_ARRIVING -> {
                result.put("title", "Almost There!");
                result.put("message", String.format("%s is almost at your location with order %s.", request.getCourierName(), orderRef));
            }
            case ORDER_DELIVERED -> {
                result.put("title", "Order Delivered");
                result.put("message", String.format("Your order %s has been delivered. Enjoy your meal!", orderRef));
            }
            case ORDER_CANCELLED -> {
                result.put("title", "Order Cancelled");
                result.put("message", String.format("Your order %s has been cancelled. %s", orderRef,
                        request.getCancellationReason() != null ? "Reason: " + request.getCancellationReason() : ""));
            }
            case PAYMENT_RECEIVED -> {
                result.put("title", "Payment Successful");
                result.put("message", String.format("Payment of %s for order %s received successfully.",
                        formatCurrency(request.getOrderTotal()), orderRef));
            }
            case PAYMENT_FAILED -> {
                result.put("title", "Payment Failed");
                result.put("message", String.format("Payment for order %s failed. Please try again or use a different payment method.", orderRef));
            }
            case PAYMENT_REFUNDED -> {
                result.put("title", "Refund Processed");
                result.put("message", String.format("A refund of %s for order %s has been processed.",
                        formatCurrency(request.getOrderTotal()), orderRef));
            }
            default -> {
                result.put("title", request.getEventType().getDisplayName());
                result.put("message", String.format("Update for order %s.", orderRef));
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
                result.put("title", "New Order Received!");
                result.put("message", String.format("New order %s from %s. Total: %s. Please confirm.",
                        orderRef, request.getCustomerName(), formatCurrency(request.getOrderTotal())));
            }
            case ORDER_ACCEPTED -> {
                result.put("title", "Order Accepted");
                result.put("message", String.format("You've accepted order %s. Please start preparation.", orderRef));
            }
            case COURIER_ASSIGNED -> {
                result.put("title", "Courier Assigned");
                result.put("message", String.format("Courier %s has been assigned to order %s.", request.getCourierName(), orderRef));
            }
            case COURIER_ARRIVED_RESTAURANT -> {
                result.put("title", "Courier Has Arrived");
                result.put("message", String.format("Courier %s has arrived to pick up order %s.", request.getCourierName(), orderRef));
            }
            case ORDER_PICKED_UP -> {
                result.put("title", "Order Picked Up");
                result.put("message", String.format("Order %s has been picked up by courier.", orderRef));
            }
            case ORDER_DELIVERED -> {
                result.put("title", "Order Delivered");
                result.put("message", String.format("Order %s has been successfully delivered to the customer.", orderRef));
            }
            case ORDER_CANCELLED -> {
                result.put("title", "Order Cancelled");
                result.put("message", String.format("Order %s has been cancelled by %s. %s",
                        orderRef, request.getCancelledBy(),
                        request.getCancellationReason() != null ? "Reason: " + request.getCancellationReason() : ""));
            }
            case PAYOUT_COMPLETED -> {
                result.put("title", "Payout Received");
                result.put("message", String.format("Payout of %s has been deposited to your account.",
                        formatCurrency(request.getOrderTotal())));
            }
            default -> {
                result.put("title", request.getEventType().getDisplayName());
                result.put("message", String.format("Update for order %s.", orderRef));
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
                result.put("title", "New Delivery Assignment");
                result.put("message", String.format("New delivery from %s. Order %s. Pick up and deliver to customer.",
                        request.getRestaurantName(), orderRef));
            }
            case ORDER_READY -> {
                result.put("title", "Order Ready for Pickup");
                result.put("message", String.format("Order %s is ready at %s. Head there now!", orderRef, request.getRestaurantName()));
            }
            case ORDER_PICKED_UP -> {
                result.put("title", "Pickup Confirmed");
                result.put("message", String.format("Order %s pickup confirmed. Proceed to delivery.", orderRef));
            }
            case ORDER_DELIVERED -> {
                result.put("title", "Delivery Completed");
                result.put("message", String.format("Order %s delivered successfully. Great job!", orderRef));
            }
            case COURIER_REASSIGNED -> {
                result.put("title", "Delivery Reassigned");
                result.put("message", String.format("Order %s has been reassigned to another courier.", orderRef));
            }
            case ORDER_CANCELLED -> {
                result.put("title", "Delivery Cancelled");
                result.put("message", String.format("Order %s has been cancelled. %s", orderRef,
                        request.getCancellationReason() != null ? "Reason: " + request.getCancellationReason() : ""));
            }
            case PAYOUT_COMPLETED -> {
                result.put("title", "Earnings Deposited");
                result.put("message", String.format("Your earnings of %s have been deposited.",
                        formatCurrency(request.getOrderTotal())));
            }
            default -> {
                result.put("title", request.getEventType().getDisplayName());
                result.put("message", String.format("Update for delivery %s.", orderRef));
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
                result.put("title", "Order Cancellation Alert");
                result.put("message", String.format("Order %s cancelled by %s. Restaurant: %s, Customer: %s. Reason: %s",
                        orderRef, request.getCancelledBy(), request.getRestaurantName(), request.getCustomerName(),
                        request.getCancellationReason() != null ? request.getCancellationReason() : "Not specified"));
            }
            case ORDER_REJECTED -> {
                result.put("title", "Order Rejection Alert");
                result.put("message", String.format("Order %s rejected by %s. Reason: %s",
                        orderRef, request.getRestaurantName(), request.getRejectionReason()));
            }
            case PAYMENT_FAILED -> {
                result.put("title", "Payment Failure Alert");
                result.put("message", String.format("Payment failed for order %s. Amount: %s",
                        orderRef, formatCurrency(request.getOrderTotal())));
            }
            case FRAUD_ALERT -> {
                result.put("title", "Fraud Alert");
                result.put("message", String.format("Potential fraud detected for order %s.", orderRef));
            }
            default -> {
                result.put("title", "System Alert: " + request.getEventType().getDisplayName());
                result.put("message", String.format("Event occurred for order %s.", orderRef));
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

    private static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return CURRENCY_FORMAT.format(amount);
    }
}

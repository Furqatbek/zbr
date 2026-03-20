package com.fooddelivery.order.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.notification.dto.OrderNotificationRequest;
import com.fooddelivery.notification.model.NotificationType;
import com.fooddelivery.notification.service.PersistentNotificationService;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.event.CourierAssignedEvent;
import com.fooddelivery.order.event.OrderCreatedEvent;
import com.fooddelivery.order.event.OrderStatusChangedEvent;
import com.fooddelivery.order.event.PaymentConfirmedEvent;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer for order-related events from RabbitMQ.
 * Processes events and triggers appropriate notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PersistentNotificationService notificationService;
    private final RestaurantRepository restaurantRepository;
    private final CourierRepository courierRepository;

    /**
     * Process order created events from RabbitMQ.
     */
    @RabbitListener(
            queues = RabbitMQConfig.ORDER_CREATED_QUEUE,
            id = "orderCreatedListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Consuming OrderCreatedEvent from queue: orderId={}", event.getOrderId());

        try {
            // Fetch restaurant to get owner's user ID for notification targeting
            Long restaurantUserId = null;
            String restaurantName = null;
            if (event.getRestaurantId() != null) {
                Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
                if (restaurant != null) {
                    restaurantUserId = restaurant.getOwner() != null ? restaurant.getOwner().getId() : null;
                    restaurantName = restaurant.getName();
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderNumber", event.getExternalOrderNo());
            metadata.put("consumerId", event.getConsumerId());
            metadata.put("restaurantId", event.getRestaurantId());
            metadata.put("orderType", event.getOrderType() != null ? event.getOrderType().name() : null);
            metadata.put("total", event.getTotal());
            metadata.put("deliveryAddress", event.getDeliveryAddress());

            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getExternalOrderNo())
                    .customerId(event.getConsumerId())
                    .restaurantId(event.getRestaurantId())
                    .restaurantUserId(restaurantUserId)
                    .restaurantName(restaurantName)
                    .eventType(NotificationType.ORDER_CREATED)
                    .totalAmount(event.getTotal())
                    .metadata(metadata)
                    .build();

            notificationService.notifyOrderCreated(request);
            log.debug("Order created notification processed for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process OrderCreatedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw e; // Re-throw to trigger DLQ handling
        }
    }

    /**
     * Process order status changed events from RabbitMQ.
     */
    @RabbitListener(
            queues = RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE,
            id = "orderStatusChangedListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Consuming OrderStatusChangedEvent from queue: orderId={}, {} -> {}",
                event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());

        try {
            OrderNotificationRequest request = buildStatusChangeRequest(event);

            switch (event.getNewStatus()) {
                case ACCEPTED -> notificationService.notifyOrderAccepted(request);
                case PREPARING -> notificationService.notifyOrderPreparing(request);
                case READY -> notificationService.notifyOrderReady(request);
                case COURIER_ASSIGNED -> notificationService.notifyCourierAssigned(request);
                case PICKED_UP -> notificationService.notifyOrderPickedUp(request);
                case IN_TRANSIT -> notificationService.notifyOrderInTransit(request);
                case DELIVERED -> notificationService.notifyOrderDelivered(request);
                case CANCELLED -> notificationService.notifyOrderCancelled(request);
                case REFUNDED -> notificationService.notifyRefundProcessed(request);
                default -> log.debug("No notification for status: {}", event.getNewStatus());
            }

            log.debug("Order status notification processed for order: {} ({})",
                    event.getOrderId(), event.getNewStatus());

        } catch (Exception e) {
            log.error("Failed to process OrderStatusChangedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Process courier assigned events from RabbitMQ.
     */
    @RabbitListener(
            queues = RabbitMQConfig.COURIER_ASSIGNED_QUEUE,
            id = "courierAssignedListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleCourierAssigned(CourierAssignedEvent event) {
        log.info("Consuming CourierAssignedEvent from queue: orderId={}, courierId={}",
                event.getOrderId(), event.getCourierId());

        try {
            // Fetch courier to get user ID for notification targeting
            Long courierUserId = null;
            String courierName = null;
            if (event.getCourierId() != null) {
                Courier courier = courierRepository.findById(event.getCourierId()).orElse(null);
                if (courier != null && courier.getUser() != null) {
                    courierUserId = courier.getUser().getId();
                    courierName = courier.getUser().getFullName();
                }
            }

            // Fetch restaurant to get owner's user ID
            Long restaurantUserId = null;
            if (event.getRestaurantId() != null) {
                Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
                if (restaurant != null && restaurant.getOwner() != null) {
                    restaurantUserId = restaurant.getOwner().getId();
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("courierId", event.getCourierId());
            metadata.put("pickupAddress", event.getPickupAddress());
            metadata.put("deliveryAddress", event.getDeliveryAddress());
            metadata.put("pickupLat", event.getPickupLat());
            metadata.put("pickupLng", event.getPickupLng());
            metadata.put("deliveryLat", event.getDeliveryLat());
            metadata.put("deliveryLng", event.getDeliveryLng());

            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getExternalOrderNo())
                    .restaurantId(event.getRestaurantId())
                    .restaurantUserId(restaurantUserId)
                    .courierId(event.getCourierId())
                    .courierUserId(courierUserId)
                    .courierName(courierName)
                    .eventType(NotificationType.COURIER_ASSIGNED)
                    .metadata(metadata)
                    .build();

            notificationService.notifyCourierAssigned(request);
            log.debug("Courier assigned notification processed for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process CourierAssignedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Process payment confirmed events from RabbitMQ.
     */
    @RabbitListener(
            queues = RabbitMQConfig.PAYMENT_CONFIRMED_QUEUE,
            id = "paymentConfirmedListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Consuming PaymentConfirmedEvent from queue: orderId={}, amount={}",
                event.getOrderId(), event.getAmount());

        try {
            // Fetch restaurant to get owner's user ID for notification targeting
            Long restaurantUserId = null;
            String restaurantName = null;
            if (event.getRestaurantId() != null) {
                Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
                if (restaurant != null) {
                    restaurantUserId = restaurant.getOwner() != null ? restaurant.getOwner().getId() : null;
                    restaurantName = restaurant.getName();
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paymentId", event.getPaymentId());
            metadata.put("amount", event.getAmount());
            metadata.put("provider", event.getProvider());
            metadata.put("providerPaymentId", event.getProviderPaymentId());

            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getExternalOrderNo())
                    .customerId(event.getConsumerId())
                    .restaurantId(event.getRestaurantId())
                    .restaurantUserId(restaurantUserId)
                    .restaurantName(restaurantName)
                    .eventType(NotificationType.PAYMENT_RECEIVED)
                    .totalAmount(event.getAmount())
                    .metadata(metadata)
                    .build();

            notificationService.notifyPaymentReceived(request);
            log.debug("Payment confirmed notification processed for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process PaymentConfirmedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Build order notification request from status change event.
     */
    private OrderNotificationRequest buildStatusChangeRequest(OrderStatusChangedEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previousStatus", event.getPreviousStatus().name());
        metadata.put("newStatus", event.getNewStatus().name());

        if (event.getReason() != null) {
            metadata.put("reason", event.getReason());
        }

        // Fetch restaurant to get owner's user ID for notification targeting
        Long restaurantUserId = null;
        String restaurantName = null;
        if (event.getRestaurantId() != null) {
            Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
            if (restaurant != null) {
                restaurantUserId = restaurant.getOwner() != null ? restaurant.getOwner().getId() : null;
                restaurantName = restaurant.getName();
            }
        }

        // Fetch courier to get user ID for notification targeting
        Long courierUserId = null;
        String courierName = null;
        if (event.getCourierId() != null) {
            Courier courier = courierRepository.findById(event.getCourierId()).orElse(null);
            if (courier != null && courier.getUser() != null) {
                courierUserId = courier.getUser().getId();
                courierName = courier.getUser().getFullName();
            }
        }

        return OrderNotificationRequest.builder()
                .orderId(event.getOrderId())
                .orderNumber(event.getExternalOrderNo())
                .customerId(event.getConsumerId())
                .restaurantId(event.getRestaurantId())
                .restaurantUserId(restaurantUserId)
                .restaurantName(restaurantName)
                .courierId(event.getCourierId())
                .courierUserId(courierUserId)
                .courierName(courierName)
                .eventType(mapStatusToNotificationType(event.getNewStatus()))
                .cancellationReason(event.getReason())
                .metadata(metadata)
                .build();
    }

    /**
     * Map order status to notification type.
     */
    private NotificationType mapStatusToNotificationType(OrderStatus status) {
        return switch (status) {
            case CREATED -> NotificationType.ORDER_CREATED;
            case ACCEPTED -> NotificationType.ORDER_ACCEPTED;
            case PREPARING -> NotificationType.ORDER_PREPARING;
            case READY -> NotificationType.ORDER_READY;
            case COURIER_ASSIGNED -> NotificationType.COURIER_ASSIGNED;
            case PICKED_UP -> NotificationType.ORDER_PICKED_UP;
            case IN_TRANSIT -> NotificationType.ORDER_IN_TRANSIT;
            case DELIVERED -> NotificationType.ORDER_DELIVERED;
            case COMPLETED -> NotificationType.ORDER_COMPLETED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
            case REFUNDED -> NotificationType.REFUND_PROCESSED;
        };
    }
}

package com.fooddelivery.courier.reassignment.service;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.common.event.EventPublisher;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.courier.reassignment.entity.ReassignmentInitiator;
import com.fooddelivery.courier.reassignment.entity.ReassignmentLog;
import com.fooddelivery.courier.reassignment.entity.ReassignmentReason;
import com.fooddelivery.courier.reassignment.repository.ReassignmentLogRepository;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.notification.service.PersistentNotificationService;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.event.OrderStatusChangedEvent;
import com.fooddelivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for reassigning orders to different couriers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourierReassignmentService {

    private static final int MAX_REASSIGNMENTS = 3;
    private static final double SEARCH_RADIUS_KM = 10.0;

    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;
    private final ReassignmentLogRepository reassignmentLogRepository;
    private final PersistentNotificationService notificationService;
    private final EventPublisher eventPublisher;

    /**
     * Reassign an order to a different courier.
     */
    @Transactional
    public ReassignmentLog reassignOrder(Long orderId, ReassignmentReason reason,
                                          ReassignmentInitiator initiator, Long initiatorUserId,
                                          String description) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Validate order state
        validateOrderForReassignment(order);

        Courier previousCourier = order.getCourier();
        if (previousCourier == null) {
            throw new BusinessException("Order has no assigned courier to reassign from");
        }

        // Check reassignment limit
        if (order.getReassignmentCount() >= MAX_REASSIGNMENTS) {
            throw new BusinessException("Order has reached maximum reassignment limit of " + MAX_REASSIGNMENTS);
        }

        log.info("Reassigning order {} from courier {} - reason: {}",
                orderId, previousCourier.getId(), reason);

        // Store previous courier info
        order.setPreviousCourierId(previousCourier.getId());
        order.setReassignmentCount(order.getReassignmentCount() + 1);

        // Unassign current courier
        order.setCourier(null);

        // Move order back to READY status if it was picked up or in transit
        OrderStatus previousStatus = order.getStatus();
        if (previousStatus == OrderStatus.PICKED_UP || previousStatus == OrderStatus.IN_TRANSIT) {
            order.setStatus(OrderStatus.READY);
            order.setPickedUpAt(null);
            order.setInTransitAt(null);
        }

        orderRepository.save(order);

        // Update courier status if needed
        if (reason.requiresCourierOffline()) {
            setCourierOffline(previousCourier, reason);
        } else {
            // Make courier available again
            if (previousCourier.getStatus() == CourierStatus.BUSY) {
                previousCourier.setStatus(CourierStatus.AVAILABLE);
                courierRepository.save(previousCourier);
            }
        }

        // Create reassignment log
        ReassignmentLog reassignmentLog = ReassignmentLog.builder()
                .order(order)
                .previousCourier(previousCourier)
                .reason(reason)
                .description(description)
                .initiatedBy(initiator)
                .initiatedByUserId(initiatorUserId)
                .build();
        reassignmentLog = reassignmentLogRepository.save(reassignmentLog);

        // Broadcast order to available couriers
        broadcastOrderAvailable(order);

        // Notify customer
        notifyCustomerOfReassignment(order, reason);

        // Publish event
        publishOrderStatusChanged(order, previousStatus);

        log.info("Order {} reassigned successfully. Previous courier: {}, Reason: {}",
                orderId, previousCourier.getId(), reason);

        return reassignmentLog;
    }

    /**
     * Request reassignment by courier.
     */
    @Transactional
    public ReassignmentLog requestReassignmentByCourier(Long orderId, Long courierId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new BusinessException("Courier is not assigned to this order");
        }

        return reassignOrder(orderId, ReassignmentReason.COURIER_REQUEST,
                ReassignmentInitiator.COURIER, courierId, reason);
    }

    /**
     * Find nearest available couriers for reassignment.
     */
    @Transactional(readOnly = true)
    public List<Courier> findAvailableCouriersNear(Order order, double radiusKm) {
        BigDecimal lat = order.getRestaurant().getLatitude();
        BigDecimal lon = order.getRestaurant().getLongitude();

        if (lat == null || lon == null) {
            // Fallback to all available couriers
            return courierRepository.findByStatus(CourierStatus.AVAILABLE);
        }

        return courierRepository.findAvailableCouriersNearLocation(
                lat.doubleValue(), lon.doubleValue(), radiusKm);
    }

    /**
     * Auto-assign order to first accepting courier.
     */
    @Transactional
    public Optional<Courier> autoAssignToBestCourier(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        List<Courier> availableCouriers = findAvailableCouriersNear(order, SEARCH_RADIUS_KM);

        if (availableCouriers.isEmpty()) {
            log.warn("No available couriers found for order {} within {} km", orderId, SEARCH_RADIUS_KM);
            return Optional.empty();
        }

        // Pick the first available courier (could be enhanced with scoring)
        Courier bestCourier = availableCouriers.get(0);

        // Assign courier to order
        order.setCourier(bestCourier);
        order.setStatus(OrderStatus.COURIER_ASSIGNED);
        bestCourier.setStatus(CourierStatus.BUSY);

        orderRepository.save(order);
        courierRepository.save(bestCourier);

        // Update reassignment log if exists
        reassignmentLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .findFirst()
                .filter(log -> log.getNewCourier() == null)
                .ifPresent(log -> {
                    log.setNewCourier(bestCourier);
                    reassignmentLogRepository.save(log);
                });

        log.info("Order {} auto-assigned to courier {}", orderId, bestCourier.getId());
        return Optional.of(bestCourier);
    }

    /**
     * Validate order can be reassigned.
     */
    private void validateOrderForReassignment(Order order) {
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.DELIVERED ||
            status == OrderStatus.COMPLETED ||
            status == OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot reassign order in status: " + status);
        }

        if (status == OrderStatus.CREATED ||
            status == OrderStatus.ACCEPTED ||
            status == OrderStatus.PREPARING) {
            throw new BusinessException("Order has not been assigned a courier yet");
        }
    }

    /**
     * Set courier to offline status due to issue.
     */
    private void setCourierOffline(Courier courier, ReassignmentReason reason) {
        courier.setStatus(CourierStatus.OFFLINE);
        courierRepository.save(courier);
        log.info("Courier {} set to OFFLINE due to: {}", courier.getId(), reason);
    }

    /**
     * Broadcast order availability to couriers.
     */
    private void broadcastOrderAvailable(Order order) {
        // This would typically send push notifications or WebSocket messages
        // to available couriers in the area
        log.info("Broadcasting order {} availability to couriers", order.getId());

        notificationService.notifyAvailableCouriers(order);
    }

    /**
     * Notify customer about reassignment.
     */
    private void notifyCustomerOfReassignment(Order order, ReassignmentReason reason) {
        String message = buildReassignmentMessage(reason);
        notificationService.notifyOrderReassigned(order, message);
    }

    /**
     * Build customer-friendly reassignment message.
     */
    private String buildReassignmentMessage(ReassignmentReason reason) {
        return switch (reason) {
            case COURIER_UNRESPONSIVE -> "We're assigning a new delivery partner to your order";
            case COURIER_ACCIDENT -> "For safety reasons, we're reassigning your delivery";
            case VEHICLE_ISSUE -> "Due to unforeseen circumstances, we're assigning a new courier";
            case ETA_EXCEEDED -> "To ensure faster delivery, we're assigning a new courier";
            case COURIER_REQUEST -> "Your order is being reassigned to another courier";
            default -> "Your order is being reassigned to ensure timely delivery";
        };
    }

    /**
     * Publish order status changed event.
     */
    private void publishOrderStatusChanged(Order order, OrderStatus previousStatus) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(),
                order.getExternalOrderNo(),
                order.getRestaurant().getId(),
                order.getConsumer().getId(),
                null, // No courier assigned yet
                previousStatus,
                order.getStatus(),
                "Order reassigned"
        );

        eventPublisher.publishAsync(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_CHANGED_KEY,
                event
        );
    }
}

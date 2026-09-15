package com.fooddelivery.order.service;

import com.fooddelivery.order.dto.OrderDto;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes an order's current state to everyone watching it live.
 *
 * <p>This exists because the two halves of the order lifecycle disagreed.
 * Restaurant-driven transitions (accept, prepare, ready, cancel) went through
 * OrderService, which broadcast to the order topic, the consumer's queue and the
 * restaurant topic. Courier-driven transitions (assigned, picked up, in transit,
 * delivered) went through CourierService, which published the RabbitMQ event and
 * broadcast nothing — so a customer watching {@code /topic/orders/{id}} got live
 * updates while the food was cooking and then silence for the entire delivery,
 * which is the part they actually wait for.
 *
 * <p>One component so a new status handler cannot quietly reintroduce the gap by
 * forgetting half the destinations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRealtimeBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderMapper orderMapper;

    /**
     * Fan the order out to every live subscriber: the order topic, the customer
     * personally, and the restaurant.
     *
     * <p>Never throws — a websocket failure must not roll back a status change
     * that has already happened. Push notifications cover the same event on a
     * separate path.
     */
    public void broadcastStatusChange(Order order) {
        OrderDto dto;
        try {
            dto = orderMapper.toDto(order);
        } catch (Exception e) {
            log.warn("Could not map order {} for broadcast: {}", order.getId(), e.getMessage());
            return;
        }

        // Anyone with this order open — customer, courier, restaurant staff.
        send("/topic/orders/" + order.getId(), dto);

        // The customer's own queue, so the app receives it without subscribing
        // per order.
        if (order.getConsumer() != null && order.getConsumer().getEmail() != null) {
            try {
                messagingTemplate.convertAndSendToUser(
                        order.getConsumer().getEmail(), "/queue/orders", dto);
            } catch (Exception e) {
                log.warn("Could not notify consumer of order {}: {}", order.getId(), e.getMessage());
            }
        }

        if (order.getRestaurant() != null) {
            send("/topic/restaurants/" + order.getRestaurant().getId() + "/orders", dto);
        }
    }

    private void send(String destination, OrderDto dto) {
        try {
            messagingTemplate.convertAndSend(destination, dto);
        } catch (Exception e) {
            log.warn("Could not broadcast to {}: {}", destination, e.getMessage());
        }
    }
}

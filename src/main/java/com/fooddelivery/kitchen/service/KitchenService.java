package com.fooddelivery.kitchen.service;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.common.util.SlugUtils;
import com.fooddelivery.kitchen.dto.KitchenTicketDto;
import com.fooddelivery.kitchen.event.KitchenTicketEvent;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for Kitchen Display System (KDS) operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KitchenService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Generate and send kitchen ticket when order is accepted.
     */
    @Transactional
    public void generateKitchenTicket(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order not found for kitchen ticket: {}", orderId);
            return;
        }

        String ticketNumber = SlugUtils.generateTicketNumber(order.getRestaurant().getId());

        KitchenTicketDto ticket = KitchenTicketDto.builder()
                .ticketNumber(ticketNumber)
                .orderId(order.getId())
                .externalOrderNo(order.getExternalOrderNo())
                .restaurantId(order.getRestaurant().getId())
                .orderType(order.getOrderType().name())
                .tableId(order.getTableId())
                .items(order.getItems().stream()
                        .map(this::toTicketItem)
                        .toList())
                .notes(order.getNotes())
                .estimatedPrepTime(order.getEstimatedPrepTimeMinutes())
                .createdAt(LocalDateTime.now())
                .build();

        // Send to RabbitMQ for print queue
        KitchenTicketEvent event = new KitchenTicketEvent(ticket);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.KITCHEN_EXCHANGE,
                RabbitMQConfig.KITCHEN_TICKET_KEY,
                event
        );

        // Send to WebSocket for KDS display
        messagingTemplate.convertAndSend(
                "/topic/restaurants/" + order.getRestaurant().getId() + "/kitchen",
                ticket
        );

        log.info("Kitchen ticket generated: {} for order {}", ticketNumber, order.getExternalOrderNo());
    }

    /**
     * Update ticket status (started, completed, etc.)
     */
    public void updateTicketStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;

        messagingTemplate.convertAndSend(
                "/topic/restaurants/" + order.getRestaurant().getId() + "/kitchen/status",
                java.util.Map.of(
                        "orderId", orderId,
                        "status", status,
                        "timestamp", LocalDateTime.now()
                )
        );
    }

    private KitchenTicketDto.TicketItem toTicketItem(OrderItem item) {
        return KitchenTicketDto.TicketItem.builder()
                .name(item.getItemName())
                .quantity(item.getQuantity())
                .variant(item.getVariantName())
                .modifiers(item.getModifiersJson())
                .specialInstructions(item.getSpecialInstructions())
                .build();
    }
}

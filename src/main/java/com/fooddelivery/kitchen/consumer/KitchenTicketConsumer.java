package com.fooddelivery.kitchen.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.kitchen.dto.KitchenTicketDto;
import com.fooddelivery.kitchen.event.KitchenTicketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumer for kitchen ticket events from RabbitMQ.
 * Handles printing and display of kitchen tickets.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KitchenTicketConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Process kitchen ticket events from RabbitMQ.
     * This handles ticket printing and forwarding to KDS displays.
     */
    @RabbitListener(
            queues = RabbitMQConfig.KITCHEN_TICKET_QUEUE,
            id = "kitchenTicketListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleKitchenTicket(KitchenTicketEvent event) {
        KitchenTicketDto ticket = event.getTicket();
        log.info("Consuming KitchenTicketEvent from queue: ticketNumber={}, orderId={}",
                ticket.getTicketNumber(), ticket.getOrderId());

        try {
            // Forward to WebSocket for KDS displays (redundant with direct send, but ensures reliability)
            messagingTemplate.convertAndSend(
                    "/topic/restaurants/" + ticket.getRestaurantId() + "/kitchen/tickets",
                    ticket
            );

            // Log for potential printer integration
            logTicketForPrinting(ticket);

            log.debug("Kitchen ticket processed: {} for order {}",
                    ticket.getTicketNumber(), ticket.getExternalOrderNo());

        } catch (Exception e) {
            log.error("Failed to process KitchenTicketEvent for order {}: {}",
                    ticket.getOrderId(), e.getMessage(), e);
            throw e; // Re-throw to trigger DLQ handling
        }
    }

    /**
     * Log ticket details for printing (placeholder for actual printer integration).
     */
    private void logTicketForPrinting(KitchenTicketDto ticket) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== KITCHEN TICKET ==========\n");
        sb.append("Ticket #: ").append(ticket.getTicketNumber()).append("\n");
        sb.append("Order: ").append(ticket.getExternalOrderNo()).append("\n");
        sb.append("Type: ").append(ticket.getOrderType()).append("\n");

        if (ticket.getTableId() != null) {
            sb.append("Table: ").append(ticket.getTableId()).append("\n");
        }

        sb.append("-----------------------------------\n");

        if (ticket.getItems() != null) {
            for (KitchenTicketDto.TicketItem item : ticket.getItems()) {
                sb.append(String.format("%dx %s", item.getQuantity(), item.getName()));

                if (item.getVariant() != null && !item.getVariant().isEmpty()) {
                    sb.append(" (").append(item.getVariant()).append(")");
                }

                sb.append("\n");

                if (item.getModifiers() != null && !item.getModifiers().isEmpty()) {
                    sb.append("   + ").append(item.getModifiers()).append("\n");
                }

                if (item.getSpecialInstructions() != null && !item.getSpecialInstructions().isEmpty()) {
                    sb.append("   * ").append(item.getSpecialInstructions()).append("\n");
                }
            }
        }

        sb.append("-----------------------------------\n");

        if (ticket.getNotes() != null && !ticket.getNotes().isEmpty()) {
            sb.append("Notes: ").append(ticket.getNotes()).append("\n");
        }

        if (ticket.getEstimatedPrepTime() != null) {
            sb.append("Est. Prep Time: ").append(ticket.getEstimatedPrepTime()).append(" min\n");
        }

        sb.append("=====================================\n");

        log.info("Kitchen ticket ready for printing:{}", sb);
    }
}

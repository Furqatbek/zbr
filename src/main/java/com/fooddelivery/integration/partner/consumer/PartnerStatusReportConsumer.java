package com.fooddelivery.integration.partner.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.integration.partner.service.PartnerStatusReportService;
import com.fooddelivery.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Forwards an order's progress to the partner whose till it came from.
 *
 * <p>Its own queue on the status-changed routing key, for the same reason the
 * push has one: two consumers sharing a queue means whichever wins a message
 * decides whether the other's work happens at all.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerStatusReportConsumer {

    private final PartnerStatusReportService reportService;

    @RabbitListener(
            queues = RabbitMQConfig.PARTNER_STATUS_REPORT_QUEUE,
            id = "partnerStatusReportListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleStatusChanged(OrderStatusChangedEvent event) {
        boolean settled = reportService.report(
                event.getOrderId(), event.getRestaurantId(), event.getExternalOrderNo(),
                event.getNewStatus(), event.getReason());

        if (!settled) {
            throw new PartnerOrderPushConsumer.PartnerPushRetryException(
                    "Status " + event.getNewStatus() + " on order " + event.getExternalOrderNo()
                            + " has not reached the partner yet");
        }
    }
}

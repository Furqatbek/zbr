package com.fooddelivery.courier.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.courier.dto.CourierLocationDto;
import com.fooddelivery.courier.repository.CourierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Consumer for courier location updates from RabbitMQ.
 * Processes location updates and broadcasts them to interested parties.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourierLocationConsumer {

    private final CourierRepository courierRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Process courier location updates from the queue.
     */
    @RabbitListener(
            queues = RabbitMQConfig.COURIER_LOCATION_QUEUE,
            id = "courierLocationListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleCourierLocation(CourierLocationDto location) {
        log.debug("Consuming courier location from queue: courierId={}, lat={}, lng={}",
                location.getCourierId(), location.getLat(), location.getLng());

        if (location.getCourierId() == null) {
            log.warn("Courier location update skipped: no courier ID provided");
            return;
        }

        if (location.getLat() == null || location.getLng() == null) {
            log.warn("Courier location update skipped: invalid coordinates for courier {}",
                    location.getCourierId());
            return;
        }

        try {
            // Update location in database
            int updated = courierRepository.updateLocation(
                    location.getCourierId(),
                    location.getLat(),
                    location.getLng()
            );

            if (updated == 0) {
                log.warn("Courier location update failed: courier {} not found",
                        location.getCourierId());
                return;
            }

            // Broadcast to WebSocket for real-time tracking
            broadcastLocationUpdate(location);

            log.debug("Courier {} location updated: ({}, {})",
                    location.getCourierId(), location.getLat(), location.getLng());

        } catch (Exception e) {
            log.error("Failed to process courier location for courier {}: {}",
                    location.getCourierId(), e.getMessage(), e);
            throw e; // Re-throw to trigger DLQ handling
        }
    }

    /**
     * Broadcast location update to WebSocket subscribers.
     */
    private void broadcastLocationUpdate(CourierLocationDto location) {
        try {
            CourierLocationDto broadcast = CourierLocationDto.builder()
                    .courierId(location.getCourierId())
                    .lat(location.getLat())
                    .lng(location.getLng())
                    .timestamp(location.getTimestamp() != null ?
                            location.getTimestamp() : LocalDateTime.now())
                    .build();

            // Broadcast to courier-specific channel
            messagingTemplate.convertAndSend(
                    "/topic/couriers/" + location.getCourierId() + "/location",
                    broadcast
            );

            // Also broadcast to tracking channel for order tracking
            messagingTemplate.convertAndSend(
                    "/topic/tracking/couriers",
                    broadcast
            );

        } catch (Exception e) {
            log.warn("Failed to broadcast courier location: {}", e.getMessage());
        }
    }
}

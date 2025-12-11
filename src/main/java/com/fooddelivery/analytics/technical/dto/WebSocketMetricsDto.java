package com.fooddelivery.analytics.technical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for WebSocket metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMetricsDto {

    /**
     * Total active connections.
     */
    private Long totalActiveConnections;

    /**
     * Connected couriers count.
     */
    private Long connectedCouriers;

    /**
     * Connected consumers count.
     */
    private Long connectedConsumers;

    /**
     * Connected restaurants count.
     */
    private Long connectedRestaurants;

    /**
     * Connected admins count.
     */
    private Long connectedAdmins;

    /**
     * Message delivery metrics.
     */
    private MessageDeliveryMetricsDto messageDelivery;

    /**
     * Connection metrics.
     */
    private ConnectionMetricsDto connectionMetrics;

    /**
     * Dropped connections in the period.
     */
    private DroppedConnectionsDto droppedConnections;

    /**
     * Connections by user type.
     */
    private Map<String, Long> connectionsByUserType;

    /**
     * Hourly connection distribution.
     */
    private List<HourlyConnectionsDto> hourlyDistribution;

    /**
     * Start of the reporting period.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime periodStart;

    /**
     * End of the reporting period.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime periodEnd;

    /**
     * Timestamp when metrics were collected.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime collectedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageDeliveryMetricsDto {
        /**
         * Total messages sent in period.
         */
        private Long totalMessagesSent;

        /**
         * Total messages delivered.
         */
        private Long totalMessagesDelivered;

        /**
         * Failed message deliveries.
         */
        private Long failedDeliveries;

        /**
         * Delivery success rate (0.0 - 1.0).
         */
        private Double deliverySuccessRate;

        /**
         * Average delivery delay in ms.
         */
        private Double avgDeliveryDelayMs;

        /**
         * P50 delivery delay in ms.
         */
        private Long p50DeliveryDelayMs;

        /**
         * P90 delivery delay in ms.
         */
        private Long p90DeliveryDelayMs;

        /**
         * P99 delivery delay in ms.
         */
        private Long p99DeliveryDelayMs;

        /**
         * Max delivery delay in ms.
         */
        private Long maxDeliveryDelayMs;

        /**
         * Messages per second.
         */
        private Double messagesPerSecond;

        /**
         * Messages by type.
         */
        private Map<String, Long> messagesByType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConnectionMetricsDto {
        /**
         * Total connections established in period.
         */
        private Long totalConnectionsEstablished;

        /**
         * Total disconnections in period.
         */
        private Long totalDisconnections;

        /**
         * Average connection duration in seconds.
         */
        private Double avgConnectionDurationSeconds;

        /**
         * Max concurrent connections.
         */
        private Long maxConcurrentConnections;

        /**
         * Average messages per connection.
         */
        private Double avgMessagesPerConnection;

        /**
         * Peak connection hour.
         */
        private Integer peakHour;

        /**
         * Connection rate (connections per minute).
         */
        private Double connectionRatePerMinute;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DroppedConnectionsDto {
        /**
         * Total dropped connections.
         */
        private Long totalDropped;

        /**
         * Dropped rate (0.0 - 1.0).
         */
        private Double droppedRate;

        /**
         * Graceful disconnects.
         */
        private Long gracefulDisconnects;

        /**
         * Forced disconnects (errors).
         */
        private Long forcedDisconnects;

        /**
         * Dropped by reason.
         */
        private Map<String, Long> droppedByReason;

        /**
         * Dropped by user type.
         */
        private Map<String, Long> droppedByUserType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyConnectionsDto {
        private Integer hour;
        private Long activeConnections;
        private Long newConnections;
        private Long disconnections;
        private Long messagesSent;
    }
}

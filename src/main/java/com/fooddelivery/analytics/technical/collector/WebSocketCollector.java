package com.fooddelivery.analytics.technical.collector;

import com.fooddelivery.analytics.technical.dto.BackendResourceMetricsDto;
import com.fooddelivery.analytics.technical.dto.WebSocketMetricsDto;
import com.fooddelivery.analytics.technical.model.WebSocketConnectionLog;
import com.fooddelivery.analytics.technical.repository.WebSocketConnectionLogRepository;
import com.fooddelivery.analytics.technical.repository.WebSocketMessageLogRepository;
import com.fooddelivery.analytics.technical.util.PercentileCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collector for WebSocket metrics.
 * Sources: WebSocket connection logs, message delivery logs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketCollector {

    private final WebSocketConnectionLogRepository connectionLogRepository;
    private final WebSocketMessageLogRepository messageLogRepository;
    private final PercentileCalculator percentileCalculator;

    // Thresholds
    private static final double DELIVERY_SUCCESS_WARNING = 0.99;
    private static final double DELIVERY_SUCCESS_CRITICAL = 0.95;
    private static final long DELIVERY_DELAY_WARNING_MS = 100;
    private static final long DELIVERY_DELAY_CRITICAL_MS = 500;
    private static final double DROPPED_RATE_WARNING = 0.01;
    private static final double DROPPED_RATE_CRITICAL = 0.05;

    /**
     * Collect WebSocket metrics for a period.
     */
    public WebSocketMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate, boolean includeHourly) {
        log.debug("Collecting WebSocket metrics from {} to {}", startDate, endDate);

        // Current connection counts
        Long totalActive = connectionLogRepository.countActiveConnections();
        Map<String, Long> connectionsByType = new HashMap<>();
        connectionLogRepository.countActiveConnectionsByUserType()
                .forEach(row -> connectionsByType.put(
                        ((WebSocketConnectionLog.UserType) row[0]).name(),
                        ((Number) row[1]).longValue()));

        Long connectedCouriers = connectionsByType.getOrDefault("COURIER", 0L);
        Long connectedConsumers = connectionsByType.getOrDefault("CONSUMER", 0L);
        Long connectedRestaurants = connectionsByType.getOrDefault("RESTAURANT", 0L);
        Long connectedAdmins = connectionsByType.getOrDefault("ADMIN", 0L);

        // Message delivery metrics
        WebSocketMetricsDto.MessageDeliveryMetricsDto messageDelivery =
                collectMessageDeliveryMetrics(startDate, endDate);

        // Connection metrics
        WebSocketMetricsDto.ConnectionMetricsDto connectionMetrics =
                collectConnectionMetrics(startDate, endDate);

        // Dropped connections
        WebSocketMetricsDto.DroppedConnectionsDto droppedConnections =
                collectDroppedConnections(startDate, endDate);

        // Hourly distribution
        List<WebSocketMetricsDto.HourlyConnectionsDto> hourlyDistribution = null;
        if (includeHourly) {
            hourlyDistribution = getHourlyDistribution(startDate, endDate);
        }

        return WebSocketMetricsDto.builder()
                .totalActiveConnections(totalActive)
                .connectedCouriers(connectedCouriers)
                .connectedConsumers(connectedConsumers)
                .connectedRestaurants(connectedRestaurants)
                .connectedAdmins(connectedAdmins)
                .messageDelivery(messageDelivery)
                .connectionMetrics(connectionMetrics)
                .droppedConnections(droppedConnections)
                .connectionsByUserType(connectionsByType)
                .hourlyDistribution(hourlyDistribution)
                .periodStart(startDate)
                .periodEnd(endDate)
                .collectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Collect current real-time WebSocket metrics.
     */
    public WebSocketMetricsDto collectRealTime() {
        log.debug("Collecting real-time WebSocket metrics");

        Long totalActive = connectionLogRepository.countActiveConnections();
        Map<String, Long> connectionsByType = new HashMap<>();
        connectionLogRepository.countActiveConnectionsByUserType()
                .forEach(row -> connectionsByType.put(
                        ((WebSocketConnectionLog.UserType) row[0]).name(),
                        ((Number) row[1]).longValue()));

        return WebSocketMetricsDto.builder()
                .totalActiveConnections(totalActive != null ? totalActive : 0L)
                .connectedCouriers(connectionsByType.getOrDefault("COURIER", 0L))
                .connectedConsumers(connectionsByType.getOrDefault("CONSUMER", 0L))
                .connectedRestaurants(connectionsByType.getOrDefault("RESTAURANT", 0L))
                .connectedAdmins(connectionsByType.getOrDefault("ADMIN", 0L))
                .connectionsByUserType(connectionsByType)
                .collectedAt(LocalDateTime.now())
                .build();
    }

    private WebSocketMetricsDto.MessageDeliveryMetricsDto collectMessageDeliveryMetrics(
            LocalDateTime startDate, LocalDateTime endDate) {

        Long totalMessages = messageLogRepository.countMessagesInPeriod(startDate, endDate);
        Long delivered = messageLogRepository.countDeliveredMessages(startDate, endDate);
        Long failed = messageLogRepository.countFailedDeliveries(startDate, endDate);

        if (totalMessages == null) totalMessages = 0L;
        if (delivered == null) delivered = 0L;
        if (failed == null) failed = 0L;

        double deliveryRate = totalMessages > 0 ? delivered.doubleValue() / totalMessages : 1.0;

        // Delivery delays
        Double avgDelay = messageLogRepository.getAverageDeliveryDelayMs(startDate, endDate);
        Long maxDelay = messageLogRepository.getMaxDeliveryDelayMs(startDate, endDate);

        // Calculate percentiles
        List<Long> delays = messageLogRepository.getDeliveryDelaysOrdered(startDate, endDate);
        Long p50 = delays.isEmpty() ? 0L : percentileCalculator.calculatePercentile(delays, 50);
        Long p90 = delays.isEmpty() ? 0L : percentileCalculator.calculatePercentile(delays, 90);
        Long p99 = delays.isEmpty() ? 0L : percentileCalculator.calculatePercentile(delays, 99);

        // Messages per second
        long periodSeconds = Duration.between(startDate, endDate).getSeconds();
        if (periodSeconds == 0) periodSeconds = 1;
        double messagesPerSecond = totalMessages.doubleValue() / periodSeconds;

        // Messages by type
        Map<String, Long> messagesByType = new HashMap<>();
        messageLogRepository.getMessageCountByType(startDate, endDate)
                .forEach(row -> messagesByType.put((String) row[0], ((Number) row[1]).longValue()));

        return WebSocketMetricsDto.MessageDeliveryMetricsDto.builder()
                .totalMessagesSent(totalMessages)
                .totalMessagesDelivered(delivered)
                .failedDeliveries(failed)
                .deliverySuccessRate(deliveryRate)
                .avgDeliveryDelayMs(avgDelay)
                .p50DeliveryDelayMs(p50)
                .p90DeliveryDelayMs(p90)
                .p99DeliveryDelayMs(p99)
                .maxDeliveryDelayMs(maxDelay)
                .messagesPerSecond(messagesPerSecond)
                .messagesByType(messagesByType)
                .build();
    }

    private WebSocketMetricsDto.ConnectionMetricsDto collectConnectionMetrics(
            LocalDateTime startDate, LocalDateTime endDate) {

        Long established = connectionLogRepository.countConnectionsInPeriod(startDate, endDate);
        Long disconnections = connectionLogRepository.countDisconnectionsInPeriod(startDate, endDate);
        Double avgDuration = connectionLogRepository.getAverageConnectionDurationSeconds(startDate, endDate);
        Double avgMessages = connectionLogRepository.getAverageMessagesPerConnection(startDate, endDate);

        if (established == null) established = 0L;
        if (disconnections == null) disconnections = 0L;

        // Get peak hour
        Integer peakHour = null;
        List<Object[]> peakData = connectionLogRepository.getPeakHour(startDate, endDate);
        if (!peakData.isEmpty()) {
            peakHour = ((Number) peakData.get(0)[0]).intValue();
        }

        // Connection rate per minute
        long periodMinutes = Duration.between(startDate, endDate).toMinutes();
        if (periodMinutes == 0) periodMinutes = 1;
        double connectionRate = established.doubleValue() / periodMinutes;

        // Max concurrent (estimate based on established - disconnected at any point)
        Long maxConcurrent = connectionLogRepository.getMaxConcurrentConnections(endDate);

        return WebSocketMetricsDto.ConnectionMetricsDto.builder()
                .totalConnectionsEstablished(established)
                .totalDisconnections(disconnections)
                .avgConnectionDurationSeconds(avgDuration)
                .maxConcurrentConnections(maxConcurrent)
                .avgMessagesPerConnection(avgMessages)
                .peakHour(peakHour)
                .connectionRatePerMinute(connectionRate)
                .build();
    }

    private WebSocketMetricsDto.DroppedConnectionsDto collectDroppedConnections(
            LocalDateTime startDate, LocalDateTime endDate) {

        Long dropped = connectionLogRepository.countDroppedConnections(startDate, endDate);
        Long graceful = connectionLogRepository.countGracefulDisconnects(startDate, endDate);
        Long total = connectionLogRepository.countDisconnectionsInPeriod(startDate, endDate);

        if (dropped == null) dropped = 0L;
        if (graceful == null) graceful = 0L;
        if (total == null) total = 0L;

        double droppedRate = total > 0 ? dropped.doubleValue() / total : 0.0;

        // Dropped by reason
        Map<String, Long> byReason = new HashMap<>();
        connectionLogRepository.getDroppedByReason(startDate, endDate)
                .forEach(row -> byReason.put(
                        row[0] != null ? (String) row[0] : "UNKNOWN",
                        ((Number) row[1]).longValue()));

        // Dropped by user type
        Map<String, Long> byUserType = new HashMap<>();
        connectionLogRepository.getDroppedByUserType(startDate, endDate)
                .forEach(row -> byUserType.put(
                        ((WebSocketConnectionLog.UserType) row[0]).name(),
                        ((Number) row[1]).longValue()));

        return WebSocketMetricsDto.DroppedConnectionsDto.builder()
                .totalDropped(dropped)
                .droppedRate(droppedRate)
                .gracefulDisconnects(graceful)
                .forcedDisconnects(dropped)
                .droppedByReason(byReason)
                .droppedByUserType(byUserType)
                .build();
    }

    private List<WebSocketMetricsDto.HourlyConnectionsDto> getHourlyDistribution(
            LocalDateTime startDate, LocalDateTime endDate) {

        return connectionLogRepository.getHourlyDistribution(startDate, endDate).stream()
                .map(row -> WebSocketMetricsDto.HourlyConnectionsDto.builder()
                        .hour(((Number) row[0]).intValue())
                        .activeConnections(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .newConnections(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                        .disconnections(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                        .messagesSent(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Calculate health status for WebSocket.
     */
    public BackendResourceMetricsDto.HealthStatus calculateHealthStatus(WebSocketMetricsDto metrics) {
        // Check delivery success rate
        if (metrics.getMessageDelivery() != null) {
            double successRate = metrics.getMessageDelivery().getDeliverySuccessRate();
            Long p99Delay = metrics.getMessageDelivery().getP99DeliveryDelayMs();

            if (successRate < DELIVERY_SUCCESS_CRITICAL ||
                (p99Delay != null && p99Delay > DELIVERY_DELAY_CRITICAL_MS)) {
                return BackendResourceMetricsDto.HealthStatus.CRITICAL;
            }
            if (successRate < DELIVERY_SUCCESS_WARNING ||
                (p99Delay != null && p99Delay > DELIVERY_DELAY_WARNING_MS)) {
                return BackendResourceMetricsDto.HealthStatus.DEGRADED;
            }
        }

        // Check dropped connection rate
        if (metrics.getDroppedConnections() != null) {
            double droppedRate = metrics.getDroppedConnections().getDroppedRate();
            if (droppedRate > DROPPED_RATE_CRITICAL) {
                return BackendResourceMetricsDto.HealthStatus.CRITICAL;
            }
            if (droppedRate > DROPPED_RATE_WARNING) {
                return BackendResourceMetricsDto.HealthStatus.DEGRADED;
            }
        }

        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }
}

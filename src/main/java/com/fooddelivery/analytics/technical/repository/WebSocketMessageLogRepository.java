package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.WebSocketMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for WebSocket message delivery logs.
 */
@Repository
public interface WebSocketMessageLogRepository extends JpaRepository<WebSocketMessageLog, Long> {

    /**
     * Count total messages in period.
     */
    @Query("SELECT COUNT(m) FROM WebSocketMessageLog m WHERE m.publishedAt BETWEEN :start AND :end")
    Long countMessagesInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count delivered messages.
     */
    @Query("SELECT COUNT(m) FROM WebSocketMessageLog m WHERE m.publishedAt BETWEEN :start AND :end " +
           "AND m.isDelivered = true")
    Long countDeliveredMessages(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count failed deliveries.
     */
    @Query("SELECT COUNT(m) FROM WebSocketMessageLog m WHERE m.publishedAt BETWEEN :start AND :end " +
           "AND m.isDelivered = false")
    Long countFailedDeliveries(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average delivery delay.
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (delivered_at - published_at)) * 1000) " +
            "FROM websocket_message_logs WHERE published_at BETWEEN :start AND :end " +
            "AND is_delivered = true AND delivered_at IS NOT NULL",
            nativeQuery = true)
    Double getAverageDeliveryDelayMs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get max delivery delay.
     */
    @Query(value = "SELECT MAX(EXTRACT(EPOCH FROM (delivered_at - published_at)) * 1000) " +
            "FROM websocket_message_logs WHERE published_at BETWEEN :start AND :end " +
            "AND is_delivered = true AND delivered_at IS NOT NULL",
            nativeQuery = true)
    Long getMaxDeliveryDelayMs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get delivery delays for percentile calculation.
     */
    @Query(value = "SELECT CAST(EXTRACT(EPOCH FROM (delivered_at - published_at)) * 1000 AS bigint) " +
            "FROM websocket_message_logs WHERE published_at BETWEEN :start AND :end " +
            "AND is_delivered = true AND delivered_at IS NOT NULL " +
            "ORDER BY (delivered_at - published_at) ASC",
            nativeQuery = true)
    List<Long> getDeliveryDelaysOrdered(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get message count by type.
     */
    @Query("SELECT m.messageType, COUNT(m) FROM WebSocketMessageLog m " +
           "WHERE m.publishedAt BETWEEN :start AND :end GROUP BY m.messageType")
    List<Object[]> getMessageCountByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average delivery attempts.
     */
    @Query("SELECT AVG(m.deliveryAttempts) FROM WebSocketMessageLog m " +
           "WHERE m.publishedAt BETWEEN :start AND :end")
    Double getAverageDeliveryAttempts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get messages requiring retries.
     */
    @Query("SELECT COUNT(m) FROM WebSocketMessageLog m WHERE m.publishedAt BETWEEN :start AND :end " +
           "AND m.deliveryAttempts > 1")
    Long countMessagesRequiringRetries(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get hourly message distribution.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM published_at) as hour, COUNT(*), " +
            "SUM(CASE WHEN is_delivered = true THEN 1 ELSE 0 END), " +
            "AVG(CASE WHEN is_delivered = true AND delivered_at IS NOT NULL " +
            "THEN EXTRACT(EPOCH FROM (delivered_at - published_at)) * 1000 ELSE NULL END) " +
            "FROM websocket_message_logs WHERE published_at BETWEEN :start AND :end " +
            "GROUP BY EXTRACT(HOUR FROM published_at) ORDER BY hour",
            nativeQuery = true)
    List<Object[]> getHourlyDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get failed messages by reason.
     */
    @Query("SELECT m.errorMessage, COUNT(m) FROM WebSocketMessageLog m " +
           "WHERE m.publishedAt BETWEEN :start AND :end AND m.isDelivered = false " +
           "GROUP BY m.errorMessage")
    List<Object[]> getFailedByReason(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get message throughput per minute.
     */
    @Query(value = "SELECT COUNT(*) * 60.0 / EXTRACT(EPOCH FROM (:endTime - :startTime)) " +
            "FROM websocket_message_logs WHERE published_at BETWEEN :startTime AND :endTime",
            nativeQuery = true)
    Double getMessagesPerMinute(@Param("startTime") LocalDateTime start, @Param("endTime") LocalDateTime end);

    /**
     * Delete old logs for data retention.
     */
    @Modifying
    @Query("DELETE FROM WebSocketMessageLog m WHERE m.publishedAt < :cutoff")
    void deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}

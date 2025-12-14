package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.WebSocketMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
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
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (m.deliveredAt - m.publishedAt)) * 1000) " +
           "FROM WebSocketMessageLog m WHERE m.publishedAt BETWEEN :start AND :end " +
           "AND m.isDelivered = true AND m.deliveredAt IS NOT NULL")
    Double getAverageDeliveryDelayMs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get max delivery delay.
     */
    @Query("SELECT MAX(EXTRACT(EPOCH FROM (m.deliveredAt - m.publishedAt)) * 1000) " +
           "FROM WebSocketMessageLog m WHERE m.publishedAt BETWEEN :start AND :end " +
           "AND m.isDelivered = true AND m.deliveredAt IS NOT NULL")
    Long getMaxDeliveryDelayMs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get delivery delays for percentile calculation.
     */
    @Query("SELECT CAST(EXTRACT(EPOCH FROM (m.deliveredAt - m.publishedAt)) * 1000 AS long) " +
           "FROM WebSocketMessageLog m WHERE m.publishedAt BETWEEN :start AND :end " +
           "AND m.isDelivered = true AND m.deliveredAt IS NOT NULL " +
           "ORDER BY (m.deliveredAt - m.publishedAt) ASC")
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
    @Query("SELECT EXTRACT(HOUR FROM m.publishedAt) as hour, COUNT(m), " +
           "SUM(CASE WHEN m.isDelivered = true THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN m.isDelivered = true AND m.deliveredAt IS NOT NULL " +
           "THEN EXTRACT(EPOCH FROM (m.deliveredAt - m.publishedAt)) * 1000 ELSE NULL END) " +
           "FROM WebSocketMessageLog m WHERE m.publishedAt BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM m.publishedAt) ORDER BY hour")
    List<Object[]> getHourlyDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get failed messages by reason.
     */
    @Query("SELECT m.failureReason, COUNT(m) FROM WebSocketMessageLog m " +
           "WHERE m.publishedAt BETWEEN :start AND :end AND m.isDelivered = false " +
           "GROUP BY m.failureReason")
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
    @Query("DELETE FROM WebSocketMessageLog m WHERE m.publishedAt < :cutoff")
    void deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}

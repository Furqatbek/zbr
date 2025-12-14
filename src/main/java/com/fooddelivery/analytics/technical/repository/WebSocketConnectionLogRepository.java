package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.WebSocketConnectionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for WebSocket connection logs.
 */
@Repository
public interface WebSocketConnectionLogRepository extends JpaRepository<WebSocketConnectionLog, Long> {

    /**
     * Count active connections (connected but not yet disconnected).
     */
    @Query("SELECT COUNT(w) FROM WebSocketConnectionLog w WHERE w.disconnectedAt IS NULL")
    Long countActiveConnections();

    /**
     * Count active connections by user type.
     */
    @Query("SELECT w.userType, COUNT(w) FROM WebSocketConnectionLog w WHERE w.disconnectedAt IS NULL " +
           "GROUP BY w.userType")
    List<Object[]> countActiveConnectionsByUserType();

    /**
     * Count connections established in a period.
     */
    @Query("SELECT COUNT(w) FROM WebSocketConnectionLog w WHERE w.connectedAt BETWEEN :start AND :end")
    Long countConnectionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count disconnections in a period.
     */
    @Query("SELECT COUNT(w) FROM WebSocketConnectionLog w WHERE w.disconnectedAt BETWEEN :start AND :end")
    Long countDisconnectionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get connection count by user type in period.
     */
    @Query("SELECT w.userType, COUNT(w) FROM WebSocketConnectionLog w " +
           "WHERE w.connectedAt BETWEEN :start AND :end GROUP BY w.userType")
    List<Object[]> getConnectionCountByUserType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average connection duration in period.
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (disconnected_at - connected_at))) " +
            "FROM websocket_connection_logs WHERE disconnected_at IS NOT NULL " +
            "AND connected_at BETWEEN :start AND :end",
            nativeQuery = true)
    Double getAverageConnectionDurationSeconds(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get max concurrent connections estimate.
     */
    @Query(value = "SELECT MAX(concurrent) FROM (" +
            "SELECT COUNT(*) as concurrent FROM websocket_connection_logs " +
            "WHERE connected_at <= :checkTime AND (disconnected_at IS NULL OR disconnected_at > :checkTime) " +
            "GROUP BY EXTRACT(HOUR FROM connected_at)) subq",
            nativeQuery = true)
    Long getMaxConcurrentConnections(@Param("checkTime") LocalDateTime checkTime);

    /**
     * Count dropped connections (not gracefully closed).
     */
    @Query("SELECT COUNT(w) FROM WebSocketConnectionLog w WHERE w.disconnectedAt BETWEEN :start AND :end " +
           "AND w.isGracefulDisconnect = false")
    Long countDroppedConnections(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count graceful disconnects.
     */
    @Query("SELECT COUNT(w) FROM WebSocketConnectionLog w WHERE w.disconnectedAt BETWEEN :start AND :end " +
           "AND w.isGracefulDisconnect = true")
    Long countGracefulDisconnects(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get dropped connections by reason.
     */
    @Query("SELECT w.disconnectReason, COUNT(w) FROM WebSocketConnectionLog w " +
           "WHERE w.disconnectedAt BETWEEN :start AND :end AND w.isGracefulDisconnect = false " +
           "GROUP BY w.disconnectReason")
    List<Object[]> getDroppedByReason(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get dropped connections by user type.
     */
    @Query("SELECT w.userType, COUNT(w) FROM WebSocketConnectionLog w " +
           "WHERE w.disconnectedAt BETWEEN :start AND :end AND w.isGracefulDisconnect = false " +
           "GROUP BY w.userType")
    List<Object[]> getDroppedByUserType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average messages per connection.
     */
    @Query("SELECT AVG(w.messagesSent + w.messagesReceived) FROM WebSocketConnectionLog w " +
           "WHERE w.connectedAt BETWEEN :start AND :end")
    Double getAverageMessagesPerConnection(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get hourly connection distribution.
     */
    @Query("SELECT EXTRACT(HOUR FROM w.connectedAt) as hour, " +
           "SUM(CASE WHEN w.disconnectedAt IS NULL OR w.disconnectedAt > :end THEN 1 ELSE 0 END), " +
           "COUNT(w), " +
           "SUM(CASE WHEN w.disconnectedAt BETWEEN :start AND :end THEN 1 ELSE 0 END), " +
           "SUM(w.messagesSent) " +
           "FROM WebSocketConnectionLog w WHERE w.connectedAt BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM w.connectedAt) ORDER BY hour")
    List<Object[]> getHourlyDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get peak connection hour.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM w.connected_at) as hour, COUNT(*) as cnt " +
           "FROM websocket_connection_logs w WHERE w.connected_at BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM w.connected_at) ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> getPeakHour(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get total messages sent in period.
     */
    @Query("SELECT SUM(w.messagesSent) FROM WebSocketConnectionLog w " +
           "WHERE w.connectedAt BETWEEN :start AND :end OR w.disconnectedAt BETWEEN :start AND :end")
    Long getTotalMessagesSent(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Delete old logs for data retention.
     */
    @Modifying
    @Query("DELETE FROM WebSocketConnectionLog w WHERE w.disconnectedAt < :cutoff")
    void deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}

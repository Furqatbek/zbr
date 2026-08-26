package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.MessageQueueStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for message queue statistics (RabbitMQ/Kafka).
 */
@Repository
public interface MessageQueueStatsRepository extends JpaRepository<MessageQueueStats, Long> {

    /**
     * Get latest stats for a queue.
     */
    @Query(value = "SELECT * FROM message_queue_stats WHERE queue_name = :queueName " +
           "ORDER BY recorded_at DESC LIMIT 1", nativeQuery = true)
    Optional<MessageQueueStats> findLatestByQueueName(@Param("queueName") String queueName);

    /**
     * Get latest stats for all queues.
     */
    @Query("SELECT m FROM MessageQueueStats m WHERE m.recordedAt = " +
           "(SELECT MAX(m2.recordedAt) FROM MessageQueueStats m2 WHERE m2.queueName = m.queueName)")
    List<MessageQueueStats> findLatestForAllQueues();

    /**
     * Get total queue depth across all queues.
     */
    @Query("SELECT COALESCE(SUM(m.queueDepth), 0) FROM MessageQueueStats m WHERE m.recordedAt = " +
           "(SELECT MAX(m2.recordedAt) FROM MessageQueueStats m2 WHERE m2.queueName = m.queueName)")
    Long getTotalQueueDepth();

    /**
     * Get total consumer lag.
     */
    @Query("SELECT COALESCE(SUM(m.consumerLag), 0) FROM MessageQueueStats m WHERE m.recordedAt = " +
           "(SELECT MAX(m2.recordedAt) FROM MessageQueueStats m2 WHERE m2.queueName = m.queueName)")
    Long getTotalConsumerLag();

    /**
     * Get average publish rate.
     */
    @Query("SELECT AVG(m.publishRate) FROM MessageQueueStats m WHERE m.recordedAt BETWEEN :start AND :end")
    Double getAveragePublishRate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average consume rate.
     */
    @Query("SELECT AVG(m.consumeRate) FROM MessageQueueStats m WHERE m.recordedAt BETWEEN :start AND :end")
    Double getAverageConsumeRate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get stats history for a queue.
     */
    @Query("SELECT m FROM MessageQueueStats m WHERE m.queueName = :queueName " +
           "AND m.recordedAt BETWEEN :start AND :end ORDER BY m.recordedAt ASC")
    List<MessageQueueStats> getQueueHistory(@Param("queueName") String queueName,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    /**
     * Get queue depth trend.
     */
    @Query("SELECT m.queueName, AVG(m.queueDepth), MAX(m.queueDepth) " +
           "FROM MessageQueueStats m WHERE m.recordedAt BETWEEN :start AND :end " +
           "GROUP BY m.queueName")
    List<Object[]> getQueueDepthSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get queues by broker type.
     */
    @Query("SELECT m FROM MessageQueueStats m WHERE m.brokerType = :brokerType " +
           "AND m.recordedAt = (SELECT MAX(m2.recordedAt) FROM MessageQueueStats m2 " +
           "WHERE m2.queueName = m.queueName)")
    List<MessageQueueStats> findLatestByBrokerType(@Param("brokerType") MessageQueueStats.BrokerType brokerType);

    /**
     * Get dead letter queue count (identified by queue name pattern).
     */
    @Query("SELECT COUNT(m) FROM MessageQueueStats m WHERE (LOWER(m.queueName) LIKE '%dlq%' OR LOWER(m.queueName) LIKE '%dead%') " +
           "AND m.recordedAt = (SELECT MAX(m2.recordedAt) FROM MessageQueueStats m2 " +
           "WHERE m2.queueName = m.queueName) AND m.queueDepth > 0")
    Long countActiveDeadLetterQueues();

    /**
     * Get total messages in dead letter queues (identified by queue name pattern).
     */
    @Query("SELECT COALESCE(SUM(m.queueDepth), 0) FROM MessageQueueStats m WHERE (LOWER(m.queueName) LIKE '%dlq%' OR LOWER(m.queueName) LIKE '%dead%') " +
           "AND m.recordedAt = (SELECT MAX(m2.recordedAt) FROM MessageQueueStats m2 " +
           "WHERE m2.queueName = m.queueName)")
    Long getTotalDeadLetterMessages();

    /**
     * Get hourly queue depth average.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM business_ts(recorded_at)) as hour, AVG(queue_depth) " +
           "FROM message_queue_stats WHERE recorded_at BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM business_ts(recorded_at)) ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourlyQueueDepthAverage(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Delete old stats for data retention.
     */
    @Query("DELETE FROM MessageQueueStats m WHERE m.recordedAt < :cutoff")
    void deleteOldStats(@Param("cutoff") LocalDateTime cutoff);
}

package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.MessageQueueStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    @Query("SELECT m FROM MessageQueueStats m WHERE m.queueName = :queueName " +
           "ORDER BY m.collectedAt DESC LIMIT 1")
    Optional<MessageQueueStats> findLatestByQueueName(@Param("queueName") String queueName);

    /**
     * Get latest stats for all queues.
     */
    @Query("SELECT m FROM MessageQueueStats m WHERE m.collectedAt = " +
           "(SELECT MAX(m2.collectedAt) FROM MessageQueueStats m2 WHERE m2.queueName = m.queueName)")
    List<MessageQueueStats> findLatestForAllQueues();

    /**
     * Get total queue depth across all queues.
     */
    @Query("SELECT COALESCE(SUM(m.queueDepth), 0) FROM MessageQueueStats m WHERE m.collectedAt = " +
           "(SELECT MAX(m2.collectedAt) FROM MessageQueueStats m2 WHERE m2.queueName = m.queueName)")
    Long getTotalQueueDepth();

    /**
     * Get total consumer lag.
     */
    @Query("SELECT COALESCE(SUM(m.consumerLag), 0) FROM MessageQueueStats m WHERE m.collectedAt = " +
           "(SELECT MAX(m2.collectedAt) FROM MessageQueueStats m2 WHERE m2.queueName = m.queueName)")
    Long getTotalConsumerLag();

    /**
     * Get average publish rate.
     */
    @Query("SELECT AVG(m.publishRate) FROM MessageQueueStats m WHERE m.collectedAt BETWEEN :start AND :end")
    Double getAveragePublishRate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average consume rate.
     */
    @Query("SELECT AVG(m.consumeRate) FROM MessageQueueStats m WHERE m.collectedAt BETWEEN :start AND :end")
    Double getAverageConsumeRate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get stats history for a queue.
     */
    @Query("SELECT m FROM MessageQueueStats m WHERE m.queueName = :queueName " +
           "AND m.collectedAt BETWEEN :start AND :end ORDER BY m.collectedAt ASC")
    List<MessageQueueStats> getQueueHistory(@Param("queueName") String queueName,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    /**
     * Get queue depth trend.
     */
    @Query("SELECT m.queueName, AVG(m.queueDepth), MAX(m.queueDepth) " +
           "FROM MessageQueueStats m WHERE m.collectedAt BETWEEN :start AND :end " +
           "GROUP BY m.queueName")
    List<Object[]> getQueueDepthSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get queues by broker type.
     */
    @Query("SELECT m FROM MessageQueueStats m WHERE m.brokerType = :brokerType " +
           "AND m.collectedAt = (SELECT MAX(m2.collectedAt) FROM MessageQueueStats m2 " +
           "WHERE m2.queueName = m.queueName)")
    List<MessageQueueStats> findLatestByBrokerType(@Param("brokerType") MessageQueueStats.BrokerType brokerType);

    /**
     * Get dead letter queue count.
     */
    @Query("SELECT COUNT(m) FROM MessageQueueStats m WHERE m.isDeadLetterQueue = true " +
           "AND m.collectedAt = (SELECT MAX(m2.collectedAt) FROM MessageQueueStats m2 " +
           "WHERE m2.queueName = m.queueName) AND m.queueDepth > 0")
    Long countActiveDeadLetterQueues();

    /**
     * Get total messages in dead letter queues.
     */
    @Query("SELECT COALESCE(SUM(m.queueDepth), 0) FROM MessageQueueStats m WHERE m.isDeadLetterQueue = true " +
           "AND m.collectedAt = (SELECT MAX(m2.collectedAt) FROM MessageQueueStats m2 " +
           "WHERE m2.queueName = m.queueName)")
    Long getTotalDeadLetterMessages();

    /**
     * Get hourly queue depth average.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM m.recorded_at) as hour, AVG(m.queue_depth) " +
           "FROM message_queue_stats m WHERE m.recorded_at BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM m.recorded_at) ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourlyQueueDepthAverage(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Delete old stats for data retention.
     */
    @Modifying
    @Query("DELETE FROM MessageQueueStats m WHERE m.collectedAt < :cutoff")
    void deleteOldStats(@Param("cutoff") LocalDateTime cutoff);
}

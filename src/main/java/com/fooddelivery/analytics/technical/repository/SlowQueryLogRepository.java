package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.SlowQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for slow query logs used in database performance analysis.
 */
@Repository
public interface SlowQueryLogRepository extends JpaRepository<SlowQueryLog, Long> {

    /**
     * Count slow queries in a period.
     */
    @Query("SELECT COUNT(s) FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end")
    Long countSlowQueriesInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count slow queries above threshold.
     */
    @Query("SELECT COUNT(s) FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end " +
           "AND s.durationMs >= :thresholdMs")
    Long countSlowQueriesAboveThreshold(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("thresholdMs") Long thresholdMs);

    /**
     * Get average duration of slow queries.
     */
    @Query("SELECT AVG(s.durationMs) FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end")
    Double getAverageDuration(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get max duration.
     */
    @Query("SELECT MAX(s.durationMs) FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end")
    Long getMaxDuration(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get top slow queries grouped by query hash.
     */
    @Query("SELECT s.queryHash, s.queryText, MAX(s.durationMs), SUM(s.rowsAffected), COUNT(s), " +
           "AVG(s.durationMs), s.tableName, MAX(CASE WHEN s.isUsingIndex = true THEN 1 ELSE 0 END), MAX(s.timestamp) " +
           "FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end " +
           "GROUP BY s.queryHash, s.queryText, s.tableName " +
           "ORDER BY AVG(s.durationMs) DESC")
    List<Object[]> getTopSlowQueries(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get slow query count by query type.
     */
    @Query("SELECT s.queryType, COUNT(s) FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end " +
           "GROUP BY s.queryType")
    List<Object[]> getCountByQueryType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get slow query count by table.
     */
    @Query("SELECT s.tableName, COUNT(s), AVG(s.durationMs) FROM SlowQueryLog s " +
           "WHERE s.timestamp BETWEEN :start AND :end " +
           "GROUP BY s.tableName ORDER BY COUNT(s) DESC")
    List<Object[]> getCountByTable(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get queries not using index.
     */
    @Query("SELECT s FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end " +
           "AND s.isUsingIndex = false ORDER BY s.durationMs DESC")
    List<SlowQueryLog> getQueriesNotUsingIndex(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count queries using index vs not.
     */
    @Query("SELECT s.isUsingIndex, COUNT(s) FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end " +
           "GROUP BY s.isUsingIndex")
    List<Object[]> getIndexUsageCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get hourly slow query distribution.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM business_ts(timestamp)) as hour, COUNT(*), AVG(duration_ms) " +
           "FROM slow_query_logs WHERE timestamp BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM business_ts(timestamp)) ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourlyDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get daily slow query trend.
     */
    @Query("SELECT CAST(s.timestamp AS date), COUNT(s), AVG(s.durationMs), MAX(s.durationMs) " +
           "FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end " +
           "GROUP BY CAST(s.timestamp AS date) ORDER BY CAST(s.timestamp AS date)")
    List<Object[]> getDailyTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get recent slow queries.
     */
    @Query("SELECT s FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end " +
           "ORDER BY s.timestamp DESC")
    List<SlowQueryLog> getRecentSlowQueries(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Delete old logs for data retention.
     */
    @Query("DELETE FROM SlowQueryLog s WHERE s.timestamp < :cutoff")
    void deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}

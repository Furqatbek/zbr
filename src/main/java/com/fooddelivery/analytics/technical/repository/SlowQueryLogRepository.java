package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.SlowQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    @Query(value = "SELECT s.query_hash, s.query_text, MAX(s.duration_ms), SUM(s.rows_affected), COUNT(*), " +
           "AVG(s.duration_ms), s.table_name, BOOL_OR(s.is_using_index), MAX(s.timestamp) " +
           "FROM slow_query_logs s WHERE s.timestamp BETWEEN :start AND :end " +
           "GROUP BY s.query_hash, s.query_text, s.table_name " +
           "ORDER BY AVG(s.duration_ms) DESC", nativeQuery = true)
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
    @Query("SELECT EXTRACT(HOUR FROM s.timestamp) as hour, COUNT(s), AVG(s.durationMs) " +
           "FROM SlowQueryLog s WHERE s.timestamp BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM s.timestamp) ORDER BY hour")
    List<Object[]> getHourlyDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get daily slow query trend.
     */
    @Query(value = "SELECT CAST(s.timestamp AS date), COUNT(*), AVG(s.duration_ms), MAX(s.duration_ms) " +
           "FROM slow_query_logs s WHERE s.timestamp BETWEEN :start AND :end " +
           "GROUP BY CAST(s.timestamp AS date) ORDER BY CAST(s.timestamp AS date)", nativeQuery = true)
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
    @Modifying
    @Query("DELETE FROM SlowQueryLog s WHERE s.timestamp < :cutoff")
    void deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}

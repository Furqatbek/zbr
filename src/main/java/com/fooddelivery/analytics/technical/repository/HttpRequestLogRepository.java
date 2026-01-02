package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.HttpRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for HTTP request logs used in API performance analysis.
 */
@Repository
public interface HttpRequestLogRepository extends JpaRepository<HttpRequestLog, Long> {

    /**
     * Count total requests in a period.
     */
    @Query("SELECT COUNT(h) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end")
    Long countRequestsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count requests by status code range (e.g., 4xx, 5xx).
     */
    @Query("SELECT COUNT(h) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "AND h.statusCode >= :minStatus AND h.statusCode < :maxStatus")
    Long countByStatusCodeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                @Param("minStatus") int minStatus, @Param("maxStatus") int maxStatus);

    /**
     * Count timeout requests.
     */
    @Query("SELECT COUNT(h) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "AND h.isTimeout = true")
    Long countTimeouts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average response time.
     */
    @Query("SELECT AVG(h.responseTimeMs) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end")
    Double getAverageResponseTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get max response time.
     */
    @Query("SELECT MAX(h.responseTimeMs) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end")
    Long getMaxResponseTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get min response time.
     */
    @Query("SELECT MIN(h.responseTimeMs) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end")
    Long getMinResponseTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get all response times for percentile calculation.
     */
    @Query("SELECT h.responseTimeMs FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "ORDER BY h.responseTimeMs ASC")
    List<Long> getAllResponseTimesOrdered(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get request count by endpoint.
     */
    @Query("SELECT h.endpoint, COUNT(h) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "GROUP BY h.endpoint ORDER BY COUNT(h) DESC")
    List<Object[]> getRequestCountByEndpoint(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get error count by endpoint.
     */
    @Query("SELECT h.endpoint, COUNT(h) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "AND h.statusCode >= 400 GROUP BY h.endpoint ORDER BY COUNT(h) DESC")
    List<Object[]> getErrorCountByEndpoint(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average response time by endpoint.
     */
    @Query("SELECT h.endpoint, AVG(h.responseTimeMs) FROM HttpRequestLog h " +
           "WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "GROUP BY h.endpoint ORDER BY AVG(h.responseTimeMs) DESC")
    List<Object[]> getAvgResponseTimeByEndpoint(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get slowest endpoints.
     */
    @Query("SELECT h.endpoint, h.method, AVG(h.responseTimeMs), COUNT(h), " +
           "SUM(CASE WHEN h.statusCode >= 400 THEN 1 ELSE 0 END) " +
           "FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "GROUP BY h.endpoint, h.method ORDER BY AVG(h.responseTimeMs) DESC")
    List<Object[]> getSlowestEndpoints(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get highest error rate endpoints.
     */
    @Query("SELECT h.endpoint, h.method, COUNT(h) as total, " +
           "SUM(CASE WHEN h.statusCode >= 400 THEN 1 ELSE 0 END) as errors " +
           "FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "GROUP BY h.endpoint, h.method " +
           "HAVING COUNT(h) >= :minRequests " +
           "ORDER BY (SUM(CASE WHEN h.statusCode >= 400 THEN 1 ELSE 0 END) * 1.0 / COUNT(h)) DESC")
    List<Object[]> getHighestErrorRateEndpoints(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("minRequests") long minRequests);

    /**
     * Get hourly request distribution.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM request_timestamp) as hour, COUNT(*), AVG(response_time_ms), " +
           "SUM(CASE WHEN status_code >= 400 THEN 1 ELSE 0 END) " +
           "FROM http_request_logs WHERE request_timestamp BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM request_timestamp) ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourlyDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get request count by method.
     */
    @Query("SELECT h.method, COUNT(h) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "GROUP BY h.method")
    List<Object[]> getRequestCountByMethod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get request count by status code.
     */
    @Query("SELECT h.statusCode, COUNT(h) FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "GROUP BY h.statusCode ORDER BY h.statusCode")
    List<Object[]> getRequestCountByStatusCode(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get response times for a specific endpoint for percentile calculation.
     */
    @Query("SELECT h.responseTimeMs FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "AND h.endpoint = :endpoint ORDER BY h.responseTimeMs ASC")
    List<Long> getResponseTimesForEndpoint(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("endpoint") String endpoint);

    /**
     * Get daily request trend.
     */
    @Query("SELECT CAST(h.requestTimestamp AS date), COUNT(h), AVG(h.responseTimeMs), " +
           "SUM(CASE WHEN h.statusCode >= 400 THEN 1 ELSE 0 END) " +
           "FROM HttpRequestLog h WHERE h.requestTimestamp BETWEEN :start AND :end " +
           "GROUP BY CAST(h.requestTimestamp AS date) ORDER BY CAST(h.requestTimestamp AS date)")
    List<Object[]> getDailyTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Delete old logs for data retention.
     */
    @Query("DELETE FROM HttpRequestLog h WHERE h.requestTimestamp < :cutoff")
    void deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}

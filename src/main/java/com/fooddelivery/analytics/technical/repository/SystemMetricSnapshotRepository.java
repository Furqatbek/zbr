package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.SystemMetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for system metric snapshots.
 */
@Repository
public interface SystemMetricSnapshotRepository extends JpaRepository<SystemMetricSnapshot, Long> {

    /**
     * Get latest snapshot for a metric.
     */
    @Query(value = "SELECT * FROM system_metric_snapshots WHERE metric_name = :metricName " +
           "ORDER BY collected_at DESC LIMIT 1", nativeQuery = true)
    Optional<SystemMetricSnapshot> findLatestByMetricName(@Param("metricName") String metricName);

    /**
     * Get latest snapshots for a metric type.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = :metricType " +
           "AND s.collectedAt = (SELECT MAX(s2.collectedAt) FROM SystemMetricSnapshot s2 " +
           "WHERE s2.metricName = s.metricName AND s2.metricType = :metricType)")
    List<SystemMetricSnapshot> findLatestByMetricType(@Param("metricType") SystemMetricSnapshot.MetricType metricType);

    /**
     * Get metric history.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricName = :metricName " +
           "AND s.collectedAt BETWEEN :start AND :end ORDER BY s.collectedAt ASC")
    List<SystemMetricSnapshot> getMetricHistory(@Param("metricName") String metricName,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    /**
     * Get average metric value.
     */
    @Query("SELECT AVG(s.metricValue) FROM SystemMetricSnapshot s WHERE s.metricName = :metricName " +
           "AND s.collectedAt BETWEEN :start AND :end")
    Double getAverageValue(@Param("metricName") String metricName,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    /**
     * Get max metric value.
     */
    @Query("SELECT MAX(s.metricValue) FROM SystemMetricSnapshot s WHERE s.metricName = :metricName " +
           "AND s.collectedAt BETWEEN :start AND :end")
    Double getMaxValue(@Param("metricName") String metricName,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    /**
     * Get min metric value.
     */
    @Query("SELECT MIN(s.metricValue) FROM SystemMetricSnapshot s WHERE s.metricName = :metricName " +
           "AND s.collectedAt BETWEEN :start AND :end")
    Double getMinValue(@Param("metricName") String metricName,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    /**
     * Get CPU usage metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'CPU' " +
           "AND s.collectedAt BETWEEN :start AND :end ORDER BY s.collectedAt ASC")
    List<SystemMetricSnapshot> getCpuMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get memory usage metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'MEMORY' " +
           "AND s.collectedAt BETWEEN :start AND :end ORDER BY s.collectedAt ASC")
    List<SystemMetricSnapshot> getMemoryMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get JVM metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'JVM' " +
           "AND s.collectedAt BETWEEN :start AND :end ORDER BY s.collectedAt ASC")
    List<SystemMetricSnapshot> getJvmMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get database connection pool metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'DATABASE' " +
           "AND s.collectedAt BETWEEN :start AND :end ORDER BY s.collectedAt ASC")
    List<SystemMetricSnapshot> getDatabasePoolMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get cache (Redis) metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'CACHE' " +
           "AND s.collectedAt BETWEEN :start AND :end ORDER BY s.collectedAt ASC")
    List<SystemMetricSnapshot> getCacheMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get hourly average for a metric.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM collected_at) as hour, AVG(metric_value) " +
           "FROM system_metric_snapshots WHERE metric_name = :metricName " +
           "AND collected_at BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM collected_at) ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourlyAverage(@Param("metricName") String metricName,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /**
     * Get daily average for a metric.
     */
    @Query("SELECT CAST(s.collectedAt AS date), AVG(s.metricValue), MAX(s.metricValue), MIN(s.metricValue) " +
           "FROM SystemMetricSnapshot s WHERE s.metricName = :metricName " +
           "AND s.collectedAt BETWEEN :start AND :end " +
           "GROUP BY CAST(s.collectedAt AS date) ORDER BY CAST(s.collectedAt AS date)")
    List<Object[]> getDailyAverage(@Param("metricName") String metricName,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    /**
     * Get all metrics at a specific time.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.collectedAt = " +
           "(SELECT MAX(s2.collectedAt) FROM SystemMetricSnapshot s2 WHERE s2.collectedAt <= :asOf)")
    List<SystemMetricSnapshot> getMetricsAt(@Param("asOf") LocalDateTime asOf);

    /**
     * Get metric names by type.
     */
    @Query("SELECT DISTINCT s.metricName FROM SystemMetricSnapshot s WHERE s.metricType = :metricType")
    List<String> getMetricNamesByType(@Param("metricType") SystemMetricSnapshot.MetricType metricType);

    /**
     * Delete old snapshots for data retention.
     */
    @Query("DELETE FROM SystemMetricSnapshot s WHERE s.collectedAt < :cutoff")
    void deleteOldSnapshots(@Param("cutoff") LocalDateTime cutoff);
}

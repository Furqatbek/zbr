package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.SystemMetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    @Query(value = "SELECT * FROM system_metric_snapshots s WHERE s.metric_name = :metricName " +
           "ORDER BY s.recorded_at DESC LIMIT 1", nativeQuery = true)
    Optional<SystemMetricSnapshot> findLatestByMetricName(@Param("metricName") String metricName);

    /**
     * Get latest snapshots for a metric type.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = :metricType " +
           "AND s.recordedAt = (SELECT MAX(s2.recordedAt) FROM SystemMetricSnapshot s2 " +
           "WHERE s2.metricName = s.metricName AND s2.metricType = :metricType)")
    List<SystemMetricSnapshot> findLatestByMetricType(@Param("metricType") SystemMetricSnapshot.MetricType metricType);

    /**
     * Get metric history.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricName = :metricName " +
           "AND s.recordedAt BETWEEN :start AND :end ORDER BY s.recordedAt ASC")
    List<SystemMetricSnapshot> getMetricHistory(@Param("metricName") String metricName,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    /**
     * Get average metric value.
     */
    @Query("SELECT AVG(s.metricValue) FROM SystemMetricSnapshot s WHERE s.metricName = :metricName " +
           "AND s.recordedAt BETWEEN :start AND :end")
    Double getAverageValue(@Param("metricName") String metricName,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    /**
     * Get max metric value.
     */
    @Query("SELECT MAX(s.metricValue) FROM SystemMetricSnapshot s WHERE s.metricName = :metricName " +
           "AND s.recordedAt BETWEEN :start AND :end")
    Double getMaxValue(@Param("metricName") String metricName,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    /**
     * Get min metric value.
     */
    @Query("SELECT MIN(s.metricValue) FROM SystemMetricSnapshot s WHERE s.metricName = :metricName " +
           "AND s.recordedAt BETWEEN :start AND :end")
    Double getMinValue(@Param("metricName") String metricName,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    /**
     * Get CPU usage metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'CPU' " +
           "AND s.recordedAt BETWEEN :start AND :end ORDER BY s.recordedAt ASC")
    List<SystemMetricSnapshot> getCpuMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get memory usage metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'MEMORY' " +
           "AND s.recordedAt BETWEEN :start AND :end ORDER BY s.recordedAt ASC")
    List<SystemMetricSnapshot> getMemoryMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get JVM metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'JVM' " +
           "AND s.recordedAt BETWEEN :start AND :end ORDER BY s.recordedAt ASC")
    List<SystemMetricSnapshot> getJvmMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get database connection pool metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'DATABASE' " +
           "AND s.recordedAt BETWEEN :start AND :end ORDER BY s.recordedAt ASC")
    List<SystemMetricSnapshot> getDatabasePoolMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get cache (Redis) metrics.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.metricType = 'CACHE' " +
           "AND s.recordedAt BETWEEN :start AND :end ORDER BY s.recordedAt ASC")
    List<SystemMetricSnapshot> getCacheMetrics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get hourly average for a metric.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM s.recorded_at) as hour, AVG(s.metric_value) " +
           "FROM system_metric_snapshots s WHERE s.metric_name = :metricName " +
           "AND s.recorded_at BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM s.recorded_at) ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourlyAverage(@Param("metricName") String metricName,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /**
     * Get daily average for a metric.
     */
    @Query(value = "SELECT CAST(s.recorded_at AS date), AVG(s.metric_value), MAX(s.metric_value), MIN(s.metric_value) " +
           "FROM system_metric_snapshots s WHERE s.metric_name = :metricName " +
           "AND s.recorded_at BETWEEN :start AND :end " +
           "GROUP BY CAST(s.recorded_at AS date) ORDER BY CAST(s.recorded_at AS date)", nativeQuery = true)
    List<Object[]> getDailyAverage(@Param("metricName") String metricName,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    /**
     * Get all metrics at a specific time.
     */
    @Query("SELECT s FROM SystemMetricSnapshot s WHERE s.recordedAt = " +
           "(SELECT MAX(s2.recordedAt) FROM SystemMetricSnapshot s2 WHERE s2.recordedAt <= :asOf)")
    List<SystemMetricSnapshot> getMetricsAt(@Param("asOf") LocalDateTime asOf);

    /**
     * Get metric names by type.
     */
    @Query("SELECT DISTINCT s.metricName FROM SystemMetricSnapshot s WHERE s.metricType = :metricType")
    List<String> getMetricNamesByType(@Param("metricType") SystemMetricSnapshot.MetricType metricType);

    /**
     * Delete old snapshots for data retention.
     */
    @Modifying
    @Query("DELETE FROM SystemMetricSnapshot s WHERE s.recordedAt < :cutoff")
    void deleteOldSnapshots(@Param("cutoff") LocalDateTime cutoff);
}

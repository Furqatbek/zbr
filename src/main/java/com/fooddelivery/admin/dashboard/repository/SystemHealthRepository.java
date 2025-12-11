package com.fooddelivery.admin.dashboard.repository;

import com.fooddelivery.admin.dashboard.model.SystemHealthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for system health snapshots.
 */
@Repository
public interface SystemHealthRepository extends JpaRepository<SystemHealthSnapshot, Long> {

    /**
     * Find the most recent health snapshot.
     */
    @Query("SELECT s FROM SystemHealthSnapshot s ORDER BY s.capturedAt DESC LIMIT 1")
    Optional<SystemHealthSnapshot> findLatestSnapshot();

    /**
     * Find recent health snapshots.
     */
    @Query("SELECT s FROM SystemHealthSnapshot s WHERE s.capturedAt > :since " +
            "ORDER BY s.capturedAt DESC")
    List<SystemHealthSnapshot> findRecentSnapshots(@Param("since") LocalDateTime since);

    /**
     * Find health snapshots by component.
     */
    @Query("SELECT s FROM SystemHealthSnapshot s WHERE s.component = :component " +
            "AND s.capturedAt > :since ORDER BY s.capturedAt DESC")
    List<SystemHealthSnapshot> findByComponent(@Param("component") String component,
                                                @Param("since") LocalDateTime since);

    /**
     * Average API latency P50 for the last hour.
     */
    @Query("SELECT COALESCE(AVG(s.apiLatencyP50), 0) FROM SystemHealthSnapshot s " +
            "WHERE s.capturedAt > :since")
    Double avgApiLatencyP50(@Param("since") LocalDateTime since);

    /**
     * Average API latency P90 for the last hour.
     */
    @Query("SELECT COALESCE(AVG(s.apiLatencyP90), 0) FROM SystemHealthSnapshot s " +
            "WHERE s.capturedAt > :since")
    Double avgApiLatencyP90(@Param("since") LocalDateTime since);

    /**
     * Average DB connections for the last hour.
     */
    @Query("SELECT COALESCE(AVG(s.dbActiveConnections), 0) FROM SystemHealthSnapshot s " +
            "WHERE s.capturedAt > :since")
    Double avgDbConnections(@Param("since") LocalDateTime since);

    /**
     * Average Redis latency for the last hour.
     */
    @Query("SELECT COALESCE(AVG(s.redisLatencyMs), 0) FROM SystemHealthSnapshot s " +
            "WHERE s.capturedAt > :since")
    Double avgRedisLatency(@Param("since") LocalDateTime since);

    /**
     * Delete old snapshots (cleanup).
     */
    void deleteByCapturedAtBefore(LocalDateTime before);
}

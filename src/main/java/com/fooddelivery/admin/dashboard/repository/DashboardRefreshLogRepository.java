package com.fooddelivery.admin.dashboard.repository;

import com.fooddelivery.admin.dashboard.model.DashboardRefreshLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for dashboard refresh logs.
 */
@Repository
public interface DashboardRefreshLogRepository extends JpaRepository<DashboardRefreshLog, Long> {

    /**
     * Find the most recent refresh log for a component.
     */
    @Query("SELECT r FROM DashboardRefreshLog r WHERE r.component = :component " +
            "ORDER BY r.refreshedAt DESC LIMIT 1")
    Optional<DashboardRefreshLog> findLatestByComponent(@Param("component") String component);

    /**
     * Find recent refresh logs.
     */
    @Query("SELECT r FROM DashboardRefreshLog r WHERE r.refreshedAt > :since " +
            "ORDER BY r.refreshedAt DESC")
    List<DashboardRefreshLog> findRecentLogs(@Param("since") LocalDateTime since);

    /**
     * Find failed refresh logs.
     */
    @Query("SELECT r FROM DashboardRefreshLog r WHERE r.success = false " +
            "AND r.refreshedAt > :since ORDER BY r.refreshedAt DESC")
    List<DashboardRefreshLog> findFailedLogs(@Param("since") LocalDateTime since);

    /**
     * Average refresh duration for a component.
     */
    @Query("SELECT COALESCE(AVG(r.durationMs), 0) FROM DashboardRefreshLog r " +
            "WHERE r.component = :component AND r.refreshedAt > :since")
    Double avgDurationByComponent(@Param("component") String component,
                                   @Param("since") LocalDateTime since);

    /**
     * Count refreshes for a component.
     */
    @Query("SELECT COUNT(r) FROM DashboardRefreshLog r WHERE r.component = :component " +
            "AND r.refreshedAt > :since")
    Long countByComponent(@Param("component") String component,
                          @Param("since") LocalDateTime since);

    /**
     * Delete old logs (cleanup).
     */
    void deleteByRefreshedAtBefore(LocalDateTime before);
}

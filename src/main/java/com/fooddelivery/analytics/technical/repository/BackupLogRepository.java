package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.BackupLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for backup logs.
 */
@Repository
public interface BackupLogRepository extends JpaRepository<BackupLog, Long> {

    /**
     * Count backups in period.
     */
    @Query("SELECT COUNT(b) FROM BackupLog b WHERE b.startedAt BETWEEN :start AND :end")
    Long countBackupsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count successful backups.
     */
    @Query("SELECT COUNT(b) FROM BackupLog b WHERE b.startedAt BETWEEN :start AND :end " +
           "AND b.status = 'COMPLETED'")
    Long countSuccessfulBackups(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count failed backups.
     */
    @Query("SELECT COUNT(b) FROM BackupLog b WHERE b.startedAt BETWEEN :start AND :end " +
           "AND b.status = 'FAILED'")
    Long countFailedBackups(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average backup duration.
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (b.completed_at - b.started_at))) " +
           "FROM backup_logs b WHERE b.started_at BETWEEN :start AND :end " +
           "AND b.status = 'COMPLETED' AND b.completed_at IS NOT NULL", nativeQuery = true)
    Double getAverageBackupDurationSeconds(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average backup size.
     */
    @Query("SELECT AVG(b.backupSizeBytes) FROM BackupLog b WHERE b.startedAt BETWEEN :start AND :end " +
           "AND b.status = 'COMPLETED'")
    Double getAverageBackupSize(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get last successful backup.
     */
    @Query("SELECT b FROM BackupLog b WHERE b.status = 'COMPLETED' ORDER BY b.completedAt DESC LIMIT 1")
    Optional<BackupLog> findLastSuccessfulBackup();

    /**
     * Get last backup (any status).
     */
    @Query("SELECT b FROM BackupLog b ORDER BY b.startedAt DESC LIMIT 1")
    Optional<BackupLog> findLastBackup();

    /**
     * Get backup history.
     */
    @Query("SELECT b FROM BackupLog b WHERE b.startedAt BETWEEN :start AND :end ORDER BY b.startedAt DESC")
    List<BackupLog> getBackupHistory(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get backups by type.
     */
    @Query("SELECT b.backupType, COUNT(b), SUM(CASE WHEN b.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN b.status = 'FAILED' THEN 1 ELSE 0 END) " +
           "FROM BackupLog b WHERE b.startedAt BETWEEN :start AND :end GROUP BY b.backupType")
    List<Object[]> getBackupsByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get failed backup reasons.
     */
    @Query("SELECT b.errorMessage, COUNT(b) FROM BackupLog b WHERE b.startedAt BETWEEN :start AND :end " +
           "AND b.status = 'FAILED' GROUP BY b.errorMessage")
    List<Object[]> getFailedBackupReasons(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get backup stats by database.
     */
    @Query("SELECT b.databaseName, COUNT(b), " +
           "SUM(CASE WHEN b.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN b.status = 'COMPLETED' THEN b.backupSizeBytes ELSE NULL END) " +
           "FROM BackupLog b WHERE b.startedAt BETWEEN :start AND :end GROUP BY b.databaseName")
    List<Object[]> getBackupStatsByDatabase(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Check if backup is currently running.
     */
    @Query("SELECT COUNT(b) > 0 FROM BackupLog b WHERE b.status = 'IN_PROGRESS'")
    Boolean isBackupInProgress();

    /**
     * Get in-progress backups.
     */
    @Query("SELECT b FROM BackupLog b WHERE b.status = 'IN_PROGRESS'")
    List<BackupLog> findInProgressBackups();

    /**
     * Get verified (completed) backups count.
     */
    @Query("SELECT COUNT(b) FROM BackupLog b WHERE b.startedAt BETWEEN :start AND :end " +
           "AND b.status = 'COMPLETED'")
    Long countVerifiedBackups(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Delete old backup logs for data retention.
     */
    @Modifying
    @Query("DELETE FROM BackupLog b WHERE b.startedAt < :cutoff")
    void deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}

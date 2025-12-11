package com.fooddelivery.analytics.technical.repository;

import com.fooddelivery.analytics.technical.model.StorageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for storage metadata used in storage/upload metrics.
 */
@Repository
public interface StorageMetadataRepository extends JpaRepository<StorageMetadata, Long> {

    /**
     * Count total files.
     */
    @Query("SELECT COUNT(s) FROM StorageMetadata s WHERE s.isDeleted = false")
    Long countTotalFiles();

    /**
     * Get total storage size.
     */
    @Query("SELECT COALESCE(SUM(s.fileSizeBytes), 0) FROM StorageMetadata s WHERE s.isDeleted = false")
    Long getTotalStorageSize();

    /**
     * Count uploads in period.
     */
    @Query("SELECT COUNT(s) FROM StorageMetadata s WHERE s.uploadedAt BETWEEN :start AND :end")
    Long countUploadsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count successful uploads.
     */
    @Query("SELECT COUNT(s) FROM StorageMetadata s WHERE s.uploadedAt BETWEEN :start AND :end " +
           "AND s.isUploadSuccessful = true")
    Long countSuccessfulUploads(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count failed uploads.
     */
    @Query("SELECT COUNT(s) FROM StorageMetadata s WHERE s.uploadedAt BETWEEN :start AND :end " +
           "AND s.isUploadSuccessful = false")
    Long countFailedUploads(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average upload latency.
     */
    @Query("SELECT AVG(s.uploadDurationMs) FROM StorageMetadata s WHERE s.uploadedAt BETWEEN :start AND :end " +
           "AND s.isUploadSuccessful = true")
    Double getAverageUploadLatency(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get max upload latency.
     */
    @Query("SELECT MAX(s.uploadDurationMs) FROM StorageMetadata s WHERE s.uploadedAt BETWEEN :start AND :end " +
           "AND s.isUploadSuccessful = true")
    Long getMaxUploadLatency(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get upload latencies for percentile calculation.
     */
    @Query("SELECT s.uploadDurationMs FROM StorageMetadata s WHERE s.uploadedAt BETWEEN :start AND :end " +
           "AND s.isUploadSuccessful = true ORDER BY s.uploadDurationMs ASC")
    List<Long> getUploadLatenciesOrdered(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average upload size.
     */
    @Query("SELECT AVG(s.fileSizeBytes) FROM StorageMetadata s WHERE s.uploadedAt BETWEEN :start AND :end " +
           "AND s.isUploadSuccessful = true")
    Double getAverageUploadSize(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get total bytes uploaded.
     */
    @Query("SELECT COALESCE(SUM(s.fileSizeBytes), 0) FROM StorageMetadata s " +
           "WHERE s.uploadedAt BETWEEN :start AND :end AND s.isUploadSuccessful = true")
    Long getTotalBytesUploaded(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get storage breakdown by content type.
     */
    @Query("SELECT s.contentType, COUNT(s), SUM(s.fileSizeBytes), AVG(s.fileSizeBytes) " +
           "FROM StorageMetadata s WHERE s.isDeleted = false GROUP BY s.contentType")
    List<Object[]> getStorageByContentType();

    /**
     * Get storage breakdown by entity type.
     */
    @Query("SELECT s.entityType, COUNT(s), SUM(s.fileSizeBytes), AVG(s.fileSizeBytes) " +
           "FROM StorageMetadata s WHERE s.isDeleted = false GROUP BY s.entityType")
    List<Object[]> getStorageByEntityType();

    /**
     * Get storage breakdown by storage type.
     */
    @Query("SELECT s.storageType, COUNT(s), SUM(s.fileSizeBytes), AVG(s.fileSizeBytes) " +
           "FROM StorageMetadata s WHERE s.isDeleted = false GROUP BY s.storageType")
    List<Object[]> getStorageByStorageType();

    /**
     * Count images.
     */
    @Query("SELECT COUNT(s) FROM StorageMetadata s WHERE s.isDeleted = false " +
           "AND s.contentType LIKE 'image/%'")
    Long countImages();

    /**
     * Get total image size.
     */
    @Query("SELECT COALESCE(SUM(s.fileSizeBytes), 0) FROM StorageMetadata s WHERE s.isDeleted = false " +
           "AND s.contentType LIKE 'image/%'")
    Long getTotalImageSize();

    /**
     * Get average image size.
     */
    @Query("SELECT AVG(s.fileSizeBytes) FROM StorageMetadata s WHERE s.isDeleted = false " +
           "AND s.contentType LIKE 'image/%'")
    Double getAverageImageSize();

    /**
     * Get image count by content type.
     */
    @Query("SELECT s.contentType, COUNT(s) FROM StorageMetadata s WHERE s.isDeleted = false " +
           "AND s.contentType LIKE 'image/%' GROUP BY s.contentType")
    List<Object[]> getImageCountByContentType();

    /**
     * Get daily upload trend.
     */
    @Query("SELECT CAST(s.uploadedAt AS date), COUNT(s), SUM(s.fileSizeBytes), " +
           "SUM(CASE WHEN s.isUploadSuccessful = false THEN 1 ELSE 0 END), AVG(s.uploadDurationMs) " +
           "FROM StorageMetadata s WHERE s.uploadedAt BETWEEN :start AND :end " +
           "GROUP BY CAST(s.uploadedAt AS date) ORDER BY CAST(s.uploadedAt AS date)")
    List<Object[]> getDailyUploadTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get failed uploads by reason.
     */
    @Query("SELECT s.uploadErrorReason, COUNT(s) FROM StorageMetadata s " +
           "WHERE s.uploadedAt BETWEEN :start AND :end AND s.isUploadSuccessful = false " +
           "GROUP BY s.uploadErrorReason")
    List<Object[]> getFailedByReason(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get storage size at a point in time.
     */
    @Query("SELECT COALESCE(SUM(s.fileSizeBytes), 0) FROM StorageMetadata s " +
           "WHERE s.uploadedAt <= :asOf AND (s.isDeleted = false OR s.deletedAt > :asOf)")
    Long getStorageSizeAt(@Param("asOf") LocalDateTime asOf);

    /**
     * Count files added in period.
     */
    @Query("SELECT COUNT(s) FROM StorageMetadata s WHERE s.uploadedAt BETWEEN :start AND :end " +
           "AND s.isUploadSuccessful = true")
    Long countFilesAddedInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count files deleted in period.
     */
    @Query("SELECT COUNT(s) FROM StorageMetadata s WHERE s.deletedAt BETWEEN :start AND :end")
    Long countFilesDeletedInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Delete records for permanently removed files older than retention.
     */
    @Query("DELETE FROM StorageMetadata s WHERE s.isDeleted = true AND s.deletedAt < :cutoff")
    void deleteOldRecords(@Param("cutoff") LocalDateTime cutoff);
}

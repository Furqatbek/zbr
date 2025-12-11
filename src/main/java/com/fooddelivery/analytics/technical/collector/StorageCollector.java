package com.fooddelivery.analytics.technical.collector;

import com.fooddelivery.analytics.technical.dto.BackendResourceMetricsDto;
import com.fooddelivery.analytics.technical.dto.StorageMetricsDto;
import com.fooddelivery.analytics.technical.repository.StorageMetadataRepository;
import com.fooddelivery.analytics.technical.util.PercentileCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collector for storage and image upload metrics.
 * Sources: Storage metadata, upload logs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorageCollector {

    private final StorageMetadataRepository storageMetadataRepository;
    private final PercentileCalculator percentileCalculator;

    // Thresholds
    private static final double UPLOAD_SUCCESS_WARNING = 0.99;
    private static final double UPLOAD_SUCCESS_CRITICAL = 0.95;
    private static final long UPLOAD_LATENCY_WARNING_MS = 2000;
    private static final long UPLOAD_LATENCY_CRITICAL_MS = 5000;
    private static final double STORAGE_GROWTH_WARNING = 0.10; // 10% per period
    private static final double STORAGE_GROWTH_CRITICAL = 0.25; // 25% per period

    // Conversion constants
    private static final double BYTES_TO_MB = 1024.0 * 1024.0;
    private static final double BYTES_TO_GB = 1024.0 * 1024.0 * 1024.0;

    /**
     * Collect storage metrics for a period.
     */
    public StorageMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate, boolean includeDailyTrend) {
        log.debug("Collecting storage metrics from {} to {}", startDate, endDate);

        // Total storage stats
        Long totalFiles = storageMetadataRepository.countTotalFiles();
        Long totalSize = storageMetadataRepository.getTotalStorageSize();

        if (totalFiles == null) totalFiles = 0L;
        if (totalSize == null) totalSize = 0L;

        // Image metrics
        StorageMetricsDto.ImageMetricsDto imageMetrics = collectImageMetrics(totalSize);

        // Upload performance
        StorageMetricsDto.UploadPerformanceDto uploadPerformance =
                collectUploadPerformance(startDate, endDate);

        // Storage breakdown by type
        Map<String, StorageMetricsDto.StorageBreakdownDto> storageByType =
                getStorageByContentType(totalSize);

        // Storage breakdown by entity type
        Map<String, StorageMetricsDto.StorageBreakdownDto> storageByEntityType =
                getStorageByEntityType(totalSize);

        // Daily upload trend
        List<StorageMetricsDto.DailyUploadDto> dailyTrend = null;
        if (includeDailyTrend) {
            dailyTrend = getDailyUploadTrend(startDate, endDate);
        }

        // Storage growth
        StorageMetricsDto.StorageGrowthDto storageGrowth =
                calculateStorageGrowth(startDate, endDate);

        return StorageMetricsDto.builder()
                .totalFileCount(totalFiles)
                .totalStorageSizeBytes(totalSize)
                .totalStorageSizeMb(totalSize / BYTES_TO_MB)
                .totalStorageSizeGb(totalSize / BYTES_TO_GB)
                .imageMetrics(imageMetrics)
                .uploadPerformance(uploadPerformance)
                .storageByType(storageByType)
                .storageByEntityType(storageByEntityType)
                .dailyUploadTrend(dailyTrend)
                .storageGrowth(storageGrowth)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    /**
     * Collect current storage summary.
     */
    public StorageMetricsDto collectSummary() {
        log.debug("Collecting storage summary");

        Long totalFiles = storageMetadataRepository.countTotalFiles();
        Long totalSize = storageMetadataRepository.getTotalStorageSize();

        if (totalFiles == null) totalFiles = 0L;
        if (totalSize == null) totalSize = 0L;

        return StorageMetricsDto.builder()
                .totalFileCount(totalFiles)
                .totalStorageSizeBytes(totalSize)
                .totalStorageSizeMb(totalSize / BYTES_TO_MB)
                .totalStorageSizeGb(totalSize / BYTES_TO_GB)
                .periodEnd(LocalDateTime.now())
                .build();
    }

    private StorageMetricsDto.ImageMetricsDto collectImageMetrics(Long totalStorageSize) {
        Long imageCount = storageMetadataRepository.countImages();
        Long imageSize = storageMetadataRepository.getTotalImageSize();
        Double avgImageSize = storageMetadataRepository.getAverageImageSize();

        if (imageCount == null) imageCount = 0L;
        if (imageSize == null) imageSize = 0L;

        double imagePercentage = totalStorageSize > 0 ?
                (imageSize.doubleValue() / totalStorageSize) * 100 : 0.0;

        // Images by content type
        Map<String, Long> byContentType = new HashMap<>();
        storageMetadataRepository.getImageCountByContentType()
                .forEach(row -> byContentType.put((String) row[0], ((Number) row[1]).longValue()));

        return StorageMetricsDto.ImageMetricsDto.builder()
                .totalImageCount(imageCount)
                .totalImageSizeBytes(imageSize)
                .avgImageSizeBytes(avgImageSize)
                .imagesByContentType(byContentType)
                .imageStoragePercentage(imagePercentage)
                .build();
    }

    private StorageMetricsDto.UploadPerformanceDto collectUploadPerformance(
            LocalDateTime startDate, LocalDateTime endDate) {

        Long totalUploads = storageMetadataRepository.countUploadsInPeriod(startDate, endDate);
        Long successful = storageMetadataRepository.countSuccessfulUploads(startDate, endDate);
        Long failed = storageMetadataRepository.countFailedUploads(startDate, endDate);

        if (totalUploads == null) totalUploads = 0L;
        if (successful == null) successful = 0L;
        if (failed == null) failed = 0L;

        double successRate = totalUploads > 0 ? successful.doubleValue() / totalUploads : 1.0;

        // Latency stats
        Double avgLatency = storageMetadataRepository.getAverageUploadLatency(startDate, endDate);
        Long maxLatency = storageMetadataRepository.getMaxUploadLatency(startDate, endDate);

        // Calculate percentiles
        List<Long> latencies = storageMetadataRepository.getUploadLatenciesOrdered(startDate, endDate);
        Long p50 = latencies.isEmpty() ? 0L : percentileCalculator.calculatePercentile(latencies, 50);
        Long p90 = latencies.isEmpty() ? 0L : percentileCalculator.calculatePercentile(latencies, 90);
        Long p99 = latencies.isEmpty() ? 0L : percentileCalculator.calculatePercentile(latencies, 99);

        // Upload rate
        long periodMinutes = Duration.between(startDate, endDate).toMinutes();
        if (periodMinutes == 0) periodMinutes = 1;
        double uploadsPerMinute = totalUploads.doubleValue() / periodMinutes;

        // Size stats
        Double avgSize = storageMetadataRepository.getAverageUploadSize(startDate, endDate);
        Long totalBytes = storageMetadataRepository.getTotalBytesUploaded(startDate, endDate);

        // Failures by reason
        Map<String, Long> failuresByReason = new HashMap<>();
        storageMetadataRepository.getFailedByReason(startDate, endDate)
                .forEach(row -> failuresByReason.put(
                        row[0] != null ? (String) row[0] : "UNKNOWN",
                        ((Number) row[1]).longValue()));

        return StorageMetricsDto.UploadPerformanceDto.builder()
                .totalUploads(totalUploads)
                .successfulUploads(successful)
                .failedUploads(failed)
                .successRate(successRate)
                .avgUploadLatencyMs(avgLatency)
                .p50UploadLatencyMs(p50)
                .p90UploadLatencyMs(p90)
                .p99UploadLatencyMs(p99)
                .maxUploadLatencyMs(maxLatency)
                .uploadsPerMinute(uploadsPerMinute)
                .avgUploadSizeBytes(avgSize)
                .totalBytesUploaded(totalBytes)
                .failuresByReason(failuresByReason)
                .build();
    }

    private Map<String, StorageMetricsDto.StorageBreakdownDto> getStorageByContentType(Long totalSize) {
        Map<String, StorageMetricsDto.StorageBreakdownDto> breakdown = new HashMap<>();

        storageMetadataRepository.getStorageByContentType().forEach(row -> {
            String type = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            Long size = ((Number) row[2]).longValue();
            Double avgSize = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            double percentage = totalSize > 0 ? (size.doubleValue() / totalSize) * 100 : 0.0;

            breakdown.put(type, StorageMetricsDto.StorageBreakdownDto.builder()
                    .type(type)
                    .fileCount(count)
                    .totalSizeBytes(size)
                    .avgFileSizeBytes(avgSize)
                    .percentageOfTotal(percentage)
                    .build());
        });

        return breakdown;
    }

    private Map<String, StorageMetricsDto.StorageBreakdownDto> getStorageByEntityType(Long totalSize) {
        Map<String, StorageMetricsDto.StorageBreakdownDto> breakdown = new HashMap<>();

        storageMetadataRepository.getStorageByEntityType().forEach(row -> {
            String type = row[0] != null ? (String) row[0] : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();
            Long size = ((Number) row[2]).longValue();
            Double avgSize = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            double percentage = totalSize > 0 ? (size.doubleValue() / totalSize) * 100 : 0.0;

            breakdown.put(type, StorageMetricsDto.StorageBreakdownDto.builder()
                    .type(type)
                    .fileCount(count)
                    .totalSizeBytes(size)
                    .avgFileSizeBytes(avgSize)
                    .percentageOfTotal(percentage)
                    .build());
        });

        return breakdown;
    }

    private List<StorageMetricsDto.DailyUploadDto> getDailyUploadTrend(
            LocalDateTime startDate, LocalDateTime endDate) {

        return storageMetadataRepository.getDailyUploadTrend(startDate, endDate).stream()
                .map(row -> StorageMetricsDto.DailyUploadDto.builder()
                        .date(row[0] != null ? ((java.sql.Date) row[0]).toLocalDate() : LocalDate.now())
                        .uploadCount(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .totalSizeBytes(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                        .failedCount(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                        .avgLatencyMs(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    private StorageMetricsDto.StorageGrowthDto calculateStorageGrowth(
            LocalDateTime startDate, LocalDateTime endDate) {

        Long startSize = storageMetadataRepository.getStorageSizeAt(startDate);
        Long endSize = storageMetadataRepository.getStorageSizeAt(endDate);
        Long filesAdded = storageMetadataRepository.countFilesAddedInPeriod(startDate, endDate);
        Long filesDeleted = storageMetadataRepository.countFilesDeletedInPeriod(startDate, endDate);

        if (startSize == null) startSize = 0L;
        if (endSize == null) endSize = storageMetadataRepository.getTotalStorageSize();
        if (endSize == null) endSize = 0L;
        if (filesAdded == null) filesAdded = 0L;
        if (filesDeleted == null) filesDeleted = 0L;

        long growth = endSize - startSize;
        double growthPercentage = startSize > 0 ? (growth * 100.0 / startSize) : 0.0;

        // Calculate daily growth rate
        long days = Duration.between(startDate, endDate).toDays();
        if (days == 0) days = 1;
        double dailyGrowthRate = growth / (double) days;

        // Project 30-day growth
        long projected30Days = endSize + (long) (dailyGrowthRate * 30);

        return StorageMetricsDto.StorageGrowthDto.builder()
                .startSizeBytes(startSize)
                .endSizeBytes(endSize)
                .growthBytes(growth)
                .growthPercentage(growthPercentage)
                .dailyGrowthRateBytes(dailyGrowthRate)
                .projectedSize30Days(projected30Days)
                .filesAdded(filesAdded)
                .filesDeleted(filesDeleted)
                .build();
    }

    /**
     * Calculate health status for storage.
     */
    public BackendResourceMetricsDto.HealthStatus calculateHealthStatus(StorageMetricsDto metrics) {
        // Check upload success rate
        if (metrics.getUploadPerformance() != null) {
            double successRate = metrics.getUploadPerformance().getSuccessRate();
            Long p90Latency = metrics.getUploadPerformance().getP90UploadLatencyMs();

            if (successRate < UPLOAD_SUCCESS_CRITICAL ||
                (p90Latency != null && p90Latency > UPLOAD_LATENCY_CRITICAL_MS)) {
                return BackendResourceMetricsDto.HealthStatus.CRITICAL;
            }
            if (successRate < UPLOAD_SUCCESS_WARNING ||
                (p90Latency != null && p90Latency > UPLOAD_LATENCY_WARNING_MS)) {
                return BackendResourceMetricsDto.HealthStatus.DEGRADED;
            }
        }

        // Check storage growth rate
        if (metrics.getStorageGrowth() != null) {
            double growthPct = metrics.getStorageGrowth().getGrowthPercentage() / 100.0;
            if (growthPct > STORAGE_GROWTH_CRITICAL) {
                return BackendResourceMetricsDto.HealthStatus.DEGRADED;
            }
        }

        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }
}

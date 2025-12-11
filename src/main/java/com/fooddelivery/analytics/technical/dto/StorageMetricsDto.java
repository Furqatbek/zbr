package com.fooddelivery.analytics.technical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Storage / Image Upload metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageMetricsDto {

    /**
     * Total file count.
     */
    private Long totalFileCount;

    /**
     * Total storage size in bytes.
     */
    private Long totalStorageSizeBytes;

    /**
     * Total storage size in MB.
     */
    private Double totalStorageSizeMb;

    /**
     * Total storage size in GB.
     */
    private Double totalStorageSizeGb;

    /**
     * Image-specific metrics.
     */
    private ImageMetricsDto imageMetrics;

    /**
     * Upload performance metrics.
     */
    private UploadPerformanceDto uploadPerformance;

    /**
     * Storage breakdown by type.
     */
    private Map<String, StorageBreakdownDto> storageByType;

    /**
     * Storage breakdown by entity type.
     */
    private Map<String, StorageBreakdownDto> storageByEntityType;

    /**
     * Daily upload trend.
     */
    private List<DailyUploadDto> dailyUploadTrend;

    /**
     * Storage growth metrics.
     */
    private StorageGrowthDto storageGrowth;

    /**
     * Start of the reporting period.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime periodStart;

    /**
     * End of the reporting period.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime periodEnd;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageMetricsDto {
        /**
         * Total image count.
         */
        private Long totalImageCount;

        /**
         * Total image size in bytes.
         */
        private Long totalImageSizeBytes;

        /**
         * Average image size in bytes.
         */
        private Double avgImageSizeBytes;

        /**
         * Images by content type.
         */
        private Map<String, Long> imagesByContentType;

        /**
         * Percentage of storage used by images.
         */
        private Double imageStoragePercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UploadPerformanceDto {
        /**
         * Total uploads in period.
         */
        private Long totalUploads;

        /**
         * Successful uploads.
         */
        private Long successfulUploads;

        /**
         * Failed uploads.
         */
        private Long failedUploads;

        /**
         * Upload success rate (0.0 - 1.0).
         */
        private Double successRate;

        /**
         * Average upload latency in ms.
         */
        private Double avgUploadLatencyMs;

        /**
         * P50 upload latency in ms.
         */
        private Long p50UploadLatencyMs;

        /**
         * P90 upload latency in ms.
         */
        private Long p90UploadLatencyMs;

        /**
         * P99 upload latency in ms.
         */
        private Long p99UploadLatencyMs;

        /**
         * Max upload latency in ms.
         */
        private Long maxUploadLatencyMs;

        /**
         * Uploads per minute.
         */
        private Double uploadsPerMinute;

        /**
         * Average upload size in bytes.
         */
        private Double avgUploadSizeBytes;

        /**
         * Total bytes uploaded.
         */
        private Long totalBytesUploaded;

        /**
         * Failed uploads by error type.
         */
        private Map<String, Long> failuresByReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageBreakdownDto {
        private String type;
        private Long fileCount;
        private Long totalSizeBytes;
        private Double avgFileSizeBytes;
        private Double percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyUploadDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate date;
        private Long uploadCount;
        private Long totalSizeBytes;
        private Long failedCount;
        private Double avgLatencyMs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageGrowthDto {
        /**
         * Storage size at period start.
         */
        private Long startSizeBytes;

        /**
         * Storage size at period end.
         */
        private Long endSizeBytes;

        /**
         * Growth in bytes.
         */
        private Long growthBytes;

        /**
         * Growth percentage.
         */
        private Double growthPercentage;

        /**
         * Daily growth rate in bytes.
         */
        private Double dailyGrowthRateBytes;

        /**
         * Projected size in 30 days (bytes).
         */
        private Long projectedSize30Days;

        /**
         * Files added in period.
         */
        private Long filesAdded;

        /**
         * Files deleted in period.
         */
        private Long filesDeleted;
    }
}

package com.fooddelivery.analytics.technical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Database Performance metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatabasePerformanceMetricsDto {

    /**
     * Slow queries summary.
     */
    private SlowQueriesSummaryDto slowQueriesSummary;

    /**
     * Top slow queries.
     */
    private List<SlowQueryDto> topSlowQueries;

    /**
     * Index usage statistics.
     */
    private IndexUsageDto indexUsage;

    /**
     * Storage statistics.
     */
    private StorageStatsDto storageStats;

    /**
     * Table statistics.
     */
    private List<TableStatsDto> tableStats;

    /**
     * Backup summary.
     */
    private BackupSummaryDto backupSummary;

    /**
     * Query performance breakdown.
     */
    private QueryPerformanceDto queryPerformance;

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
    public static class SlowQueriesSummaryDto {
        /**
         * Total slow queries count.
         */
        private Long totalSlowQueries;

        /**
         * Average duration of slow queries in ms.
         */
        private Double avgDurationMs;

        /**
         * Max duration in ms.
         */
        private Long maxDurationMs;

        /**
         * Threshold used for slow query detection (ms).
         */
        private Long slowQueryThresholdMs;

        /**
         * Queries by type.
         */
        private QueryTypeBreakdownDto queryTypeBreakdown;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueryTypeBreakdownDto {
        private Long selectCount;
        private Long insertCount;
        private Long updateCount;
        private Long deleteCount;
        private Long otherCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SlowQueryDto {
        /**
         * Query hash for deduplication.
         */
        private String queryHash;

        /**
         * Normalized query text (with parameters replaced).
         */
        private String queryText;

        /**
         * Duration in ms.
         */
        private Long durationMs;

        /**
         * Rows affected.
         */
        private Long rowsAffected;

        /**
         * Number of occurrences.
         */
        private Long occurrences;

        /**
         * Average duration across occurrences.
         */
        private Double avgDurationMs;

        /**
         * Table name.
         */
        private String tableName;

        /**
         * Is using index.
         */
        private Boolean isUsingIndex;

        /**
         * Timestamp of last occurrence.
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastOccurrence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndexUsageDto {
        /**
         * Index hit rate (0.0 - 1.0).
         */
        private Double indexHitRate;

        /**
         * Total index scans.
         */
        private Long totalIndexScans;

        /**
         * Total sequential scans.
         */
        private Long totalSeqScans;

        /**
         * Unused indexes count.
         */
        private Integer unusedIndexesCount;

        /**
         * Missing indexes suggestions.
         */
        private Integer suggestedIndexesCount;

        /**
         * Index size in bytes.
         */
        private Long totalIndexSizeBytes;

        /**
         * List of unused indexes.
         */
        private List<String> unusedIndexes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageStatsDto {
        /**
         * Total database size in bytes.
         */
        private Long totalDatabaseSizeBytes;

        /**
         * Data size in bytes.
         */
        private Long dataSizeBytes;

        /**
         * Index size in bytes.
         */
        private Long indexSizeBytes;

        /**
         * Storage growth rate (bytes per day).
         */
        private Long dailyGrowthBytes;

        /**
         * Estimated days until storage threshold.
         */
        private Integer estimatedDaysToThreshold;

        /**
         * Dead tuple percentage.
         */
        private Double deadTuplePercentage;

        /**
         * Last vacuum timestamp.
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastVacuumAt;

        /**
         * Last analyze timestamp.
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastAnalyzeAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TableStatsDto {
        private String tableName;
        private Long tableSizeBytes;
        private Long indexSizeBytes;
        private Long rowCount;
        private Long deadTuples;
        private Double deadTuplePercentage;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastVacuum;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastAnalyze;
        private Long seqScans;
        private Long indexScans;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BackupSummaryDto {
        /**
         * Total backups in period.
         */
        private Long totalBackups;

        /**
         * Successful backups.
         */
        private Long successfulBackups;

        /**
         * Failed backups.
         */
        private Long failedBackups;

        /**
         * Success rate (0.0 - 1.0).
         */
        private Double successRate;

        /**
         * Average backup duration in seconds.
         */
        private Double avgDurationSeconds;

        /**
         * Average backup size in bytes.
         */
        private Double avgBackupSizeBytes;

        /**
         * Last successful backup timestamp.
         */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastSuccessfulBackup;

        /**
         * Last backup status.
         */
        private String lastBackupStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueryPerformanceDto {
        /**
         * Total queries executed.
         */
        private Long totalQueries;

        /**
         * Queries per second.
         */
        private Double queriesPerSecond;

        /**
         * Average query time in ms.
         */
        private Double avgQueryTimeMs;

        /**
         * Cache hit rate.
         */
        private Double cacheHitRate;

        /**
         * Transactions per second.
         */
        private Double transactionsPerSecond;

        /**
         * Rollback rate.
         */
        private Double rollbackRate;
    }
}

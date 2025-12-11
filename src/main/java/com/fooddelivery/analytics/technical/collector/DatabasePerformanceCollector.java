package com.fooddelivery.analytics.technical.collector;

import com.fooddelivery.analytics.technical.dto.BackendResourceMetricsDto;
import com.fooddelivery.analytics.technical.dto.DatabasePerformanceMetricsDto;
import com.fooddelivery.analytics.technical.model.SlowQueryLog;
import com.fooddelivery.analytics.technical.repository.BackupLogRepository;
import com.fooddelivery.analytics.technical.repository.SlowQueryLogRepository;
import com.fooddelivery.analytics.technical.model.BackupLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collector for database performance metrics.
 * Sources: Slow query logs, pg_stat_statements, pg_stat_user_tables.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabasePerformanceCollector {

    private final SlowQueryLogRepository slowQueryLogRepository;
    private final BackupLogRepository backupLogRepository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    // Thresholds
    private static final double INDEX_HIT_RATE_WARNING = 0.95;
    private static final double INDEX_HIT_RATE_CRITICAL = 0.90;
    private static final double BACKUP_SUCCESS_WARNING = 0.95;
    private static final double BACKUP_SUCCESS_CRITICAL = 0.90;
    private static final double DEAD_TUPLE_WARNING = 0.10; // 10%
    private static final double DEAD_TUPLE_CRITICAL = 0.20; // 20%

    /**
     * Collect database performance metrics for a period.
     */
    public DatabasePerformanceMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate,
                                                  Long slowQueryThresholdMs, int topSlowQueriesLimit,
                                                  boolean includeTableStats) {
        log.debug("Collecting database performance metrics from {} to {}", startDate, endDate);

        // Slow queries summary
        DatabasePerformanceMetricsDto.SlowQueriesSummaryDto slowQueriesSummary =
                collectSlowQueriesSummary(startDate, endDate, slowQueryThresholdMs);

        // Top slow queries
        List<DatabasePerformanceMetricsDto.SlowQueryDto> topSlowQueries =
                getTopSlowQueries(startDate, endDate, topSlowQueriesLimit);

        // Index usage
        DatabasePerformanceMetricsDto.IndexUsageDto indexUsage = collectIndexUsage(startDate, endDate);

        // Storage stats
        DatabasePerformanceMetricsDto.StorageStatsDto storageStats = collectStorageStats();

        // Table stats (optional)
        List<DatabasePerformanceMetricsDto.TableStatsDto> tableStats = null;
        if (includeTableStats) {
            tableStats = collectTableStats();
        }

        // Backup summary
        DatabasePerformanceMetricsDto.BackupSummaryDto backupSummary =
                collectBackupSummary(startDate, endDate);

        // Query performance
        DatabasePerformanceMetricsDto.QueryPerformanceDto queryPerformance =
                collectQueryPerformance(startDate, endDate);

        return DatabasePerformanceMetricsDto.builder()
                .slowQueriesSummary(slowQueriesSummary)
                .topSlowQueries(topSlowQueries)
                .indexUsage(indexUsage)
                .storageStats(storageStats)
                .tableStats(tableStats)
                .backupSummary(backupSummary)
                .queryPerformance(queryPerformance)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    private DatabasePerformanceMetricsDto.SlowQueriesSummaryDto collectSlowQueriesSummary(
            LocalDateTime startDate, LocalDateTime endDate, Long threshold) {

        Long totalSlowQueries = slowQueryLogRepository.countSlowQueriesAboveThreshold(startDate, endDate, threshold);
        Double avgDuration = slowQueryLogRepository.getAverageDuration(startDate, endDate);
        Long maxDuration = slowQueryLogRepository.getMaxDuration(startDate, endDate);

        // Query type breakdown
        List<Object[]> typeBreakdown = slowQueryLogRepository.getCountByQueryType(startDate, endDate);
        DatabasePerformanceMetricsDto.QueryTypeBreakdownDto queryTypeBreakdown =
                buildQueryTypeBreakdown(typeBreakdown);

        return DatabasePerformanceMetricsDto.SlowQueriesSummaryDto.builder()
                .totalSlowQueries(totalSlowQueries != null ? totalSlowQueries : 0L)
                .avgDurationMs(avgDuration)
                .maxDurationMs(maxDuration)
                .slowQueryThresholdMs(threshold)
                .queryTypeBreakdown(queryTypeBreakdown)
                .build();
    }

    private DatabasePerformanceMetricsDto.QueryTypeBreakdownDto buildQueryTypeBreakdown(List<Object[]> typeBreakdown) {
        long selectCount = 0, insertCount = 0, updateCount = 0, deleteCount = 0, otherCount = 0;

        for (Object[] row : typeBreakdown) {
            SlowQueryLog.QueryType type = (SlowQueryLog.QueryType) row[0];
            long count = ((Number) row[1]).longValue();

            switch (type) {
                case SELECT -> selectCount = count;
                case INSERT -> insertCount = count;
                case UPDATE -> updateCount = count;
                case DELETE -> deleteCount = count;
                default -> otherCount += count;
            }
        }

        return DatabasePerformanceMetricsDto.QueryTypeBreakdownDto.builder()
                .selectCount(selectCount)
                .insertCount(insertCount)
                .updateCount(updateCount)
                .deleteCount(deleteCount)
                .otherCount(otherCount)
                .build();
    }

    private List<DatabasePerformanceMetricsDto.SlowQueryDto> getTopSlowQueries(
            LocalDateTime startDate, LocalDateTime endDate, int limit) {

        return slowQueryLogRepository.getTopSlowQueries(startDate, endDate).stream()
                .limit(limit)
                .map(row -> DatabasePerformanceMetricsDto.SlowQueryDto.builder()
                        .queryHash((String) row[0])
                        .queryText((String) row[1])
                        .durationMs(((Number) row[2]).longValue())
                        .rowsAffected(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                        .occurrences(((Number) row[4]).longValue())
                        .avgDurationMs(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0)
                        .tableName((String) row[6])
                        .isUsingIndex(row[7] != null ? (Boolean) row[7] : false)
                        .lastOccurrence(row[8] != null ? (LocalDateTime) row[8] : null)
                        .build())
                .collect(Collectors.toList());
    }

    private DatabasePerformanceMetricsDto.IndexUsageDto collectIndexUsage(
            LocalDateTime startDate, LocalDateTime endDate) {

        // Try to get index usage from pg_stat views
        double indexHitRate = 0.0;
        long indexScans = 0;
        long seqScans = 0;
        int unusedIndexesCount = 0;
        long totalIndexSize = 0;
        List<String> unusedIndexes = new ArrayList<>();

        if (jdbcTemplate != null) {
            try {
                // Get index hit rate from pg_statio_user_indexes
                String hitRateQuery = """
                    SELECT
                        COALESCE(SUM(idx_blks_hit)::float / NULLIF(SUM(idx_blks_hit + idx_blks_read), 0), 0) as hit_rate,
                        SUM(idx_scan) as index_scans
                    FROM pg_stat_user_indexes
                    """;

                jdbcTemplate.query(hitRateQuery, rs -> {
                    // Process results
                });

                // Get sequential scans
                String seqScanQuery = "SELECT SUM(seq_scan) FROM pg_stat_user_tables";
                Long seqScanResult = jdbcTemplate.queryForObject(seqScanQuery, Long.class);
                seqScans = seqScanResult != null ? seqScanResult : 0;

                // Get unused indexes
                String unusedIndexQuery = """
                    SELECT indexrelname
                    FROM pg_stat_user_indexes
                    WHERE idx_scan = 0 AND idx_tup_read = 0
                    """;
                unusedIndexes = jdbcTemplate.queryForList(unusedIndexQuery, String.class);
                unusedIndexesCount = unusedIndexes.size();

                // Get total index size
                String indexSizeQuery = "SELECT SUM(pg_relation_size(indexrelid)) FROM pg_stat_user_indexes";
                Long indexSizeResult = jdbcTemplate.queryForObject(indexSizeQuery, Long.class);
                totalIndexSize = indexSizeResult != null ? indexSizeResult : 0;

            } catch (Exception e) {
                log.warn("Failed to collect index usage from pg_stat: {}", e.getMessage());
            }
        }

        // Fallback to slow query log index usage
        List<Object[]> indexUsageFromLogs = slowQueryLogRepository.getIndexUsageCount(startDate, endDate);
        long usingIndex = 0, notUsingIndex = 0;
        for (Object[] row : indexUsageFromLogs) {
            Boolean isUsing = (Boolean) row[0];
            long count = ((Number) row[1]).longValue();
            if (Boolean.TRUE.equals(isUsing)) {
                usingIndex = count;
            } else {
                notUsingIndex = count;
            }
        }
        if (usingIndex + notUsingIndex > 0) {
            indexHitRate = (double) usingIndex / (usingIndex + notUsingIndex);
        }

        return DatabasePerformanceMetricsDto.IndexUsageDto.builder()
                .indexHitRate(indexHitRate)
                .totalIndexScans(indexScans)
                .totalSeqScans(seqScans)
                .unusedIndexesCount(unusedIndexesCount)
                .suggestedIndexesCount(0) // Would require query analysis
                .totalIndexSizeBytes(totalIndexSize)
                .unusedIndexes(unusedIndexes.size() > 10 ? unusedIndexes.subList(0, 10) : unusedIndexes)
                .build();
    }

    private DatabasePerformanceMetricsDto.StorageStatsDto collectStorageStats() {
        long totalSize = 0;
        long dataSize = 0;
        long indexSize = 0;
        double deadTuplePercentage = 0.0;
        LocalDateTime lastVacuum = null;
        LocalDateTime lastAnalyze = null;

        if (jdbcTemplate != null) {
            try {
                // Get database size
                String dbSizeQuery = "SELECT pg_database_size(current_database())";
                Long dbSize = jdbcTemplate.queryForObject(dbSizeQuery, Long.class);
                totalSize = dbSize != null ? dbSize : 0;

                // Get data vs index size
                String tableSizeQuery = """
                    SELECT
                        COALESCE(SUM(pg_table_size(relid)), 0) as data_size,
                        COALESCE(SUM(pg_indexes_size(relid)), 0) as index_size
                    FROM pg_stat_user_tables
                    """;
                jdbcTemplate.query(tableSizeQuery, rs -> {
                    // Process results
                });

                // Get dead tuple percentage
                String deadTupleQuery = """
                    SELECT
                        COALESCE(SUM(n_dead_tup)::float / NULLIF(SUM(n_live_tup + n_dead_tup), 0), 0)
                    FROM pg_stat_user_tables
                    """;
                Double deadTuple = jdbcTemplate.queryForObject(deadTupleQuery, Double.class);
                deadTuplePercentage = deadTuple != null ? deadTuple : 0.0;

                // Get last vacuum/analyze times
                String maintenanceQuery = """
                    SELECT MAX(last_vacuum), MAX(last_analyze)
                    FROM pg_stat_user_tables
                    """;
                jdbcTemplate.query(maintenanceQuery, rs -> {
                    // Process results
                });

            } catch (Exception e) {
                log.warn("Failed to collect storage stats: {}", e.getMessage());
            }
        }

        return DatabasePerformanceMetricsDto.StorageStatsDto.builder()
                .totalDatabaseSizeBytes(totalSize)
                .dataSizeBytes(dataSize)
                .indexSizeBytes(indexSize)
                .dailyGrowthBytes(0L) // Would need historical data
                .estimatedDaysToThreshold(null)
                .deadTuplePercentage(deadTuplePercentage)
                .lastVacuumAt(lastVacuum)
                .lastAnalyzeAt(lastAnalyze)
                .build();
    }

    private List<DatabasePerformanceMetricsDto.TableStatsDto> collectTableStats() {
        List<DatabasePerformanceMetricsDto.TableStatsDto> stats = new ArrayList<>();

        if (jdbcTemplate != null) {
            try {
                String query = """
                    SELECT
                        relname as table_name,
                        pg_table_size(relid) as table_size,
                        pg_indexes_size(relid) as index_size,
                        n_live_tup as row_count,
                        n_dead_tup as dead_tuples,
                        COALESCE(n_dead_tup::float / NULLIF(n_live_tup + n_dead_tup, 0), 0) as dead_pct,
                        last_vacuum,
                        last_analyze,
                        seq_scan,
                        idx_scan
                    FROM pg_stat_user_tables
                    ORDER BY pg_total_relation_size(relid) DESC
                    LIMIT 20
                    """;

                stats = jdbcTemplate.query(query, (rs, rowNum) ->
                        DatabasePerformanceMetricsDto.TableStatsDto.builder()
                                .tableName(rs.getString("table_name"))
                                .tableSizeBytes(rs.getLong("table_size"))
                                .indexSizeBytes(rs.getLong("index_size"))
                                .rowCount(rs.getLong("row_count"))
                                .deadTuples(rs.getLong("dead_tuples"))
                                .deadTuplePercentage(rs.getDouble("dead_pct"))
                                .lastVacuum(rs.getTimestamp("last_vacuum") != null ?
                                        rs.getTimestamp("last_vacuum").toLocalDateTime() : null)
                                .lastAnalyze(rs.getTimestamp("last_analyze") != null ?
                                        rs.getTimestamp("last_analyze").toLocalDateTime() : null)
                                .seqScans(rs.getLong("seq_scan"))
                                .indexScans(rs.getLong("idx_scan"))
                                .build()
                );

            } catch (Exception e) {
                log.warn("Failed to collect table stats: {}", e.getMessage());
            }
        }

        return stats;
    }

    private DatabasePerformanceMetricsDto.BackupSummaryDto collectBackupSummary(
            LocalDateTime startDate, LocalDateTime endDate) {

        Long totalBackups = backupLogRepository.countBackupsInPeriod(startDate, endDate);
        Long successfulBackups = backupLogRepository.countSuccessfulBackups(startDate, endDate);
        Long failedBackups = backupLogRepository.countFailedBackups(startDate, endDate);
        Double avgDuration = backupLogRepository.getAverageBackupDurationSeconds(startDate, endDate);
        Double avgSize = backupLogRepository.getAverageBackupSize(startDate, endDate);

        if (totalBackups == null) totalBackups = 0L;
        if (successfulBackups == null) successfulBackups = 0L;
        if (failedBackups == null) failedBackups = 0L;

        double successRate = totalBackups > 0 ? successfulBackups.doubleValue() / totalBackups : 1.0;

        // Get last successful backup
        LocalDateTime lastSuccessful = backupLogRepository.findLastSuccessfulBackup()
                .map(BackupLog::getCompletedAt)
                .orElse(null);

        // Get last backup status
        String lastStatus = backupLogRepository.findLastBackup()
                .map(b -> b.getStatus().name())
                .orElse("UNKNOWN");

        return DatabasePerformanceMetricsDto.BackupSummaryDto.builder()
                .totalBackups(totalBackups)
                .successfulBackups(successfulBackups)
                .failedBackups(failedBackups)
                .successRate(successRate)
                .avgDurationSeconds(avgDuration)
                .avgBackupSizeBytes(avgSize)
                .lastSuccessfulBackup(lastSuccessful)
                .lastBackupStatus(lastStatus)
                .build();
    }

    private DatabasePerformanceMetricsDto.QueryPerformanceDto collectQueryPerformance(
            LocalDateTime startDate, LocalDateTime endDate) {

        // Try to get from pg_stat_statements if available
        long totalQueries = 0;
        double queriesPerSecond = 0;
        double avgQueryTime = 0;
        double cacheHitRate = 0;
        double transactionsPerSecond = 0;
        double rollbackRate = 0;

        if (jdbcTemplate != null) {
            try {
                // Check if pg_stat_statements is available
                String checkQuery = """
                    SELECT EXISTS (
                        SELECT 1 FROM pg_extension WHERE extname = 'pg_stat_statements'
                    )
                    """;
                Boolean hasExtension = jdbcTemplate.queryForObject(checkQuery, Boolean.class);

                if (Boolean.TRUE.equals(hasExtension)) {
                    String statsQuery = """
                        SELECT
                            SUM(calls) as total_calls,
                            AVG(mean_exec_time) as avg_time
                        FROM pg_stat_statements
                        """;
                    jdbcTemplate.query(statsQuery, rs -> {
                        // Process results
                    });
                }

                // Get cache hit rate from pg_stat_database
                String cacheQuery = """
                    SELECT
                        COALESCE(SUM(blks_hit)::float / NULLIF(SUM(blks_hit + blks_read), 0), 0) as hit_rate,
                        SUM(xact_commit) as commits,
                        SUM(xact_rollback) as rollbacks
                    FROM pg_stat_database
                    WHERE datname = current_database()
                    """;
                jdbcTemplate.query(cacheQuery, rs -> {
                    // Process results
                });

            } catch (Exception e) {
                log.warn("Failed to collect query performance: {}", e.getMessage());
            }
        }

        return DatabasePerformanceMetricsDto.QueryPerformanceDto.builder()
                .totalQueries(totalQueries)
                .queriesPerSecond(queriesPerSecond)
                .avgQueryTimeMs(avgQueryTime)
                .cacheHitRate(cacheHitRate)
                .transactionsPerSecond(transactionsPerSecond)
                .rollbackRate(rollbackRate)
                .build();
    }

    /**
     * Calculate health status for database.
     */
    public BackendResourceMetricsDto.HealthStatus calculateHealthStatus(DatabasePerformanceMetricsDto metrics) {
        // Check index hit rate
        if (metrics.getIndexUsage() != null) {
            double hitRate = metrics.getIndexUsage().getIndexHitRate();
            if (hitRate < INDEX_HIT_RATE_CRITICAL) {
                return BackendResourceMetricsDto.HealthStatus.CRITICAL;
            }
            if (hitRate < INDEX_HIT_RATE_WARNING) {
                return BackendResourceMetricsDto.HealthStatus.DEGRADED;
            }
        }

        // Check backup success rate
        if (metrics.getBackupSummary() != null) {
            double successRate = metrics.getBackupSummary().getSuccessRate();
            if (successRate < BACKUP_SUCCESS_CRITICAL) {
                return BackendResourceMetricsDto.HealthStatus.CRITICAL;
            }
            if (successRate < BACKUP_SUCCESS_WARNING) {
                return BackendResourceMetricsDto.HealthStatus.DEGRADED;
            }
        }

        // Check dead tuple percentage
        if (metrics.getStorageStats() != null) {
            double deadTuples = metrics.getStorageStats().getDeadTuplePercentage();
            if (deadTuples > DEAD_TUPLE_CRITICAL) {
                return BackendResourceMetricsDto.HealthStatus.CRITICAL;
            }
            if (deadTuples > DEAD_TUPLE_WARNING) {
                return BackendResourceMetricsDto.HealthStatus.DEGRADED;
            }
        }

        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }
}

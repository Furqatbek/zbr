package com.fooddelivery.analytics.technical.collector;

import com.fooddelivery.analytics.technical.dto.BackendResourceMetricsDto;
import com.fooddelivery.analytics.technical.model.SystemMetricSnapshot;
import com.fooddelivery.analytics.technical.repository.MessageQueueStatsRepository;
import com.fooddelivery.analytics.technical.repository.SystemMetricSnapshotRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Collector for backend resource metrics.
 * Sources: JVM, Actuator, HikariCP, Redis, Message Queues.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackendResourceCollector {

    private final MeterRegistry meterRegistry;
    private final SystemMetricSnapshotRepository systemMetricSnapshotRepository;
    private final MessageQueueStatsRepository messageQueueStatsRepository;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // Thresholds
    private static final double CPU_WARNING = 0.70;
    private static final double CPU_CRITICAL = 0.90;
    private static final double MEMORY_WARNING = 0.75;
    private static final double MEMORY_CRITICAL = 0.90;
    private static final double HEAP_WARNING = 0.80;
    private static final double HEAP_CRITICAL = 0.95;
    private static final double DB_POOL_WARNING = 0.80;
    private static final double DB_POOL_CRITICAL = 0.95;
    private static final long REDIS_LATENCY_WARNING_MS = 10;
    private static final long REDIS_LATENCY_CRITICAL_MS = 50;
    private static final long QUEUE_BACKLOG_WARNING = 1000;
    private static final long QUEUE_BACKLOG_CRITICAL = 10000;

    /**
     * Collect current backend resource metrics.
     */
    public BackendResourceMetricsDto collect() {
        log.debug("Collecting backend resource metrics");

        BackendResourceMetricsDto.CpuMetricsDto cpuMetrics = collectCpuMetrics();
        BackendResourceMetricsDto.MemoryMetricsDto memoryMetrics = collectMemoryMetrics();
        BackendResourceMetricsDto.JvmMetricsDto jvmMetrics = collectJvmMetrics();
        BackendResourceMetricsDto.DatabasePoolMetricsDto dbPoolMetrics = collectDatabasePoolMetrics();
        BackendResourceMetricsDto.RedisMetricsDto redisMetrics = collectRedisMetrics();
        BackendResourceMetricsDto.MessageQueueMetricsDto mqMetrics = collectMessageQueueMetrics();

        BackendResourceMetricsDto.HealthStatus overallStatus = calculateOverallHealth(
                cpuMetrics, memoryMetrics, jvmMetrics, dbPoolMetrics, redisMetrics, mqMetrics);

        return BackendResourceMetricsDto.builder()
                .cpu(cpuMetrics)
                .memory(memoryMetrics)
                .jvm(jvmMetrics)
                .databasePool(dbPoolMetrics)
                .redis(redisMetrics)
                .messageQueue(mqMetrics)
                .overallStatus(overallStatus)
                .collectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Collect historical metrics for a period.
     */
    public BackendResourceMetricsDto collectHistorical(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Collecting historical backend metrics from {} to {}", startDate, endDate);

        // Get historical CPU metrics
        List<SystemMetricSnapshot> cpuSnapshots = systemMetricSnapshotRepository.getCpuMetrics(startDate, endDate);
        Double avgCpu = systemMetricSnapshotRepository.getAverageValue("system.cpu.usage", startDate, endDate);
        Double maxCpu = systemMetricSnapshotRepository.getMaxValue("system.cpu.usage", startDate, endDate);

        // Get historical memory metrics
        List<SystemMetricSnapshot> memorySnapshots = systemMetricSnapshotRepository.getMemoryMetrics(startDate, endDate);
        Double avgMemory = systemMetricSnapshotRepository.getAverageValue("jvm.memory.used", startDate, endDate);
        Double maxMemory = systemMetricSnapshotRepository.getMaxValue("jvm.memory.used", startDate, endDate);

        // Build response with historical averages
        BackendResourceMetricsDto.CpuMetricsDto cpuMetrics = BackendResourceMetricsDto.CpuMetricsDto.builder()
                .systemCpuUsage(avgCpu != null ? avgCpu : 0.0)
                .status(calculateCpuStatus(avgCpu != null ? avgCpu : 0.0))
                .build();

        BackendResourceMetricsDto.MemoryMetricsDto memoryMetrics = BackendResourceMetricsDto.MemoryMetricsDto.builder()
                .usedMemoryBytes(avgMemory != null ? avgMemory.longValue() : 0L)
                .status(calculateMemoryStatus(avgMemory != null ? avgMemory / getMaxMemory() : 0.0))
                .build();

        return BackendResourceMetricsDto.builder()
                .cpu(cpuMetrics)
                .memory(memoryMetrics)
                .overallStatus(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                .collectedAt(LocalDateTime.now())
                .build();
    }

    private BackendResourceMetricsDto.CpuMetricsDto collectCpuMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double systemLoad = osBean.getSystemLoadAverage();
        int processors = osBean.getAvailableProcessors();

        double systemCpuUsage = 0.0;
        double processCpuUsage = 0.0;

        // Try to get more detailed CPU info if available
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            systemCpuUsage = sunOsBean.getCpuLoad();
            processCpuUsage = sunOsBean.getProcessCpuLoad();
        }

        // Also check Micrometer
        Gauge cpuGauge = meterRegistry.find("system.cpu.usage").gauge();
        if (cpuGauge != null) {
            systemCpuUsage = cpuGauge.value();
        }

        Gauge processCpuGauge = meterRegistry.find("process.cpu.usage").gauge();
        if (processCpuGauge != null) {
            processCpuUsage = processCpuGauge.value();
        }

        return BackendResourceMetricsDto.CpuMetricsDto.builder()
                .systemCpuUsage(systemCpuUsage)
                .processCpuUsage(processCpuUsage)
                .systemLoadAverage(systemLoad)
                .availableProcessors(processors)
                .loadPerProcessor(processors > 0 ? systemLoad / processors : 0.0)
                .status(calculateCpuStatus(systemCpuUsage))
                .build();
    }

    private BackendResourceMetricsDto.MemoryMetricsDto collectMemoryMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double usagePercentage = maxMemory > 0 ? (double) usedMemory / maxMemory : 0.0;

        return BackendResourceMetricsDto.MemoryMetricsDto.builder()
                .totalMemoryBytes(maxMemory)
                .usedMemoryBytes(usedMemory)
                .freeMemoryBytes(maxMemory - usedMemory)
                .usagePercentage(usagePercentage)
                .status(calculateMemoryStatus(usagePercentage))
                .build();
    }

    private BackendResourceMetricsDto.JvmMetricsDto collectJvmMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();

        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
        long nonHeapCommitted = memoryBean.getNonHeapMemoryUsage().getCommitted();

        double heapUsage = heapMax > 0 ? (double) heapUsed / heapMax : 0.0;

        // Get thread info
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        int peakThreadCount = ManagementFactory.getThreadMXBean().getPeakThreadCount();

        // Get GC info from Micrometer
        long gcCount = 0;
        double gcTimeMs = 0;
        try {
            var gcCounter = meterRegistry.find("jvm.gc.pause").timer();
            if (gcCounter != null) {
                gcCount = gcCounter.count();
                gcTimeMs = gcCounter.totalTime(TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.trace("Could not get GC metrics: {}", e.getMessage());
        }

        return BackendResourceMetricsDto.JvmMetricsDto.builder()
                .heapUsedBytes(heapUsed)
                .heapMaxBytes(heapMax)
                .heapCommittedBytes(heapCommitted)
                .heapUsagePercentage(heapUsage)
                .nonHeapUsedBytes(nonHeapUsed)
                .nonHeapMaxBytes(nonHeapMax)
                .nonHeapCommittedBytes(nonHeapCommitted)
                .threadCount(threadCount)
                .peakThreadCount(peakThreadCount)
                .gcCount(gcCount)
                .gcTimeMs(gcTimeMs)
                .status(calculateHeapStatus(heapUsage))
                .build();
    }

    private BackendResourceMetricsDto.DatabasePoolMetricsDto collectDatabasePoolMetrics() {
        if (dataSource == null) {
            return BackendResourceMetricsDto.DatabasePoolMetricsDto.builder()
                    .status(BackendResourceMetricsDto.HealthStatus.UNKNOWN)
                    .build();
        }

        try {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();
                if (poolBean != null) {
                    int active = poolBean.getActiveConnections();
                    int idle = poolBean.getIdleConnections();
                    int total = poolBean.getTotalConnections();
                    int pending = poolBean.getThreadsAwaitingConnection();
                    int maxPoolSize = hikariDataSource.getMaximumPoolSize();

                    double usagePercentage = maxPoolSize > 0 ? (double) active / maxPoolSize : 0.0;

                    return BackendResourceMetricsDto.DatabasePoolMetricsDto.builder()
                            .activeConnections(active)
                            .idleConnections(idle)
                            .totalConnections(total)
                            .maxPoolSize(maxPoolSize)
                            .pendingConnections(pending)
                            .usagePercentage(usagePercentage)
                            .status(calculateDbPoolStatus(usagePercentage))
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to collect database pool metrics: {}", e.getMessage());
        }

        return BackendResourceMetricsDto.DatabasePoolMetricsDto.builder()
                .status(BackendResourceMetricsDto.HealthStatus.UNKNOWN)
                .build();
    }

    private BackendResourceMetricsDto.RedisMetricsDto collectRedisMetrics() {
        if (redisTemplate == null) {
            return BackendResourceMetricsDto.RedisMetricsDto.builder()
                    .isConnected(false)
                    .status(BackendResourceMetricsDto.HealthStatus.UNKNOWN)
                    .build();
        }

        try {
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return BackendResourceMetricsDto.RedisMetricsDto.builder()
                        .isConnected(false)
                        .status(BackendResourceMetricsDto.HealthStatus.CRITICAL)
                        .build();
            }

            // Measure ping latency
            long startTime = System.currentTimeMillis();
            String pingResult = connectionFactory.getConnection().ping();
            long latency = System.currentTimeMillis() - startTime;

            boolean isConnected = "PONG".equals(pingResult);

            // Try to get Redis info
            Long usedMemory = null;
            Long connectedClients = null;
            Double hitRate = null;
            Long keyCount = null;

            try {
                var connection = connectionFactory.getConnection();
                var info = connection.serverCommands().info("memory");
                if (info != null) {
                    var props = info.getProperties();
                    if (props.containsKey("used_memory")) {
                        usedMemory = Long.parseLong(props.getProperty("used_memory"));
                    }
                }

                var clientInfo = connection.serverCommands().info("clients");
                if (clientInfo != null) {
                    var props = clientInfo.getProperties();
                    if (props.containsKey("connected_clients")) {
                        connectedClients = Long.parseLong(props.getProperty("connected_clients"));
                    }
                }

                var statsInfo = connection.serverCommands().info("stats");
                if (statsInfo != null) {
                    var props = statsInfo.getProperties();
                    long hits = props.containsKey("keyspace_hits") ?
                            Long.parseLong(props.getProperty("keyspace_hits")) : 0;
                    long misses = props.containsKey("keyspace_misses") ?
                            Long.parseLong(props.getProperty("keyspace_misses")) : 0;
                    if (hits + misses > 0) {
                        hitRate = (double) hits / (hits + misses);
                    }
                }
            } catch (Exception e) {
                log.trace("Could not get detailed Redis info: {}", e.getMessage());
            }

            return BackendResourceMetricsDto.RedisMetricsDto.builder()
                    .isConnected(isConnected)
                    .pingLatencyMs(latency)
                    .usedMemoryBytes(usedMemory)
                    .connectedClients(connectedClients)
                    .hitRate(hitRate)
                    .keyCount(keyCount)
                    .status(calculateRedisStatus(isConnected, latency))
                    .build();

        } catch (Exception e) {
            log.warn("Failed to collect Redis metrics: {}", e.getMessage());
            return BackendResourceMetricsDto.RedisMetricsDto.builder()
                    .isConnected(false)
                    .status(BackendResourceMetricsDto.HealthStatus.CRITICAL)
                    .build();
        }
    }

    private BackendResourceMetricsDto.MessageQueueMetricsDto collectMessageQueueMetrics() {
        try {
            Long totalQueueDepth = messageQueueStatsRepository.getTotalQueueDepth();
            Long totalConsumerLag = messageQueueStatsRepository.getTotalConsumerLag();
            Long deadLetterMessages = messageQueueStatsRepository.getTotalDeadLetterMessages();

            if (totalQueueDepth == null) totalQueueDepth = 0L;
            if (totalConsumerLag == null) totalConsumerLag = 0L;
            if (deadLetterMessages == null) deadLetterMessages = 0L;

            // Get queue details
            Map<String, Long> queueDepths = new HashMap<>();
            messageQueueStatsRepository.findLatestForAllQueues()
                    .forEach(stats -> queueDepths.put(stats.getQueueName(), stats.getQueueDepth()));

            return BackendResourceMetricsDto.MessageQueueMetricsDto.builder()
                    .totalQueueDepth(totalQueueDepth)
                    .totalConsumerLag(totalConsumerLag)
                    .deadLetterQueueCount(deadLetterMessages)
                    .queueDepths(queueDepths)
                    .status(calculateQueueStatus(totalQueueDepth))
                    .build();

        } catch (Exception e) {
            log.warn("Failed to collect message queue metrics: {}", e.getMessage());
            return BackendResourceMetricsDto.MessageQueueMetricsDto.builder()
                    .totalQueueDepth(0L)
                    .status(BackendResourceMetricsDto.HealthStatus.UNKNOWN)
                    .build();
        }
    }

    private BackendResourceMetricsDto.HealthStatus calculateCpuStatus(double usage) {
        if (usage >= CPU_CRITICAL) return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        if (usage >= CPU_WARNING) return BackendResourceMetricsDto.HealthStatus.DEGRADED;
        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }

    private BackendResourceMetricsDto.HealthStatus calculateMemoryStatus(double usage) {
        if (usage >= MEMORY_CRITICAL) return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        if (usage >= MEMORY_WARNING) return BackendResourceMetricsDto.HealthStatus.DEGRADED;
        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }

    private BackendResourceMetricsDto.HealthStatus calculateHeapStatus(double usage) {
        if (usage >= HEAP_CRITICAL) return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        if (usage >= HEAP_WARNING) return BackendResourceMetricsDto.HealthStatus.DEGRADED;
        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }

    private BackendResourceMetricsDto.HealthStatus calculateDbPoolStatus(double usage) {
        if (usage >= DB_POOL_CRITICAL) return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        if (usage >= DB_POOL_WARNING) return BackendResourceMetricsDto.HealthStatus.DEGRADED;
        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }

    private BackendResourceMetricsDto.HealthStatus calculateRedisStatus(boolean isConnected, long latency) {
        if (!isConnected) return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        if (latency >= REDIS_LATENCY_CRITICAL_MS) return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        if (latency >= REDIS_LATENCY_WARNING_MS) return BackendResourceMetricsDto.HealthStatus.DEGRADED;
        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }

    private BackendResourceMetricsDto.HealthStatus calculateQueueStatus(long backlog) {
        if (backlog >= QUEUE_BACKLOG_CRITICAL) return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        if (backlog >= QUEUE_BACKLOG_WARNING) return BackendResourceMetricsDto.HealthStatus.DEGRADED;
        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }

    private BackendResourceMetricsDto.HealthStatus calculateOverallHealth(
            BackendResourceMetricsDto.CpuMetricsDto cpu,
            BackendResourceMetricsDto.MemoryMetricsDto memory,
            BackendResourceMetricsDto.JvmMetricsDto jvm,
            BackendResourceMetricsDto.DatabasePoolMetricsDto dbPool,
            BackendResourceMetricsDto.RedisMetricsDto redis,
            BackendResourceMetricsDto.MessageQueueMetricsDto mq) {

        // If any critical, overall is critical
        if (cpu.getStatus() == BackendResourceMetricsDto.HealthStatus.CRITICAL ||
            memory.getStatus() == BackendResourceMetricsDto.HealthStatus.CRITICAL ||
            jvm.getStatus() == BackendResourceMetricsDto.HealthStatus.CRITICAL ||
            dbPool.getStatus() == BackendResourceMetricsDto.HealthStatus.CRITICAL ||
            redis.getStatus() == BackendResourceMetricsDto.HealthStatus.CRITICAL ||
            mq.getStatus() == BackendResourceMetricsDto.HealthStatus.CRITICAL) {
            return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        }

        // If any degraded, overall is degraded
        if (cpu.getStatus() == BackendResourceMetricsDto.HealthStatus.DEGRADED ||
            memory.getStatus() == BackendResourceMetricsDto.HealthStatus.DEGRADED ||
            jvm.getStatus() == BackendResourceMetricsDto.HealthStatus.DEGRADED ||
            dbPool.getStatus() == BackendResourceMetricsDto.HealthStatus.DEGRADED ||
            redis.getStatus() == BackendResourceMetricsDto.HealthStatus.DEGRADED ||
            mq.getStatus() == BackendResourceMetricsDto.HealthStatus.DEGRADED) {
            return BackendResourceMetricsDto.HealthStatus.DEGRADED;
        }

        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }

    private long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }
}

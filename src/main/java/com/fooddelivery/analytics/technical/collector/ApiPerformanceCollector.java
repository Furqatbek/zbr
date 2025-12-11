package com.fooddelivery.analytics.technical.collector;

import com.fooddelivery.analytics.technical.dto.ApiPerformanceMetricsDto;
import com.fooddelivery.analytics.technical.dto.BackendResourceMetricsDto;
import com.fooddelivery.analytics.technical.repository.HttpRequestLogRepository;
import com.fooddelivery.analytics.technical.util.PercentileCalculator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for API performance metrics.
 * Sources: HTTP request logs, Micrometer metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiPerformanceCollector {

    private final HttpRequestLogRepository httpRequestLogRepository;
    private final MeterRegistry meterRegistry;
    private final PercentileCalculator percentileCalculator;

    // Thresholds for health status
    private static final double ERROR_RATE_WARNING = 0.01; // 1%
    private static final double ERROR_RATE_CRITICAL = 0.05; // 5%
    private static final long RESPONSE_TIME_WARNING_MS = 500;
    private static final long RESPONSE_TIME_CRITICAL_MS = 2000;
    private static final double TIMEOUT_RATE_WARNING = 0.001; // 0.1%
    private static final double TIMEOUT_RATE_CRITICAL = 0.01; // 1%

    /**
     * Collect API performance metrics for a period.
     */
    public ApiPerformanceMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate,
                                            int topEndpointsLimit, boolean includeHourly) {
        log.debug("Collecting API performance metrics from {} to {}", startDate, endDate);

        long periodMinutes = Duration.between(startDate, endDate).toMinutes();
        if (periodMinutes == 0) periodMinutes = 1;

        // Get counts
        Long totalRequests = httpRequestLogRepository.countRequestsInPeriod(startDate, endDate);
        if (totalRequests == null) totalRequests = 0L;

        Long clientErrors = httpRequestLogRepository.countByStatusCodeRange(startDate, endDate, 400, 500);
        Long serverErrors = httpRequestLogRepository.countByStatusCodeRange(startDate, endDate, 500, 600);
        Long timeouts = httpRequestLogRepository.countTimeouts(startDate, endDate);

        if (clientErrors == null) clientErrors = 0L;
        if (serverErrors == null) serverErrors = 0L;
        if (timeouts == null) timeouts = 0L;

        // Calculate rates
        double requestsPerMinute = totalRequests.doubleValue() / periodMinutes;
        double errorRate = totalRequests > 0 ? (clientErrors + serverErrors) / totalRequests.doubleValue() : 0.0;
        double clientErrorRate = totalRequests > 0 ? clientErrors / totalRequests.doubleValue() : 0.0;
        double serverErrorRate = totalRequests > 0 ? serverErrors / totalRequests.doubleValue() : 0.0;
        double timeoutRate = totalRequests > 0 ? timeouts / totalRequests.doubleValue() : 0.0;

        // Response time stats
        Double avgResponseTime = httpRequestLogRepository.getAverageResponseTime(startDate, endDate);
        Long maxResponseTime = httpRequestLogRepository.getMaxResponseTime(startDate, endDate);
        Long minResponseTime = httpRequestLogRepository.getMinResponseTime(startDate, endDate);

        // Calculate percentiles
        List<Long> responseTimes = httpRequestLogRepository.getAllResponseTimesOrdered(startDate, endDate);
        ApiPerformanceMetricsDto.ResponseTimePercentilesDto percentiles = calculatePercentiles(responseTimes);

        // Get request count by method
        Map<String, Long> requestsByMethod = new HashMap<>();
        httpRequestLogRepository.getRequestCountByMethod(startDate, endDate)
                .forEach(row -> requestsByMethod.put((String) row[0], ((Number) row[1]).longValue()));

        // Get request count by status
        Map<Integer, Long> requestsByStatus = new HashMap<>();
        httpRequestLogRepository.getRequestCountByStatusCode(startDate, endDate)
                .forEach(row -> requestsByStatus.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue()));

        // Get slowest endpoints
        List<ApiPerformanceMetricsDto.EndpointMetricsDto> slowestEndpoints =
                getTopEndpointsByResponseTime(startDate, endDate, topEndpointsLimit);

        // Get highest error rate endpoints
        List<ApiPerformanceMetricsDto.EndpointMetricsDto> highestErrorEndpoints =
                getTopEndpointsByErrorRate(startDate, endDate, topEndpointsLimit);

        // Get most requested endpoints
        List<ApiPerformanceMetricsDto.EndpointMetricsDto> mostRequestedEndpoints =
                getTopEndpointsByRequestCount(startDate, endDate, topEndpointsLimit);

        // Hourly distribution
        List<ApiPerformanceMetricsDto.HourlyMetricsDto> hourlyDistribution = null;
        if (includeHourly) {
            hourlyDistribution = getHourlyDistribution(startDate, endDate);
        }

        // Calculate health status
        BackendResourceMetricsDto.HealthStatus status = calculateHealthStatus(errorRate, percentiles, timeoutRate);

        return ApiPerformanceMetricsDto.builder()
                .totalRequests(totalRequests)
                .requestsPerMinute(requestsPerMinute)
                .avgResponseTimeMs(avgResponseTime)
                .maxResponseTimeMs(maxResponseTime)
                .minResponseTimeMs(minResponseTime)
                .responseTimePercentiles(percentiles)
                .totalErrors(clientErrors + serverErrors)
                .errorRate(errorRate)
                .clientErrorRate(clientErrorRate)
                .serverErrorRate(serverErrorRate)
                .timeoutCount(timeouts)
                .timeoutRate(timeoutRate)
                .requestsByMethod(requestsByMethod)
                .requestsByStatus(requestsByStatus)
                .slowestEndpoints(slowestEndpoints)
                .highestErrorEndpoints(highestErrorEndpoints)
                .mostRequestedEndpoints(mostRequestedEndpoints)
                .hourlyDistribution(hourlyDistribution)
                .periodStart(startDate)
                .periodEnd(endDate)
                .status(status)
                .build();
    }

    /**
     * Get real-time metrics from Micrometer.
     */
    public ApiPerformanceMetricsDto collectRealTime() {
        log.debug("Collecting real-time API metrics from Micrometer");

        try {
            Timer httpTimer = meterRegistry.find("http.server.requests").timer();
            if (httpTimer != null) {
                long totalRequests = httpTimer.count();
                double meanMs = httpTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
                double maxMs = httpTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS);

                return ApiPerformanceMetricsDto.builder()
                        .totalRequests(totalRequests)
                        .avgResponseTimeMs(meanMs)
                        .maxResponseTimeMs((long) maxMs)
                        .periodStart(LocalDateTime.now().minusMinutes(5))
                        .periodEnd(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to collect real-time metrics from Micrometer: {}", e.getMessage());
        }

        return ApiPerformanceMetricsDto.builder()
                .totalRequests(0L)
                .periodStart(LocalDateTime.now().minusMinutes(5))
                .periodEnd(LocalDateTime.now())
                .build();
    }

    private ApiPerformanceMetricsDto.ResponseTimePercentilesDto calculatePercentiles(List<Long> responseTimes) {
        if (responseTimes == null || responseTimes.isEmpty()) {
            return ApiPerformanceMetricsDto.ResponseTimePercentilesDto.builder()
                    .p50(0L).p75(0L).p90(0L).p95(0L).p99(0L)
                    .build();
        }

        return ApiPerformanceMetricsDto.ResponseTimePercentilesDto.builder()
                .p50(percentileCalculator.calculatePercentile(responseTimes, 50))
                .p75(percentileCalculator.calculatePercentile(responseTimes, 75))
                .p90(percentileCalculator.calculatePercentile(responseTimes, 90))
                .p95(percentileCalculator.calculatePercentile(responseTimes, 95))
                .p99(percentileCalculator.calculatePercentile(responseTimes, 99))
                .build();
    }

    private List<ApiPerformanceMetricsDto.EndpointMetricsDto> getTopEndpointsByResponseTime(
            LocalDateTime start, LocalDateTime end, int limit) {

        return httpRequestLogRepository.getSlowestEndpoints(start, end).stream()
                .limit(limit)
                .map(row -> ApiPerformanceMetricsDto.EndpointMetricsDto.builder()
                        .endpoint((String) row[0])
                        .method((String) row[1])
                        .avgResponseTimeMs(((Number) row[2]).doubleValue())
                        .requestCount(((Number) row[3]).longValue())
                        .errorCount(((Number) row[4]).longValue())
                        .errorRate(((Number) row[3]).longValue() > 0 ?
                                ((Number) row[4]).doubleValue() / ((Number) row[3]).longValue() : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ApiPerformanceMetricsDto.EndpointMetricsDto> getTopEndpointsByErrorRate(
            LocalDateTime start, LocalDateTime end, int limit) {

        return httpRequestLogRepository.getHighestErrorRateEndpoints(start, end, 10).stream()
                .limit(limit)
                .map(row -> {
                    long total = ((Number) row[2]).longValue();
                    long errors = ((Number) row[3]).longValue();
                    return ApiPerformanceMetricsDto.EndpointMetricsDto.builder()
                            .endpoint((String) row[0])
                            .method((String) row[1])
                            .requestCount(total)
                            .errorCount(errors)
                            .errorRate(total > 0 ? (double) errors / total : 0.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<ApiPerformanceMetricsDto.EndpointMetricsDto> getTopEndpointsByRequestCount(
            LocalDateTime start, LocalDateTime end, int limit) {

        return httpRequestLogRepository.getSlowestEndpoints(start, end).stream()
                .sorted((a, b) -> Long.compare(((Number) b[3]).longValue(), ((Number) a[3]).longValue()))
                .limit(limit)
                .map(row -> ApiPerformanceMetricsDto.EndpointMetricsDto.builder()
                        .endpoint((String) row[0])
                        .method((String) row[1])
                        .avgResponseTimeMs(((Number) row[2]).doubleValue())
                        .requestCount(((Number) row[3]).longValue())
                        .errorCount(((Number) row[4]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ApiPerformanceMetricsDto.HourlyMetricsDto> getHourlyDistribution(
            LocalDateTime start, LocalDateTime end) {

        return httpRequestLogRepository.getHourlyDistribution(start, end).stream()
                .map(row -> ApiPerformanceMetricsDto.HourlyMetricsDto.builder()
                        .hour(((Number) row[0]).intValue())
                        .requestCount(((Number) row[1]).longValue())
                        .avgResponseTimeMs(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .errorCount(((Number) row[3]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    private BackendResourceMetricsDto.HealthStatus calculateHealthStatus(
            double errorRate, ApiPerformanceMetricsDto.ResponseTimePercentilesDto percentiles, double timeoutRate) {

        if (errorRate >= ERROR_RATE_CRITICAL ||
            (percentiles.getP99() != null && percentiles.getP99() >= RESPONSE_TIME_CRITICAL_MS) ||
            timeoutRate >= TIMEOUT_RATE_CRITICAL) {
            return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        }

        if (errorRate >= ERROR_RATE_WARNING ||
            (percentiles.getP99() != null && percentiles.getP99() >= RESPONSE_TIME_WARNING_MS) ||
            timeoutRate >= TIMEOUT_RATE_WARNING) {
            return BackendResourceMetricsDto.HealthStatus.DEGRADED;
        }

        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }
}

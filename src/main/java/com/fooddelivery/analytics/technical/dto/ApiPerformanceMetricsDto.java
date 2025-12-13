package com.fooddelivery.analytics.technical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for API Performance metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiPerformanceMetricsDto {

    /**
     * Total requests in the period.
     */
    private Long totalRequests;

    /**
     * Requests per minute (average).
     */
    private Double requestsPerMinute;

    /**
     * Response time percentiles in milliseconds.
     */
    private ResponseTimePercentilesDto responseTimePercentiles;

    /**
     * 4xx error rate (0.0 - 1.0).
     */
    private Double errorRate4xx;

    /**
     * Client error rate (alias for 4xx error rate).
     */
    private Double clientErrorRate;

    /**
     * Server error rate (alias for 5xx error rate).
     */
    private Double serverErrorRate;

    /**
     * 5xx error rate (0.0 - 1.0).
     */
    private Double errorRate5xx;

    /**
     * Timeout rate (0.0 - 1.0).
     */
    private Double timeoutRate;

    /**
     * Total 4xx errors.
     */
    private Long total4xxErrors;

    /**
     * Total 5xx errors.
     */
    private Long total5xxErrors;

    /**
     * Total timeouts.
     */
    private Long totalTimeouts;

    /**
     * Timeout count (alias for totalTimeouts).
     */
    private Long timeoutCount;

    /**
     * Total errors (4xx + 5xx).
     */
    private Long totalErrors;

    /**
     * Overall error rate (0.0 - 1.0).
     */
    private Double errorRate;

    /**
     * API health status (HEALTHY, DEGRADED, CRITICAL).
     */
    private BackendResourceMetricsDto.HealthStatus status;

    /**
     * Average response time in ms.
     */
    private Double avgResponseTimeMs;

    /**
     * Max response time in ms.
     */
    private Long maxResponseTimeMs;

    /**
     * Min response time in ms.
     */
    private Long minResponseTimeMs;

    /**
     * Requests breakdown by HTTP method.
     */
    private Map<String, Long> requestsByMethod;

    /**
     * Requests breakdown by status code range.
     */
    private Map<String, Long> requestsByStatusRange;

    /**
     * Requests breakdown by status (alias).
     */
    private Map<String, Long> requestsByStatus;

    /**
     * Top slowest endpoints.
     */
    private List<EndpointMetricsDto> slowestEndpoints;

    /**
     * Most requested endpoints.
     */
    private List<EndpointMetricsDto> mostRequestedEndpoints;

    /**
     * Top endpoints by error rate.
     */
    private List<EndpointMetricsDto> highestErrorEndpoints;

    /**
     * Hourly request distribution.
     */
    private List<HourlyMetricsDto> hourlyDistribution;

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
    public static class ResponseTimePercentilesDto {
        private Long p50;
        private Long p75;
        private Long p90;
        private Long p95;
        private Long p99;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EndpointMetricsDto {
        private String endpoint;
        private String method;
        private Long requestCount;
        private Double avgResponseTimeMs;
        private Long p99ResponseTimeMs;
        private Double errorRate;
        private Long errorCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyMetricsDto {
        private Integer hour;
        private Long requestCount;
        private Double avgResponseTimeMs;
        private Double errorRate;
        private Long errorCount;
    }
}

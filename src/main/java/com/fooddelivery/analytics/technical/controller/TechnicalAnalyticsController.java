package com.fooddelivery.analytics.technical.controller;

import com.fooddelivery.analytics.technical.dto.*;
import com.fooddelivery.analytics.technical.service.TechnicalAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller for technical analytics endpoints.
 * Provides platform health and performance metrics.
 * Restricted to ADMIN and PLATFORM roles only.
 */
@RestController
@RequestMapping("/api/v1/analytics/technical")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Technical Analytics", description = "Platform health and performance metrics")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
public class TechnicalAnalyticsController {

    private final TechnicalAnalyticsService technicalAnalyticsService;

    @Operation(summary = "Get technical summary",
            description = "Returns a high-level summary of all technical metrics with health scores")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved summary",
                    content = @Content(schema = @Schema(implementation = TechnicalSummaryDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/summary")
    public ResponseEntity<TechnicalSummaryDto> getTechnicalSummary(
            @Parameter(description = "Start date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("GET /api/v1/analytics/technical/summary from {} to {}", startDate, endDate);
        TechnicalSummaryDto summary = technicalAnalyticsService.getTechnicalSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get API performance metrics",
            description = "Returns detailed API performance metrics including response times, error rates, and endpoint analysis")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved API metrics",
                    content = @Content(schema = @Schema(implementation = ApiPerformanceMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/api-performance")
    public ResponseEntity<ApiPerformanceMetricsDto> getApiPerformanceMetrics(
            @Valid @RequestBody TechnicalMetricsRequest request) {

        log.info("POST /api/v1/analytics/technical/api-performance");
        ApiPerformanceMetricsDto metrics = technicalAnalyticsService.getApiPerformanceMetrics(request);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get real-time API metrics",
            description = "Returns current API performance from Micrometer metrics")
    @GetMapping("/api-performance/realtime")
    public ResponseEntity<ApiPerformanceMetricsDto> getApiPerformanceRealTime() {
        log.info("GET /api/v1/analytics/technical/api-performance/realtime");
        ApiPerformanceMetricsDto metrics = technicalAnalyticsService.getApiPerformanceRealTime();
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get backend resource metrics",
            description = "Returns current backend resource usage (CPU, memory, JVM, DB pool, Redis)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved backend metrics",
                    content = @Content(schema = @Schema(implementation = BackendResourceMetricsDto.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/backend-resources")
    public ResponseEntity<BackendResourceMetricsDto> getBackendResourceMetrics() {
        log.info("GET /api/v1/analytics/technical/backend-resources");
        BackendResourceMetricsDto metrics = technicalAnalyticsService.getBackendResourceMetrics();
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get historical backend metrics",
            description = "Returns averaged backend metrics for a time period")
    @GetMapping("/backend-resources/historical")
    public ResponseEntity<BackendResourceMetricsDto> getBackendResourceMetricsHistorical(
            @Parameter(description = "Start date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("GET /api/v1/analytics/technical/backend-resources/historical from {} to {}", startDate, endDate);
        BackendResourceMetricsDto metrics = technicalAnalyticsService.getBackendResourceMetricsHistorical(startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get database performance metrics",
            description = "Returns database performance metrics including slow queries, index usage, and backup status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved database metrics",
                    content = @Content(schema = @Schema(implementation = DatabasePerformanceMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/database-performance")
    public ResponseEntity<DatabasePerformanceMetricsDto> getDatabasePerformanceMetrics(
            @Valid @RequestBody TechnicalMetricsRequest request) {

        log.info("POST /api/v1/analytics/technical/database-performance");
        DatabasePerformanceMetricsDto metrics = technicalAnalyticsService.getDatabasePerformanceMetrics(request);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get WebSocket metrics",
            description = "Returns WebSocket connection and message delivery metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved WebSocket metrics",
                    content = @Content(schema = @Schema(implementation = WebSocketMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/websocket")
    public ResponseEntity<WebSocketMetricsDto> getWebSocketMetrics(
            @Valid @RequestBody TechnicalMetricsRequest request) {

        log.info("POST /api/v1/analytics/technical/websocket");
        WebSocketMetricsDto metrics = technicalAnalyticsService.getWebSocketMetrics(request);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get real-time WebSocket metrics",
            description = "Returns current WebSocket connection status")
    @GetMapping("/websocket/realtime")
    public ResponseEntity<WebSocketMetricsDto> getWebSocketMetricsRealTime() {
        log.info("GET /api/v1/analytics/technical/websocket/realtime");
        WebSocketMetricsDto metrics = technicalAnalyticsService.getWebSocketMetricsRealTime();
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get storage metrics",
            description = "Returns storage and file upload metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved storage metrics",
                    content = @Content(schema = @Schema(implementation = StorageMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/storage")
    public ResponseEntity<StorageMetricsDto> getStorageMetrics(
            @Valid @RequestBody TechnicalMetricsRequest request) {

        log.info("POST /api/v1/analytics/technical/storage");
        StorageMetricsDto metrics = technicalAnalyticsService.getStorageMetrics(request);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get storage summary",
            description = "Returns current storage totals")
    @GetMapping("/storage/summary")
    public ResponseEntity<StorageMetricsDto> getStorageSummary() {
        log.info("GET /api/v1/analytics/technical/storage/summary");
        StorageMetricsDto summary = technicalAnalyticsService.getStorageSummary();
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get health scores",
            description = "Returns health scores for all subsystems")
    @GetMapping("/health-scores")
    public ResponseEntity<HealthScoresResponse> getHealthScores(
            @Parameter(description = "Start date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("GET /api/v1/analytics/technical/health-scores from {} to {}", startDate, endDate);

        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        ApiPerformanceMetricsDto apiMetrics = technicalAnalyticsService.getApiPerformanceMetrics(request);
        BackendResourceMetricsDto backendMetrics = technicalAnalyticsService.getBackendResourceMetrics();
        DatabasePerformanceMetricsDto dbMetrics = technicalAnalyticsService.getDatabasePerformanceMetrics(request);
        WebSocketMetricsDto wsMetrics = technicalAnalyticsService.getWebSocketMetrics(request);
        StorageMetricsDto storageMetrics = technicalAnalyticsService.getStorageMetrics(request);

        HealthScoresResponse response = new HealthScoresResponse(
                technicalAnalyticsService.calculateApiHealthScore(apiMetrics),
                technicalAnalyticsService.calculateBackendHealthScore(backendMetrics),
                technicalAnalyticsService.calculateDatabaseHealthScore(dbMetrics),
                technicalAnalyticsService.calculateWebSocketHealthScore(wsMetrics),
                technicalAnalyticsService.calculateStorageHealthScore(storageMetrics)
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh metrics cache",
            description = "Forces a refresh of all cached technical metrics")
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshMetrics() {
        log.info("POST /api/v1/analytics/technical/refresh");
        technicalAnalyticsService.refreshAllMetrics();
        return ResponseEntity.ok().build();
    }

    /**
     * Response DTO for health scores endpoint.
     */
    public record HealthScoresResponse(
            Integer apiHealthScore,
            Integer backendHealthScore,
            Integer databaseHealthScore,
            Integer websocketHealthScore,
            Integer storageHealthScore
    ) {}
}

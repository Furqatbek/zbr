package com.fooddelivery.analytics.technical.service;

import com.fooddelivery.analytics.technical.dto.*;

import java.time.LocalDateTime;

/**
 * Service interface for technical analytics operations.
 */
public interface TechnicalAnalyticsService {

    /**
     * Get a summary of all technical metrics.
     *
     * @param startDate Start of the reporting period
     * @param endDate   End of the reporting period
     * @return Technical summary with health scores
     */
    TechnicalSummaryDto getTechnicalSummary(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get detailed API performance metrics.
     *
     * @param request Request parameters
     * @return API performance metrics
     */
    ApiPerformanceMetricsDto getApiPerformanceMetrics(TechnicalMetricsRequest request);

    /**
     * Get real-time API performance metrics.
     *
     * @return Current API metrics from Micrometer
     */
    ApiPerformanceMetricsDto getApiPerformanceRealTime();

    /**
     * Get backend resource metrics.
     *
     * @return Current backend resource usage
     */
    BackendResourceMetricsDto getBackendResourceMetrics();

    /**
     * Get historical backend resource metrics.
     *
     * @param startDate Start of the reporting period
     * @param endDate   End of the reporting period
     * @return Historical backend metrics
     */
    BackendResourceMetricsDto getBackendResourceMetricsHistorical(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get database performance metrics.
     *
     * @param request Request parameters
     * @return Database performance metrics
     */
    DatabasePerformanceMetricsDto getDatabasePerformanceMetrics(TechnicalMetricsRequest request);

    /**
     * Get WebSocket metrics.
     *
     * @param request Request parameters
     * @return WebSocket metrics
     */
    WebSocketMetricsDto getWebSocketMetrics(TechnicalMetricsRequest request);

    /**
     * Get real-time WebSocket metrics.
     *
     * @return Current WebSocket connection status
     */
    WebSocketMetricsDto getWebSocketMetricsRealTime();

    /**
     * Get storage/upload metrics.
     *
     * @param request Request parameters
     * @return Storage metrics
     */
    StorageMetricsDto getStorageMetrics(TechnicalMetricsRequest request);

    /**
     * Get current storage summary.
     *
     * @return Storage summary
     */
    StorageMetricsDto getStorageSummary();

    /**
     * Calculate health score for API (0-100).
     *
     * @param metrics API metrics
     * @return Health score
     */
    Integer calculateApiHealthScore(ApiPerformanceMetricsDto metrics);

    /**
     * Calculate health score for backend (0-100).
     *
     * @param metrics Backend metrics
     * @return Health score
     */
    Integer calculateBackendHealthScore(BackendResourceMetricsDto metrics);

    /**
     * Calculate health score for database (0-100).
     *
     * @param metrics Database metrics
     * @return Health score
     */
    Integer calculateDatabaseHealthScore(DatabasePerformanceMetricsDto metrics);

    /**
     * Calculate health score for WebSocket (0-100).
     *
     * @param metrics WebSocket metrics
     * @return Health score
     */
    Integer calculateWebSocketHealthScore(WebSocketMetricsDto metrics);

    /**
     * Calculate health score for storage (0-100).
     *
     * @param metrics Storage metrics
     * @return Health score
     */
    Integer calculateStorageHealthScore(StorageMetricsDto metrics);

    /**
     * Refresh all cached metrics.
     */
    void refreshAllMetrics();

    /**
     * Get active alerts count.
     *
     * @return Number of active alerts
     */
    Integer getActiveAlertsCount();
}

package com.fooddelivery.analytics.fraud.controller;

import com.fooddelivery.analytics.fraud.dto.*;
import com.fooddelivery.analytics.fraud.service.FraudAnalyticsService;
import com.fooddelivery.analytics.fraud.util.FraudConstants;
import com.fooddelivery.analytics.fraud.util.FraudDateUtils;
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
 * REST controller for fraud analytics and detection.
 * Provides endpoints for retrieving fraud metrics, risk assessments, and alerts.
 */
@RestController
@RequestMapping("/api/v1/analytics/fraud")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fraud Analytics", description = "APIs for fraud detection and security metrics")
public class FraudAnalyticsController {

    private final FraudAnalyticsService fraudAnalyticsService;

    @Operation(summary = "Get comprehensive fraud summary",
            description = "Returns overall fraud risk assessment with metrics from all categories")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved fraud summary",
                    content = @Content(schema = @Schema(implementation = FraudSummaryDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @PostMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<FraudSummaryDto> getFraudSummary(
            @Valid @RequestBody FraudMetricsRequest request) {
        log.info("Received request for fraud summary from {} to {}",
                request.getStartDate(), request.getEndDate());
        FraudSummaryDto summary = fraudAnalyticsService.getFraudSummary(request);
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get fraud summary with default parameters",
            description = "Returns fraud summary for the last 24 hours with default thresholds")
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<FraudSummaryDto> getFraudSummaryDefault(
            @Parameter(description = "Start date (defaults to 24 hours ago)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (defaults to now)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (startDate == null || endDate == null) {
            LocalDateTime[] defaultPeriod = FraudDateUtils.getDefaultAnalysisPeriod();
            startDate = defaultPeriod[0];
            endDate = defaultPeriod[1];
        }

        FraudMetricsRequest request = FraudMetricsRequest.createDefault(startDate, endDate);
        return getFraudSummary(request);
    }

    @Operation(summary = "Get payment fraud metrics",
            description = "Returns detailed payment fraud metrics including failed payments, card testing patterns, and high-risk users")
    @GetMapping("/payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST') or hasRole('PAYMENT_ANALYST')")
    public ResponseEntity<PaymentFraudMetricsDto> getPaymentFraudMetrics(
            @Parameter(description = "Start date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Threshold for failed payments to flag a user")
            @RequestParam(defaultValue = "3") int failedThreshold,

            @Parameter(description = "Time window in minutes for velocity analysis")
            @RequestParam(defaultValue = "60") int timeWindowMinutes,

            @Parameter(description = "Include detailed user lists")
            @RequestParam(defaultValue = "false") boolean includeDetails,

            @Parameter(description = "Maximum size for detailed lists")
            @RequestParam(defaultValue = "100") int maxListSize) {

        log.info("Received request for payment fraud metrics from {} to {}", startDate, endDate);
        PaymentFraudMetricsDto metrics = fraudAnalyticsService.getPaymentFraudMetrics(
                startDate, endDate, failedThreshold, timeWindowMinutes, includeDetails, maxListSize);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get behavioral fraud metrics",
            description = "Returns behavioral fraud metrics including velocity violations, refund abuse, and order value anomalies")
    @GetMapping("/behavioral")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<BehavioralFraudMetricsDto> getBehavioralFraudMetrics(
            @Parameter(description = "Start date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Maximum orders per hour threshold")
            @RequestParam(defaultValue = "5") int velocityThreshold,

            @Parameter(description = "High refund rate threshold (percentage)")
            @RequestParam(defaultValue = "30.0") double refundRateThreshold,

            @Parameter(description = "Z-score threshold for order value anomalies")
            @RequestParam(defaultValue = "3.0") double orderValueZScore,

            @Parameter(description = "Threshold for users per address")
            @RequestParam(defaultValue = "3") int addressUserThreshold,

            @Parameter(description = "Include detailed lists")
            @RequestParam(defaultValue = "false") boolean includeDetails,

            @Parameter(description = "Maximum size for detailed lists")
            @RequestParam(defaultValue = "100") int maxListSize) {

        log.info("Received request for behavioral fraud metrics from {} to {}", startDate, endDate);
        BehavioralFraudMetricsDto metrics = fraudAnalyticsService.getBehavioralFraudMetrics(
                startDate, endDate, velocityThreshold, refundRateThreshold,
                orderValueZScore, addressUserThreshold, includeDetails, maxListSize);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get account integrity metrics",
            description = "Returns account integrity metrics including fake account detection and multi-account analysis")
    @GetMapping("/account-integrity")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<AccountIntegrityMetricsDto> getAccountIntegrityMetrics(
            @Parameter(description = "Start date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Account age threshold in hours for fake account detection")
            @RequestParam(defaultValue = "24") int fakeAccountAgeThreshold,

            @Parameter(description = "Threshold for users per device")
            @RequestParam(defaultValue = "2") int deviceThreshold,

            @Parameter(description = "Threshold for users per IP")
            @RequestParam(defaultValue = "5") int ipThreshold,

            @Parameter(description = "Include detailed lists")
            @RequestParam(defaultValue = "false") boolean includeDetails,

            @Parameter(description = "Maximum size for detailed lists")
            @RequestParam(defaultValue = "100") int maxListSize) {

        log.info("Received request for account integrity metrics from {} to {}", startDate, endDate);
        AccountIntegrityMetricsDto metrics = fraudAnalyticsService.getAccountIntegrityMetrics(
                startDate, endDate, fakeAccountAgeThreshold, deviceThreshold,
                ipThreshold, includeDetails, maxListSize);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get referral fraud metrics",
            description = "Returns referral fraud metrics including device/IP reuse and circular referral detection")
    @GetMapping("/referral")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST') or hasRole('MARKETING_ANALYST')")
    public ResponseEntity<ReferralFraudMetricsDto> getReferralFraudMetrics(
            @Parameter(description = "Start date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Threshold for signups per device")
            @RequestParam(defaultValue = "2") int deviceSignupThreshold,

            @Parameter(description = "Threshold for signups per IP")
            @RequestParam(defaultValue = "5") int ipSignupThreshold,

            @Parameter(description = "Promo abuse rate threshold (percentage)")
            @RequestParam(defaultValue = "80.0") double promoAbuseRate,

            @Parameter(description = "Include detailed lists")
            @RequestParam(defaultValue = "false") boolean includeDetails,

            @Parameter(description = "Maximum size for detailed lists")
            @RequestParam(defaultValue = "100") int maxListSize) {

        log.info("Received request for referral fraud metrics from {} to {}", startDate, endDate);
        ReferralFraudMetricsDto metrics = fraudAnalyticsService.getReferralFraudMetrics(
                startDate, endDate, deviceSignupThreshold, ipSignupThreshold,
                promoAbuseRate, includeDetails, maxListSize);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get security log metrics",
            description = "Returns security metrics including login failures, rate limits, and IP threat analysis")
    @GetMapping("/security")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_ANALYST')")
    public ResponseEntity<SecurityLogMetricsDto> getSecurityLogMetrics(
            @Parameter(description = "Start date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Threshold for failed logins")
            @RequestParam(defaultValue = "5") int failedLoginThreshold,

            @Parameter(description = "Time window in minutes for login velocity")
            @RequestParam(defaultValue = "15") int loginTimeWindowMinutes,

            @Parameter(description = "Rate limit violation threshold")
            @RequestParam(defaultValue = "100") int rateLimitThreshold,

            @Parameter(description = "Include detailed lists")
            @RequestParam(defaultValue = "false") boolean includeDetails,

            @Parameter(description = "Maximum size for detailed lists")
            @RequestParam(defaultValue = "100") int maxListSize) {

        log.info("Received request for security log metrics from {} to {}", startDate, endDate);
        SecurityLogMetricsDto metrics = fraudAnalyticsService.getSecurityLogMetrics(
                startDate, endDate, failedLoginThreshold, loginTimeWindowMinutes,
                rateLimitThreshold, includeDetails, maxListSize);
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get real-time fraud alerts",
            description = "Returns current active fraud alerts based on real-time analysis")
    @PostMapping("/alerts/realtime")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST') or hasRole('SECURITY_ANALYST')")
    public ResponseEntity<FraudSummaryDto> getRealTimeAlerts(
            @Valid @RequestBody FraudMetricsRequest request) {
        log.info("Received request for real-time fraud alerts");
        FraudSummaryDto alerts = fraudAnalyticsService.getRealTimeAlerts(request);
        return ResponseEntity.ok(alerts);
    }

    @Operation(summary = "Get user fraud risk score",
            description = "Returns comprehensive fraud risk assessment for a specific user")
    @GetMapping("/user/{userId}/risk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<UserFraudRiskDto> getUserRiskScore(
            @Parameter(description = "User ID to analyze")
            @PathVariable Long userId,

            @Parameter(description = "Start date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date for analysis period")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Received request for user {} risk score from {} to {}", userId, startDate, endDate);
        UserFraudRiskDto userRisk = fraudAnalyticsService.getUserRiskScore(userId, startDate, endDate);
        return ResponseEntity.ok(userRisk);
    }

    @Operation(summary = "Invalidate fraud analytics cache",
            description = "Clears all cached fraud metrics. Use after significant data changes.")
    @PostMapping("/cache/invalidate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> invalidateCache() {
        log.info("Invalidating fraud analytics cache");
        fraudAnalyticsService.invalidateCache();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Invalidate specific category cache",
            description = "Clears cached metrics for a specific fraud category")
    @PostMapping("/cache/invalidate/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> invalidateCategoryCache(
            @Parameter(description = "Category name (PAYMENT, BEHAVIORAL, ACCOUNT, REFERRAL, SECURITY)")
            @PathVariable String category) {
        log.info("Invalidating cache for category: {}", category);
        fraudAnalyticsService.invalidateCategoryCache(category);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Health check for fraud analytics service",
            description = "Returns service health status")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Fraud Analytics Service is operational");
    }
}

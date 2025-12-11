package com.fooddelivery.analytics.fraud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.analytics.fraud.dto.*;
import com.fooddelivery.analytics.fraud.service.FraudAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FraudAnalyticsController.class)
@DisplayName("FraudAnalyticsController Unit Tests")
class FraudAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudAnalyticsService fraudAnalyticsService;

    private ObjectMapper objectMapper;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        startDate = LocalDateTime.now().minusDays(1);
        endDate = LocalDateTime.now();
    }

    @Nested
    @DisplayName("POST /api/v1/analytics/fraud/summary")
    class GetFraudSummaryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return fraud summary for admin")
        void shouldReturnFraudSummaryForAdmin() throws Exception {
            // Arrange
            FraudMetricsRequest request = FraudMetricsRequest.createDefault(startDate, endDate);

            FraudSummaryDto expectedSummary = FraudSummaryDto.builder()
                    .overallRiskScore(45)
                    .riskLevel("MEDIUM")
                    .totalFlaggedUsers(25L)
                    .totalSecurityIncidents(100L)
                    .financialImpact(FraudSummaryDto.FinancialImpactDto.builder()
                            .totalEstimatedLoss(BigDecimal.valueOf(5000))
                            .build())
                    .build();

            when(fraudAnalyticsService.getFraudSummary(any(FraudMetricsRequest.class)))
                    .thenReturn(expectedSummary);

            // Act & Assert
            mockMvc.perform(post("/api/v1/analytics/fraud/summary")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overallRiskScore").value(45))
                    .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                    .andExpect(jsonPath("$.totalFlaggedUsers").value(25));

            verify(fraudAnalyticsService).getFraudSummary(any(FraudMetricsRequest.class));
        }

        @Test
        @WithMockUser(roles = "FRAUD_ANALYST")
        @DisplayName("Should allow fraud analyst access")
        void shouldAllowFraudAnalystAccess() throws Exception {
            // Arrange
            FraudMetricsRequest request = FraudMetricsRequest.createDefault(startDate, endDate);
            FraudSummaryDto summary = FraudSummaryDto.builder()
                    .overallRiskScore(30)
                    .riskLevel("LOW")
                    .build();

            when(fraudAnalyticsService.getFraudSummary(any())).thenReturn(summary);

            // Act & Assert
            mockMvc.perform(post("/api/v1/analytics/fraud/summary")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should deny access for regular user")
        void shouldDenyAccessForRegularUser() throws Exception {
            // Arrange
            FraudMetricsRequest request = FraudMetricsRequest.createDefault(startDate, endDate);

            // Act & Assert
            mockMvc.perform(post("/api/v1/analytics/fraud/summary")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/fraud/payment")
    class GetPaymentFraudMetricsTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return payment fraud metrics")
        void shouldReturnPaymentFraudMetrics() throws Exception {
            // Arrange
            PaymentFraudMetricsDto metrics = PaymentFraudMetricsDto.builder()
                    .totalPaymentAttempts(1000L)
                    .failedPayments(50L)
                    .failedPaymentRate(5.0)
                    .declineRate(3.0)
                    .highRiskUsers(10L)
                    .build();

            when(fraudAnalyticsService.getPaymentFraudMetrics(
                    any(), any(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get("/api/v1/analytics/fraud/payment")
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString())
                            .param("failedThreshold", "3")
                            .param("timeWindowMinutes", "60"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPaymentAttempts").value(1000))
                    .andExpect(jsonPath("$.failedPayments").value(50))
                    .andExpect(jsonPath("$.failedPaymentRate").value(5.0));
        }

        @Test
        @WithMockUser(roles = "PAYMENT_ANALYST")
        @DisplayName("Should allow payment analyst access")
        void shouldAllowPaymentAnalystAccess() throws Exception {
            // Arrange
            PaymentFraudMetricsDto metrics = PaymentFraudMetricsDto.builder()
                    .totalPaymentAttempts(500L)
                    .build();

            when(fraudAnalyticsService.getPaymentFraudMetrics(
                    any(), any(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get("/api/v1/analytics/fraud/payment")
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/fraud/behavioral")
    class GetBehavioralFraudMetricsTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return behavioral fraud metrics")
        void shouldReturnBehavioralFraudMetrics() throws Exception {
            // Arrange
            BehavioralFraudMetricsDto metrics = BehavioralFraudMetricsDto.builder()
                    .velocityMetrics(BehavioralFraudMetricsDto.VelocityMetricsDto.builder()
                            .totalOrdersAnalyzed(5000L)
                            .velocityViolations(15L)
                            .build())
                    .refundMetrics(BehavioralFraudMetricsDto.RefundMetricsDto.builder()
                            .totalRefunds(100L)
                            .highRefundUsers(8L)
                            .build())
                    .build();

            when(fraudAnalyticsService.getBehavioralFraudMetrics(
                    any(), any(), anyInt(), anyDouble(), anyDouble(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get("/api/v1/analytics/fraud/behavioral")
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString())
                            .param("velocityThreshold", "5")
                            .param("refundRateThreshold", "30.0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.velocityMetrics.totalOrdersAnalyzed").value(5000))
                    .andExpect(jsonPath("$.velocityMetrics.velocityViolations").value(15));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/fraud/account-integrity")
    class GetAccountIntegrityMetricsTests {

        @Test
        @WithMockUser(roles = "FRAUD_ANALYST")
        @DisplayName("Should return account integrity metrics")
        void shouldReturnAccountIntegrityMetrics() throws Exception {
            // Arrange
            AccountIntegrityMetricsDto metrics = AccountIntegrityMetricsDto.builder()
                    .totalFlaggedAccounts(15L)
                    .accountIntegrityScore(85.0)
                    .fakeAccounts(AccountIntegrityMetricsDto.FakeAccountMetricsDto.builder()
                            .totalNewAccounts(100L)
                            .suspectedFakeAccounts(5L)
                            .build())
                    .build();

            when(fraudAnalyticsService.getAccountIntegrityMetrics(
                    any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get("/api/v1/analytics/fraud/account-integrity")
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalFlaggedAccounts").value(15))
                    .andExpect(jsonPath("$.accountIntegrityScore").value(85.0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/fraud/referral")
    class GetReferralFraudMetricsTests {

        @Test
        @WithMockUser(roles = "MARKETING_ANALYST")
        @DisplayName("Should allow marketing analyst access")
        void shouldAllowMarketingAnalystAccess() throws Exception {
            // Arrange
            ReferralFraudMetricsDto metrics = ReferralFraudMetricsDto.builder()
                    .totalReferrals(500L)
                    .totalFraudulentReferrals(25L)
                    .overallReferralFraudRate(5.0)
                    .build();

            when(fraudAnalyticsService.getReferralFraudMetrics(
                    any(), any(), anyInt(), anyInt(), anyDouble(), anyBoolean(), anyInt()))
                    .thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get("/api/v1/analytics/fraud/referral")
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalReferrals").value(500))
                    .andExpect(jsonPath("$.totalFraudulentReferrals").value(25));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/fraud/security")
    class GetSecurityLogMetricsTests {

        @Test
        @WithMockUser(roles = "SECURITY_ANALYST")
        @DisplayName("Should allow security analyst access")
        void shouldAllowSecurityAnalystAccess() throws Exception {
            // Arrange
            SecurityLogMetricsDto metrics = SecurityLogMetricsDto.builder()
                    .loginMetrics(SecurityLogMetricsDto.LoginMetricsDto.builder()
                            .totalLogins(10000L)
                            .failedLogins(200L)
                            .failedLoginRate(2.0)
                            .build())
                    .bruteForceMetrics(SecurityLogMetricsDto.BruteForceMetricsDto.builder()
                            .detectedAttacks(2L)
                            .build())
                    .build();

            when(fraudAnalyticsService.getSecurityLogMetrics(
                    any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get("/api/v1/analytics/fraud/security")
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loginMetrics.totalLogins").value(10000))
                    .andExpect(jsonPath("$.bruteForceMetrics.detectedAttacks").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/fraud/user/{userId}/risk")
    class GetUserRiskScoreTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return user risk score")
        void shouldReturnUserRiskScore() throws Exception {
            // Arrange
            Long userId = 1L;
            UserFraudRiskDto userRisk = UserFraudRiskDto.builder()
                    .userId(userId)
                    .overallRiskScore(45)
                    .riskLevel("MEDIUM")
                    .paymentRiskScore(30)
                    .behavioralRiskScore(50)
                    .riskIndicators(List.of("HIGH_FAILURE_RATE", "REFUND_ABUSE"))
                    .recommendedActions(List.of("Monitor account activity"))
                    .build();

            when(fraudAnalyticsService.getUserRiskScore(eq(userId), any(), any()))
                    .thenReturn(userRisk);

            // Act & Assert
            mockMvc.perform(get("/api/v1/analytics/fraud/user/{userId}/risk", userId)
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(userId))
                    .andExpect(jsonPath("$.overallRiskScore").value(45))
                    .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                    .andExpect(jsonPath("$.riskIndicators").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/analytics/fraud/cache/invalidate")
    class CacheInvalidationTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should invalidate cache for admin")
        void shouldInvalidateCacheForAdmin() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/analytics/fraud/cache/invalidate")
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(fraudAnalyticsService).invalidateCache();
        }

        @Test
        @WithMockUser(roles = "FRAUD_ANALYST")
        @DisplayName("Should deny cache invalidation for non-admin")
        void shouldDenyCacheInvalidationForNonAdmin() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/analytics/fraud/cache/invalidate")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/fraud/health")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return health status without authentication")
        void shouldReturnHealthStatus() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/analytics/fraud/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Fraud Analytics Service is operational"));
        }
    }
}

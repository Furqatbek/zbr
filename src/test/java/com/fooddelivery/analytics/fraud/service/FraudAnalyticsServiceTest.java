package com.fooddelivery.analytics.fraud.service;

import com.fooddelivery.analytics.fraud.collector.*;
import com.fooddelivery.analytics.fraud.dto.*;
import com.fooddelivery.analytics.fraud.mapper.FraudSummaryMapper;
import com.fooddelivery.analytics.fraud.repository.*;
import com.fooddelivery.analytics.fraud.util.FraudRiskCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudAnalyticsService Unit Tests")
class FraudAnalyticsServiceTest {

    @Mock
    private PaymentFraudCollector paymentFraudCollector;

    @Mock
    private BehavioralFraudCollector behavioralFraudCollector;

    @Mock
    private AccountIntegrityCollector accountIntegrityCollector;

    @Mock
    private ReferralFraudCollector referralFraudCollector;

    @Mock
    private SecurityLogCollector securityLogCollector;

    @Mock
    private FraudSummaryMapper fraudSummaryMapper;

    @Mock
    private FraudRiskCalculator riskCalculator;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private FraudOrderHistoryRepository orderHistoryRepository;

    @Mock
    private AuthLogRepository authLogRepository;

    @Mock
    private DeviceFingerprintRepository deviceFingerprintRepository;

    @Mock
    private ReferralEventRepository referralEventRepository;

    @InjectMocks
    private FraudAnalyticsServiceImpl fraudAnalyticsService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.now().minusDays(1);
        endDate = LocalDateTime.now();
    }

    @Nested
    @DisplayName("getFraudSummary Tests")
    class GetFraudSummaryTests {

        @Test
        @DisplayName("Should return fraud summary with all metrics")
        void shouldReturnFraudSummaryWithAllMetrics() {
            // Arrange
            FraudMetricsRequest request = FraudMetricsRequest.createDefault(startDate, endDate);

            PaymentFraudMetricsDto paymentMetrics = createMockPaymentMetrics();
            BehavioralFraudMetricsDto behavioralMetrics = createMockBehavioralMetrics();
            AccountIntegrityMetricsDto accountMetrics = createMockAccountMetrics();
            ReferralFraudMetricsDto referralMetrics = createMockReferralMetrics();
            SecurityLogMetricsDto securityMetrics = createMockSecurityMetrics();

            when(paymentFraudCollector.collect(any(), any(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(paymentMetrics);
            when(behavioralFraudCollector.collect(any(), any(), anyInt(), anyInt(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(behavioralMetrics);
            when(accountIntegrityCollector.collect(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(accountMetrics);
            when(referralFraudCollector.collect(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(referralMetrics);
            when(securityLogCollector.collect(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(securityMetrics);

            FraudSummaryDto expectedSummary = FraudSummaryDto.builder()
                    .overallRiskScore(45)
                    .riskLevel("MEDIUM")
                    .totalFlaggedUsers(10L)
                    .build();

            when(fraudSummaryMapper.buildFraudSummary(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(expectedSummary);

            // Act
            FraudSummaryDto result = fraudAnalyticsService.getFraudSummary(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOverallRiskScore()).isEqualTo(45);
            assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");

            verify(paymentFraudCollector).collect(any(), any(), anyInt(), anyInt(), anyBoolean(), anyInt());
            verify(behavioralFraudCollector).collect(any(), any(), anyInt(), anyInt(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyBoolean(), anyInt());
            verify(accountIntegrityCollector).collect(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt());
            verify(referralFraudCollector).collect(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt());
            verify(securityLogCollector).collect(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt());
        }
    }

    @Nested
    @DisplayName("getPaymentFraudMetrics Tests")
    class GetPaymentFraudMetricsTests {

        @Test
        @DisplayName("Should return payment fraud metrics")
        void shouldReturnPaymentFraudMetrics() {
            // Arrange
            PaymentFraudMetricsDto expectedMetrics = createMockPaymentMetrics();
            when(paymentFraudCollector.collect(any(), any(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(expectedMetrics);

            // Act
            PaymentFraudMetricsDto result = fraudAnalyticsService.getPaymentFraudMetrics(
                    startDate, endDate, 3, 60, true, 100);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalPaymentAttempts()).isEqualTo(1000L);
            assertThat(result.getFailedPayments()).isEqualTo(50L);
            assertThat(result.getFailedPaymentRate()).isEqualTo(5.0);

            verify(paymentFraudCollector).collect(startDate, endDate, 3, 60, true, 100);
        }

        @Test
        @DisplayName("Should handle high failure rate scenario")
        void shouldHandleHighFailureRate() {
            // Arrange
            PaymentFraudMetricsDto metrics = PaymentFraudMetricsDto.builder()
                    .totalPaymentAttempts(100L)
                    .failedPayments(80L)
                    .failedPaymentRate(80.0)
                    .highRiskUsers(25L)
                    .build();

            when(paymentFraudCollector.collect(any(), any(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(metrics);

            // Act
            PaymentFraudMetricsDto result = fraudAnalyticsService.getPaymentFraudMetrics(
                    startDate, endDate, 3, 60, false, 100);

            // Assert
            assertThat(result.getFailedPaymentRate()).isEqualTo(80.0);
            assertThat(result.getHighRiskUsers()).isEqualTo(25L);
        }
    }

    @Nested
    @DisplayName("getBehavioralFraudMetrics Tests")
    class GetBehavioralFraudMetricsTests {

        @Test
        @DisplayName("Should return behavioral fraud metrics")
        void shouldReturnBehavioralFraudMetrics() {
            // Arrange
            BehavioralFraudMetricsDto expectedMetrics = createMockBehavioralMetrics();
            when(behavioralFraudCollector.collect(any(), any(), anyInt(), anyInt(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(expectedMetrics);

            // Act
            BehavioralFraudMetricsDto result = fraudAnalyticsService.getBehavioralFraudMetrics(
                    startDate, endDate, 5, 60, 3.0, 30.0, 3, 5, true, 100);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getVelocityMetrics()).isNotNull();
            assertThat(result.getRefundMetrics()).isNotNull();

            verify(behavioralFraudCollector).collect(startDate, endDate, 5, 60, 3.0, 30.0, 3, 5, true, 100);
        }
    }

    @Nested
    @DisplayName("getAccountIntegrityMetrics Tests")
    class GetAccountIntegrityMetricsTests {

        @Test
        @DisplayName("Should return account integrity metrics")
        void shouldReturnAccountIntegrityMetrics() {
            // Arrange
            AccountIntegrityMetricsDto expectedMetrics = createMockAccountMetrics();
            when(accountIntegrityCollector.collect(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(expectedMetrics);

            // Act
            AccountIntegrityMetricsDto result = fraudAnalyticsService.getAccountIntegrityMetrics(
                    startDate, endDate, 24, 2, 5, true, 100);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalFlaggedAccounts()).isEqualTo(15L);
            assertThat(result.getAccountIntegrityScore()).isEqualTo(85.0);

            verify(accountIntegrityCollector).collect(startDate, endDate, 24, 2, 5, true, 100);
        }
    }

    @Nested
    @DisplayName("getReferralFraudMetrics Tests")
    class GetReferralFraudMetricsTests {

        @Test
        @DisplayName("Should return referral fraud metrics")
        void shouldReturnReferralFraudMetrics() {
            // Arrange
            ReferralFraudMetricsDto expectedMetrics = createMockReferralMetrics();
            when(referralFraudCollector.collect(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(expectedMetrics);

            // Act
            ReferralFraudMetricsDto result = fraudAnalyticsService.getReferralFraudMetrics(
                    startDate, endDate, 2, 5, 3, true, 100);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalReferrals()).isEqualTo(500L);
            assertThat(result.getSuspiciousReferrals()).isEqualTo(25L);

            verify(referralFraudCollector).collect(startDate, endDate, 2, 5, 3, true, 100);
        }
    }

    @Nested
    @DisplayName("getSecurityLogMetrics Tests")
    class GetSecurityLogMetricsTests {

        @Test
        @DisplayName("Should return security log metrics")
        void shouldReturnSecurityLogMetrics() {
            // Arrange
            SecurityLogMetricsDto expectedMetrics = createMockSecurityMetrics();
            when(securityLogCollector.collect(any(), any(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt()))
                    .thenReturn(expectedMetrics);

            // Act
            SecurityLogMetricsDto result = fraudAnalyticsService.getSecurityLogMetrics(
                    startDate, endDate, 5, 15, 100, true, 100);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getLoginMetrics()).isNotNull();
            assertThat(result.getIpSecurityMetrics()).isNotNull();

            verify(securityLogCollector).collect(startDate, endDate, 5, 15, 100, true, 100);
        }
    }

    @Nested
    @DisplayName("getUserRiskScore Tests")
    class GetUserRiskScoreTests {

        @Test
        @DisplayName("Should return user risk score")
        void shouldReturnUserRiskScore() {
            // Arrange
            Long userId = 1L;

            when(paymentAttemptRepository.countByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                    .thenReturn(10L);
            when(paymentAttemptRepository.countUserFailedPayments(eq(userId), any(), any()))
                    .thenReturn(2L);
            when(paymentAttemptRepository.getUserTotalFailedAmount(eq(userId), any(), any()))
                    .thenReturn(BigDecimal.valueOf(50.00));

            List<Object[]> orderStats = new ArrayList<>();
            orderStats.add(new Object[]{100L, 5L, BigDecimal.valueOf(25.00)});
            when(orderHistoryRepository.getUserOrderStats(eq(userId), any(), any()))
                    .thenReturn(orderStats);

            when(deviceFingerprintRepository.countUserDevices(eq(userId), any(), any()))
                    .thenReturn(2L);
            when(deviceFingerprintRepository.hasSharedDevice(eq(userId), any(), any()))
                    .thenReturn(false);

            when(referralEventRepository.countByReferrerId(userId))
                    .thenReturn(5L);
            when(referralEventRepository.countSuspiciousReferralsByReferrer(eq(userId), any(), any()))
                    .thenReturn(0L);

            when(authLogRepository.countFailedLoginsByUser(eq(userId), any(), any()))
                    .thenReturn(1L);
            when(authLogRepository.countTotalLoginsByUser(eq(userId), any(), any()))
                    .thenReturn(20L);
            when(authLogRepository.hasVpnUsage(eq(userId), any(), any()))
                    .thenReturn(false);
            when(authLogRepository.hasTorUsage(eq(userId), any(), any()))
                    .thenReturn(false);
            when(authLogRepository.hasRateLimitViolations(eq(userId), any(), any()))
                    .thenReturn(false);

            when(riskCalculator.calculatePaymentRiskScore(anyInt(), anyDouble(), anyInt()))
                    .thenReturn(20);
            when(riskCalculator.calculateRefundRiskScore(anyDouble(), anyLong()))
                    .thenReturn(15);
            when(riskCalculator.calculateLoginRiskScore(anyLong(), anyDouble()))
                    .thenReturn(10);
            when(riskCalculator.calculateOverallRiskScore(anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(25);
            when(riskCalculator.getRiskLevel(25))
                    .thenReturn("LOW");

            // Act
            UserFraudRiskDto result = fraudAnalyticsService.getUserRiskScore(userId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getOverallRiskScore()).isEqualTo(25);
            assertThat(result.getRiskLevel()).isEqualTo("LOW");
            assertThat(result.getPaymentRisk()).isNotNull();
            assertThat(result.getBehavioralRisk()).isNotNull();
            assertThat(result.getAccountRisk()).isNotNull();
            assertThat(result.getSecurityRisk()).isNotNull();
        }

        @Test
        @DisplayName("Should identify high risk user")
        void shouldIdentifyHighRiskUser() {
            // Arrange
            Long userId = 2L;

            when(paymentAttemptRepository.countByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                    .thenReturn(20L);
            when(paymentAttemptRepository.countUserFailedPayments(eq(userId), any(), any()))
                    .thenReturn(15L);
            when(paymentAttemptRepository.getUserTotalFailedAmount(eq(userId), any(), any()))
                    .thenReturn(BigDecimal.valueOf(500.00));

            List<Object[]> orderStats = new ArrayList<>();
            orderStats.add(new Object[]{50L, 25L, BigDecimal.valueOf(30.00)});
            when(orderHistoryRepository.getUserOrderStats(eq(userId), any(), any()))
                    .thenReturn(orderStats);

            when(deviceFingerprintRepository.countUserDevices(eq(userId), any(), any()))
                    .thenReturn(8L);
            when(deviceFingerprintRepository.hasSharedDevice(eq(userId), any(), any()))
                    .thenReturn(true);

            when(referralEventRepository.countByReferrerId(userId))
                    .thenReturn(20L);
            when(referralEventRepository.countSuspiciousReferralsByReferrer(eq(userId), any(), any()))
                    .thenReturn(15L);

            when(authLogRepository.countFailedLoginsByUser(eq(userId), any(), any()))
                    .thenReturn(30L);
            when(authLogRepository.countTotalLoginsByUser(eq(userId), any(), any()))
                    .thenReturn(50L);
            when(authLogRepository.hasVpnUsage(eq(userId), any(), any()))
                    .thenReturn(true);
            when(authLogRepository.hasTorUsage(eq(userId), any(), any()))
                    .thenReturn(true);
            when(authLogRepository.hasRateLimitViolations(eq(userId), any(), any()))
                    .thenReturn(true);

            when(riskCalculator.calculatePaymentRiskScore(anyInt(), anyDouble(), anyInt()))
                    .thenReturn(75);
            when(riskCalculator.calculateRefundRiskScore(anyDouble(), anyLong()))
                    .thenReturn(80);
            when(riskCalculator.calculateLoginRiskScore(anyLong(), anyDouble()))
                    .thenReturn(70);
            when(riskCalculator.calculateOverallRiskScore(anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(85);
            when(riskCalculator.getRiskLevel(85))
                    .thenReturn("CRITICAL");

            // Act
            UserFraudRiskDto result = fraudAnalyticsService.getUserRiskScore(userId, startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOverallRiskScore()).isEqualTo(85);
            assertThat(result.getRiskLevel()).isEqualTo("CRITICAL");
            assertThat(result.getRiskIndicators()).isNotEmpty();
            assertThat(result.getRecommendedActions()).isNotEmpty();
        }
    }

    // Helper methods to create mock data

    private PaymentFraudMetricsDto createMockPaymentMetrics() {
        return PaymentFraudMetricsDto.builder()
                .totalPaymentAttempts(1000L)
                .successfulPayments(950L)
                .failedPayments(50L)
                .failedPaymentRate(5.0)
                .declineRate(3.0)
                .highRiskUsers(10L)
                .totalFailedAmount(BigDecimal.valueOf(2500.00))
                .build();
    }

    private BehavioralFraudMetricsDto createMockBehavioralMetrics() {
        return BehavioralFraudMetricsDto.builder()
                .velocityMetrics(BehavioralFraudMetricsDto.VelocityMetricsDto.builder()
                        .totalOrdersAnalyzed(5000L)
                        .velocityViolations(15L)
                        .build())
                .refundMetrics(BehavioralFraudMetricsDto.RefundMetricsDto.builder()
                        .totalRefunds(100L)
                        .highRefundUsers(8L)
                        .totalRefundAmount(BigDecimal.valueOf(5000.00))
                        .build())
                .build();
    }

    private AccountIntegrityMetricsDto createMockAccountMetrics() {
        return AccountIntegrityMetricsDto.builder()
                .totalFlaggedAccounts(15L)
                .accountIntegrityScore(85.0)
                .fakeAccounts(AccountIntegrityMetricsDto.FakeAccountMetricsDto.builder()
                        .totalNewAccounts(100L)
                        .suspectedFakeAccounts(5L)
                        .build())
                .multiAccounts(AccountIntegrityMetricsDto.MultiAccountMetricsDto.builder()
                        .devicesWithMultipleUsers(10L)
                        .suspectedMultiAccountUsers(8L)
                        .build())
                .build();
    }

    private ReferralFraudMetricsDto createMockReferralMetrics() {
        return ReferralFraudMetricsDto.builder()
                .totalReferrals(500L)
                .completedReferrals(450L)
                .suspiciousReferrals(25L)
                .fraudulentReferralRate(5.0)
                .build();
    }

    private SecurityLogMetricsDto createMockSecurityMetrics() {
        return SecurityLogMetricsDto.builder()
                .loginMetrics(SecurityLogMetricsDto.LoginMetricsDto.builder()
                        .totalLogins(10000L)
                        .failedLogins(200L)
                        .failedLoginRate(2.0)
                        .build())
                .ipSecurityMetrics(SecurityLogMetricsDto.IpSecurityMetricsDto.builder()
                        .suspiciousIpCount(15L)
                        .vpnUsageCount(50L)
                        .torUsageCount(5L)
                        .build())
                .bruteForceMetrics(SecurityLogMetricsDto.BruteForceMetricsDto.builder()
                        .detectedAttacks(2L)
                        .blockedAttempts(150L)
                        .build())
                .build();
    }
}

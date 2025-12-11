package com.fooddelivery.analytics.fraud.integration;

import com.fooddelivery.analytics.fraud.dto.*;
import com.fooddelivery.analytics.fraud.model.*;
import com.fooddelivery.analytics.fraud.repository.*;
import com.fooddelivery.analytics.fraud.service.FraudAnalyticsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Fraud Analytics Integration Tests")
class FraudAnalyticsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("fraud_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private FraudAnalyticsService fraudAnalyticsService;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private AuthLogRepository authLogRepository;

    @Autowired
    private ReferralEventRepository referralEventRepository;

    @Autowired
    private PromoUsageLogRepository promoUsageLogRepository;

    @Autowired
    private FraudOrderHistoryRepository orderHistoryRepository;

    @Autowired
    private DeviceFingerprintRepository deviceFingerprintRepository;

    @Autowired
    private SecurityFlagRepository securityFlagRepository;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.now().minusDays(7);
        endDate = LocalDateTime.now();
    }

    @Nested
    @DisplayName("Payment Fraud Detection Tests")
    class PaymentFraudTests {

        @Test
        @Order(1)
        @DisplayName("Should detect high payment failure rate")
        void shouldDetectHighPaymentFailureRate() {
            // Arrange - Create user with high failure rate
            Long userId = 1001L;
            createPaymentAttempts(userId, 10, 8); // 10 total, 8 failed = 80% failure rate

            // Act
            PaymentFraudMetricsDto metrics = fraudAnalyticsService.getPaymentFraudMetrics(
                    startDate, endDate, 3, 60, true, 100);

            // Assert
            assertThat(metrics).isNotNull();
            assertThat(metrics.getTotalPaymentAttempts()).isGreaterThanOrEqualTo(10L);
            assertThat(metrics.getFailedPayments()).isGreaterThanOrEqualTo(8L);
        }

        @Test
        @Order(2)
        @DisplayName("Should identify card testing patterns")
        void shouldIdentifyCardTestingPatterns() {
            // Arrange - Create rapid small amount transactions
            Long userId = 1002L;
            for (int i = 0; i < 10; i++) {
                PaymentAttempt attempt = PaymentAttempt.builder()
                        .userId(userId)
                        .amount(BigDecimal.valueOf(1.00))
                        .paymentMethod("CARD")
                        .status(PaymentAttempt.PaymentStatus.DECLINED)
                        .failureReason("CARD_DECLINED")
                        .createdAt(LocalDateTime.now().minusMinutes(i))
                        .build();
                paymentAttemptRepository.save(attempt);
            }

            // Act
            PaymentFraudMetricsDto metrics = fraudAnalyticsService.getPaymentFraudMetrics(
                    startDate, endDate, 3, 15, true, 100);

            // Assert
            assertThat(metrics.getHighRiskUsersList()).isNotEmpty();
        }

        private void createPaymentAttempts(Long userId, int total, int failed) {
            for (int i = 0; i < total; i++) {
                PaymentAttempt attempt = PaymentAttempt.builder()
                        .userId(userId)
                        .amount(BigDecimal.valueOf(25.00 + i))
                        .paymentMethod("CARD")
                        .status(i < failed ? PaymentAttempt.PaymentStatus.FAILED : PaymentAttempt.PaymentStatus.SUCCESS)
                        .failureReason(i < failed ? "INSUFFICIENT_FUNDS" : null)
                        .createdAt(LocalDateTime.now().minusHours(i))
                        .build();
                paymentAttemptRepository.save(attempt);
            }
        }
    }

    @Nested
    @DisplayName("Behavioral Fraud Detection Tests")
    class BehavioralFraudTests {

        @Test
        @Order(3)
        @DisplayName("Should detect velocity violations")
        void shouldDetectVelocityViolations() {
            // Arrange - Create user with high order velocity
            Long userId = 2001L;
            for (int i = 0; i < 15; i++) {
                FraudOrderHistory order = FraudOrderHistory.builder()
                        .orderId((long) (3000 + i))
                        .userId(userId)
                        .restaurantId(100L)
                        .orderTotal(BigDecimal.valueOf(30.00))
                        .createdAt(LocalDateTime.now().minusMinutes(i * 5)) // 15 orders in 75 minutes
                        .build();
                orderHistoryRepository.save(order);
            }

            // Act
            BehavioralFraudMetricsDto metrics = fraudAnalyticsService.getBehavioralFraudMetrics(
                    startDate, endDate, 5, 30.0, 3.0, 3, true, 100);

            // Assert
            assertThat(metrics).isNotNull();
            assertThat(metrics.getVelocityMetrics()).isNotNull();
        }

        @Test
        @Order(4)
        @DisplayName("Should detect refund abuse")
        void shouldDetectRefundAbuse() {
            // Arrange - Create user with high refund rate
            Long userId = 2002L;
            for (int i = 0; i < 10; i++) {
                FraudOrderHistory order = FraudOrderHistory.builder()
                        .orderId((long) (4000 + i))
                        .userId(userId)
                        .restaurantId(101L)
                        .orderTotal(BigDecimal.valueOf(40.00))
                        .isRefunded(i < 7) // 7 out of 10 refunded = 70%
                        .refundAmount(i < 7 ? BigDecimal.valueOf(40.00) : BigDecimal.ZERO)
                        .refundReason(i < 7 ? "CUSTOMER_REQUEST" : null)
                        .createdAt(LocalDateTime.now().minusDays(i))
                        .build();
                orderHistoryRepository.save(order);
            }

            // Act
            BehavioralFraudMetricsDto metrics = fraudAnalyticsService.getBehavioralFraudMetrics(
                    startDate, endDate, 5, 30.0, 3.0, 3, true, 100);

            // Assert
            assertThat(metrics.getRefundMetrics()).isNotNull();
            assertThat(metrics.getRefundMetrics().getHighRefundUsers()).isGreaterThanOrEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Account Integrity Tests")
    class AccountIntegrityTests {

        @Test
        @Order(5)
        @DisplayName("Should detect device sharing across users")
        void shouldDetectDeviceSharing() {
            // Arrange - Create multiple users on same device
            String sharedDeviceId = "SHARED-DEVICE-001";
            for (long userId = 3001L; userId <= 3005L; userId++) {
                DeviceFingerprint device = DeviceFingerprint.builder()
                        .deviceId(sharedDeviceId)
                        .userId(userId)
                        .deviceType(DeviceFingerprint.DeviceType.MOBILE_ANDROID)
                        .trustScore(80)
                        .firstSeenAt(LocalDateTime.now().minusDays(1))
                        .lastSeenAt(LocalDateTime.now())
                        .build();
                deviceFingerprintRepository.save(device);
            }

            // Act
            AccountIntegrityMetricsDto metrics = fraudAnalyticsService.getAccountIntegrityMetrics(
                    startDate, endDate, 24, 2, 5, true, 100);

            // Assert
            assertThat(metrics).isNotNull();
            assertThat(metrics.getMultiAccounts()).isNotNull();
            assertThat(metrics.getMultiAccounts().getDevicesWithMultipleUsers()).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @Order(6)
        @DisplayName("Should detect suspicious devices")
        void shouldDetectSuspiciousDevices() {
            // Arrange - Create emulator and rooted devices
            DeviceFingerprint emulator = DeviceFingerprint.builder()
                    .deviceId("EMULATOR-001")
                    .userId(3010L)
                    .deviceType(DeviceFingerprint.DeviceType.MOBILE_ANDROID)
                    .isEmulator(true)
                    .trustScore(20)
                    .firstSeenAt(LocalDateTime.now().minusHours(2))
                    .lastSeenAt(LocalDateTime.now())
                    .build();
            deviceFingerprintRepository.save(emulator);

            DeviceFingerprint rooted = DeviceFingerprint.builder()
                    .deviceId("ROOTED-001")
                    .userId(3011L)
                    .deviceType(DeviceFingerprint.DeviceType.MOBILE_ANDROID)
                    .isRooted(true)
                    .trustScore(30)
                    .firstSeenAt(LocalDateTime.now().minusHours(1))
                    .lastSeenAt(LocalDateTime.now())
                    .build();
            deviceFingerprintRepository.save(rooted);

            // Act
            AccountIntegrityMetricsDto metrics = fraudAnalyticsService.getAccountIntegrityMetrics(
                    startDate, endDate, 24, 2, 5, true, 100);

            // Assert
            assertThat(metrics.getDeviceIntegrity()).isNotNull();
            assertThat(metrics.getDeviceIntegrity().getSuspiciousDevices()).isGreaterThanOrEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Security Log Tests")
    class SecurityLogTests {

        @Test
        @Order(7)
        @DisplayName("Should detect brute force login attempts")
        void shouldDetectBruteForceAttempts() {
            // Arrange - Create rapid failed login attempts
            Long targetUserId = 4001L;
            String attackerIp = "192.168.1.100";
            for (int i = 0; i < 20; i++) {
                AuthLog log = AuthLog.builder()
                        .userId(targetUserId)
                        .authType(AuthLog.AuthType.LOGIN)
                        .status(AuthLog.AuthStatus.FAILED)
                        .ipAddress(attackerIp)
                        .failureReason("INVALID_PASSWORD")
                        .createdAt(LocalDateTime.now().minusMinutes(i))
                        .build();
                authLogRepository.save(log);
            }

            // Act
            SecurityLogMetricsDto metrics = fraudAnalyticsService.getSecurityLogMetrics(
                    startDate, endDate, 5, 15, 100, true, 100);

            // Assert
            assertThat(metrics).isNotNull();
            assertThat(metrics.getLoginMetrics()).isNotNull();
            assertThat(metrics.getLoginMetrics().getFailedLogins()).isGreaterThanOrEqualTo(20L);
        }

        @Test
        @Order(8)
        @DisplayName("Should detect suspicious IP patterns")
        void shouldDetectSuspiciousIpPatterns() {
            // Arrange - Create logins from VPN and TOR
            AuthLog vpnLogin = AuthLog.builder()
                    .userId(4002L)
                    .authType(AuthLog.AuthType.LOGIN)
                    .status(AuthLog.AuthStatus.SUCCESS)
                    .ipAddress("10.0.0.1")
                    .isVpn(true)
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .build();
            authLogRepository.save(vpnLogin);

            AuthLog torLogin = AuthLog.builder()
                    .userId(4003L)
                    .authType(AuthLog.AuthType.LOGIN)
                    .status(AuthLog.AuthStatus.SUCCESS)
                    .ipAddress("10.0.0.2")
                    .isTor(true)
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build();
            authLogRepository.save(torLogin);

            // Act
            SecurityLogMetricsDto metrics = fraudAnalyticsService.getSecurityLogMetrics(
                    startDate, endDate, 5, 15, 100, true, 100);

            // Assert
            assertThat(metrics.getIpSecurityMetrics()).isNotNull();
            assertThat(metrics.getIpSecurityMetrics().getVpnUsageCount()).isGreaterThanOrEqualTo(1L);
            assertThat(metrics.getIpSecurityMetrics().getTorUsageCount()).isGreaterThanOrEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Referral Fraud Tests")
    class ReferralFraudTests {

        @Test
        @Order(9)
        @DisplayName("Should detect device-based referral fraud")
        void shouldDetectDeviceBasedReferralFraud() {
            // Arrange - Create multiple referrals from same device
            String fraudDevice = "FRAUD-DEVICE-001";
            Long referrerId = 5001L;
            for (long referredId = 5002L; referredId <= 5010L; referredId++) {
                ReferralEvent event = ReferralEvent.builder()
                        .referrerId(referrerId)
                        .referredUserId(referredId)
                        .referralCode("REF-" + referrerId)
                        .signupDeviceId(fraudDevice)
                        .signupIpAddress("192.168.1." + (referredId % 256))
                        .rewardEarned(BigDecimal.valueOf(5.00))
                        .rewardType(ReferralEvent.RewardType.CREDIT)
                        .createdAt(LocalDateTime.now().minusDays((int)(referredId - 5002)))
                        .build();
                referralEventRepository.save(event);
            }

            // Act
            ReferralFraudMetricsDto metrics = fraudAnalyticsService.getReferralFraudMetrics(
                    startDate, endDate, 2, 5, 80.0, true, 100);

            // Assert
            assertThat(metrics).isNotNull();
            assertThat(metrics.getDeviceFraudMetrics()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Fraud Summary Tests")
    class FraudSummaryTests {

        @Test
        @Order(10)
        @DisplayName("Should generate comprehensive fraud summary")
        void shouldGenerateComprehensiveFraudSummary() {
            // Act
            FraudMetricsRequest request = FraudMetricsRequest.createDefault(startDate, endDate);
            FraudSummaryDto summary = fraudAnalyticsService.getFraudSummary(request);

            // Assert
            assertThat(summary).isNotNull();
            assertThat(summary.getOverallRiskScore()).isNotNull();
            assertThat(summary.getRiskLevel()).isNotNull();
            assertThat(summary.getCategorySummaries()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("User Risk Score Tests")
    class UserRiskScoreTests {

        @Test
        @Order(11)
        @DisplayName("Should calculate user risk score")
        void shouldCalculateUserRiskScore() {
            // Arrange - Create test user with some activity
            Long userId = 6001L;

            // Add payment attempts
            PaymentAttempt payment = PaymentAttempt.builder()
                    .userId(userId)
                    .amount(BigDecimal.valueOf(50.00))
                    .paymentMethod("CARD")
                    .status(PaymentAttempt.PaymentStatus.SUCCESS)
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .build();
            paymentAttemptRepository.save(payment);

            // Add login
            AuthLog login = AuthLog.builder()
                    .userId(userId)
                    .authType(AuthLog.AuthType.LOGIN)
                    .status(AuthLog.AuthStatus.SUCCESS)
                    .ipAddress("192.168.1.50")
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build();
            authLogRepository.save(login);

            // Act
            UserFraudRiskDto userRisk = fraudAnalyticsService.getUserRiskScore(userId, startDate, endDate);

            // Assert
            assertThat(userRisk).isNotNull();
            assertThat(userRisk.getUserId()).isEqualTo(userId);
            assertThat(userRisk.getOverallRiskScore()).isNotNull();
            assertThat(userRisk.getRiskLevel()).isNotNull();
        }
    }

    @AfterAll
    static void tearDown() {
        // Clean up containers
        postgres.stop();
        redis.stop();
    }
}

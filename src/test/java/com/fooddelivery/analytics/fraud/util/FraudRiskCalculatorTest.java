package com.fooddelivery.analytics.fraud.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FraudRiskCalculator Unit Tests")
class FraudRiskCalculatorTest {

    private FraudRiskCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FraudRiskCalculator();
    }

    @Nested
    @DisplayName("Payment Risk Score Calculations")
    class PaymentRiskScoreTests {

        @Test
        @DisplayName("Should return 0 for no payment attempts")
        void shouldReturnZeroForNoAttempts() {
            int score = calculator.calculatePaymentRiskScore(0, 0.0, 0);
            assertThat(score).isZero();
        }

        @Test
        @DisplayName("Should return low score for low failure rate")
        void shouldReturnLowScoreForLowFailureRate() {
            int score = calculator.calculatePaymentRiskScore(1, 10.0, 10);
            assertThat(score).isLessThanOrEqualTo(30);
        }

        @Test
        @DisplayName("Should return high score for high failure rate")
        void shouldReturnHighScoreForHighFailureRate() {
            int score = calculator.calculatePaymentRiskScore(10, 80.0, 12);
            assertThat(score).isGreaterThanOrEqualTo(60);
        }

        @ParameterizedTest
        @CsvSource({
                "5, 50.0, 10, 50",
                "10, 80.0, 15, 80",
                "3, 30.0, 10, 30"
        })
        @DisplayName("Should calculate payment risk scores correctly")
        void shouldCalculatePaymentRiskCorrectly(int failedCount, double failureRate,
                                                   int totalCount, int minExpectedScore) {
            int score = calculator.calculatePaymentRiskScore(failedCount, failureRate, totalCount);
            assertThat(score).isGreaterThanOrEqualTo(minExpectedScore - 20);
            assertThat(score).isLessThanOrEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Refund Risk Score Calculations")
    class RefundRiskScoreTests {

        @Test
        @DisplayName("Should return 0 for no refunds")
        void shouldReturnZeroForNoRefunds() {
            int score = calculator.calculateRefundRiskScore(0.0, 0);
            assertThat(score).isZero();
        }

        @Test
        @DisplayName("Should return high score for high refund rate")
        void shouldReturnHighScoreForHighRefundRate() {
            int score = calculator.calculateRefundRiskScore(60.0, 15);
            assertThat(score).isGreaterThanOrEqualTo(70);
        }

        @ParameterizedTest
        @CsvSource({
                "10.0, 2, 10",
                "30.0, 5, 35",
                "50.0, 10, 80"
        })
        @DisplayName("Should calculate refund risk scores correctly")
        void shouldCalculateRefundRiskCorrectly(double refundRate, long refundCount, int minExpected) {
            int score = calculator.calculateRefundRiskScore(refundRate, refundCount);
            assertThat(score).isGreaterThanOrEqualTo(minExpected);
        }
    }

    @Nested
    @DisplayName("Fake Account Risk Score Calculations")
    class FakeAccountRiskScoreTests {

        @Test
        @DisplayName("Should return very high score for very new account with promo")
        void shouldReturnVeryHighScoreForVeryNewAccountWithPromo() {
            int score = calculator.calculateFakeAccountRiskScore(0, true);
            assertThat(score).isEqualTo(100);
        }

        @Test
        @DisplayName("Should return low score for old account without promo")
        void shouldReturnLowScoreForOldAccount() {
            int score = calculator.calculateFakeAccountRiskScore(72, false);
            assertThat(score).isZero();
        }

        @ParameterizedTest
        @CsvSource({
                "0, true, 100",
                "0, false, 60",
                "12, true, 60",
                "48, false, 0"
        })
        @DisplayName("Should calculate fake account risk correctly")
        void shouldCalculateFakeAccountRiskCorrectly(int ageHours, boolean hasPromo, int expectedScore) {
            int score = calculator.calculateFakeAccountRiskScore(ageHours, hasPromo);
            assertThat(score).isEqualTo(expectedScore);
        }
    }

    @Nested
    @DisplayName("Multi-Account Risk Score Calculations")
    class MultiAccountRiskScoreTests {

        @Test
        @DisplayName("Should return 0 for single user")
        void shouldReturnZeroForSingleUser() {
            int score = calculator.calculateMultiAccountRiskScore(1, 5);
            assertThat(score).isZero();
        }

        @Test
        @DisplayName("Should return high score for many users sharing device")
        void shouldReturnHighScoreForManyUsers() {
            int score = calculator.calculateMultiAccountRiskScore(10, 50);
            assertThat(score).isEqualTo(100);
        }

        @ParameterizedTest
        @CsvSource({
                "2, 5, 15",
                "5, 20, 70",
                "10, 100, 100"
        })
        @DisplayName("Should calculate multi-account risk correctly")
        void shouldCalculateMultiAccountRiskCorrectly(int userCount, long orderCount, int minExpected) {
            int score = calculator.calculateMultiAccountRiskScore(userCount, orderCount);
            assertThat(score).isGreaterThanOrEqualTo(minExpected);
        }
    }

    @Nested
    @DisplayName("Login Risk Score Calculations")
    class LoginRiskScoreTests {

        @Test
        @DisplayName("Should return 0 for no failed logins")
        void shouldReturnZeroForNoFailedLogins() {
            int score = calculator.calculateLoginRiskScore(0, 0.0);
            assertThat(score).isZero();
        }

        @Test
        @DisplayName("Should return high score for many failed logins")
        void shouldReturnHighScoreForManyFailedLogins() {
            int score = calculator.calculateLoginRiskScore(25, 90.0);
            assertThat(score).isGreaterThanOrEqualTo(80);
        }
    }

    @Nested
    @DisplayName("IP Threat Score Calculations")
    class IpThreatScoreTests {

        @Test
        @DisplayName("Should return highest score for TOR exit node")
        void shouldReturnHighestScoreForTor() {
            int score = calculator.calculateIpThreatScore(false, true, false, 20, 10);
            assertThat(score).isGreaterThanOrEqualTo(80);
        }

        @Test
        @DisplayName("Should return moderate score for VPN")
        void shouldReturnModerateScoreForVpn() {
            int score = calculator.calculateIpThreatScore(true, false, false, 5, 2);
            assertThat(score).isBetween(20, 50);
        }

        @Test
        @DisplayName("Should return 0 for clean IP")
        void shouldReturnZeroForCleanIp() {
            int score = calculator.calculateIpThreatScore(false, false, false, 0, 1);
            assertThat(score).isZero();
        }
    }

    @Nested
    @DisplayName("Brute Force Risk Score Calculations")
    class BruteForceRiskScoreTests {

        @Test
        @DisplayName("Should detect brute force attack pattern")
        void shouldDetectBruteForcePattern() {
            int score = calculator.calculateBruteForceRiskScore(100, 5, 50);
            assertThat(score).isGreaterThanOrEqualTo(80);
        }

        @Test
        @DisplayName("Should return low score for normal login pattern")
        void shouldReturnLowScoreForNormalPattern() {
            int score = calculator.calculateBruteForceRiskScore(5, 60, 1);
            assertThat(score).isLessThanOrEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Overall Risk Score Calculations")
    class OverallRiskScoreTests {

        @Test
        @DisplayName("Should calculate weighted overall score")
        void shouldCalculateWeightedOverallScore() {
            int score = calculator.calculateOverallRiskScore(50, 50, 50, 50, 50);
            assertThat(score).isEqualTo(50);
        }

        @Test
        @DisplayName("Should weight payment fraud higher")
        void shouldWeightPaymentFraudHigher() {
            // Payment fraud has 0.30 weight, so 100 * 0.30 = 30 for payment alone
            int scoreWithHighPayment = calculator.calculateOverallRiskScore(100, 0, 0, 0, 0);
            int scoreWithHighReferral = calculator.calculateOverallRiskScore(0, 0, 0, 100, 0);

            assertThat(scoreWithHighPayment).isGreaterThan(scoreWithHighReferral);
        }

        @Test
        @DisplayName("Should return 0 when all scores are 0")
        void shouldReturnZeroWhenAllScoresAreZero() {
            int score = calculator.calculateOverallRiskScore(0, 0, 0, 0, 0);
            assertThat(score).isZero();
        }

        @Test
        @DisplayName("Should cap at 100")
        void shouldCapAtHundred() {
            int score = calculator.calculateOverallRiskScore(100, 100, 100, 100, 100);
            assertThat(score).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Risk Level Classification")
    class RiskLevelTests {

        @ParameterizedTest
        @CsvSource({
                "0, LOW",
                "15, LOW",
                "30, LOW",
                "31, MEDIUM",
                "45, MEDIUM",
                "60, MEDIUM",
                "61, HIGH",
                "75, HIGH",
                "80, HIGH",
                "81, CRITICAL",
                "90, CRITICAL",
                "100, CRITICAL"
        })
        @DisplayName("Should classify risk levels correctly")
        void shouldClassifyRiskLevelsCorrectly(int score, String expectedLevel) {
            String level = calculator.getRiskLevel(score);
            assertThat(level).isEqualTo(expectedLevel);
        }

        @Test
        @DisplayName("Should identify high risk correctly")
        void shouldIdentifyHighRisk() {
            assertThat(calculator.isHighRisk(61)).isTrue();
            assertThat(calculator.isHighRisk(60)).isFalse();
        }

        @Test
        @DisplayName("Should identify critical risk correctly")
        void shouldIdentifyCriticalRisk() {
            assertThat(calculator.isCriticalRisk(81)).isTrue();
            assertThat(calculator.isCriticalRisk(80)).isFalse();
        }
    }

    @Nested
    @DisplayName("Device Risk Score Calculations")
    class DeviceRiskScoreTests {

        @Test
        @DisplayName("Should return high score for emulator with low trust")
        void shouldReturnHighScoreForEmulator() {
            int score = calculator.calculateDeviceRiskScore(30, true, false);
            assertThat(score).isGreaterThanOrEqualTo(50);
        }

        @Test
        @DisplayName("Should return very high score for rooted emulator with low trust")
        void shouldReturnVeryHighScoreForRootedEmulator() {
            int score = calculator.calculateDeviceRiskScore(20, true, true);
            assertThat(score).isGreaterThanOrEqualTo(80);
        }

        @Test
        @DisplayName("Should return low score for high trust device")
        void shouldReturnLowScoreForHighTrustDevice() {
            int score = calculator.calculateDeviceRiskScore(95, false, false);
            assertThat(score).isLessThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Circular Referral Risk Score Calculations")
    class CircularReferralRiskScoreTests {

        @Test
        @DisplayName("Should return high base score for any circular referral")
        void shouldReturnHighBaseScore() {
            int score = calculator.calculateCircularReferralRiskScore(2, 2);
            assertThat(score).isGreaterThanOrEqualTo(70);
        }

        @Test
        @DisplayName("Should increase score for longer chains")
        void shouldIncreaseScoreForLongerChains() {
            int shortChain = calculator.calculateCircularReferralRiskScore(2, 2);
            int longChain = calculator.calculateCircularReferralRiskScore(6, 6);

            assertThat(longChain).isGreaterThan(shortChain);
        }
    }

    @Nested
    @DisplayName("Promo Abuse Risk Score Calculations")
    class PromoAbuseRiskScoreTests {

        @Test
        @DisplayName("Should return maximum score for promo-only user")
        void shouldReturnMaxScoreForPromoOnlyUser() {
            int score = calculator.calculatePromoAbuseRiskScore(100.0, true);
            assertThat(score).isEqualTo(100);
        }

        @Test
        @DisplayName("Should return 0 for low promo usage with paid orders")
        void shouldReturnZeroForLowPromoUsage() {
            int score = calculator.calculatePromoAbuseRiskScore(20.0, false);
            assertThat(score).isZero();
        }

        @ParameterizedTest
        @CsvSource({
                "90.0, true, 100",
                "90.0, false, 60",
                "50.0, true, 65",
                "30.0, false, 10"
        })
        @DisplayName("Should calculate promo abuse risk correctly")
        void shouldCalculatePromoAbuseRiskCorrectly(double promoRate, boolean noPaidOrders, int expectedScore) {
            int score = calculator.calculatePromoAbuseRiskScore(promoRate, noPaidOrders);
            assertThat(score).isEqualTo(expectedScore);
        }
    }
}

package com.fooddelivery.admin.dashboard.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Dashboard Metrics Calculator Tests")
class DashboardMetricsCalculatorTest {

    @Nested
    @DisplayName("Percentage Calculation Tests")
    class PercentageTests {

        @Test
        @DisplayName("Should calculate percentage correctly")
        void shouldCalculatePercentage() {
            Double result = DashboardMetricsCalculator.calculatePercentage(75L, 100L);
            assertThat(result).isEqualTo(75.0);
        }

        @Test
        @DisplayName("Should return 0 for null total")
        void shouldReturnZeroForNullTotal() {
            Double result = DashboardMetricsCalculator.calculatePercentage(50L, null);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0 for zero total")
        void shouldReturnZeroForZeroTotal() {
            Double result = DashboardMetricsCalculator.calculatePercentage(50L, 0L);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate percentage with BigDecimal precision")
        void shouldCalculatePercentagePrecise() {
            BigDecimal result = DashboardMetricsCalculator.calculatePercentagePrecise(
                    BigDecimal.valueOf(333),
                    BigDecimal.valueOf(1000));
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(33.30));
        }
    }

    @Nested
    @DisplayName("Percentage Change Tests")
    class PercentageChangeTests {

        @Test
        @DisplayName("Should calculate positive percentage change")
        void shouldCalculatePositiveChange() {
            Double result = DashboardMetricsCalculator.calculatePercentageChange(100.0, 150.0);
            assertThat(result).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should calculate negative percentage change")
        void shouldCalculateNegativeChange() {
            Double result = DashboardMetricsCalculator.calculatePercentageChange(100.0, 75.0);
            assertThat(result).isEqualTo(-25.0);
        }

        @Test
        @DisplayName("Should return 100% when previous is 0 and current is positive")
        void shouldReturn100WhenPreviousIsZero() {
            Double result = DashboardMetricsCalculator.calculatePercentageChange(0.0, 50.0);
            assertThat(result).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should return 0% when both are 0")
        void shouldReturnZeroWhenBothZero() {
            Double result = DashboardMetricsCalculator.calculatePercentageChange(0.0, 0.0);
            assertThat(result).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Rounding Tests")
    class RoundingTests {

        @ParameterizedTest
        @CsvSource({
                "3.14159, 3.14",
                "2.5, 2.5",
                "99.999, 100.0",
                "0.001, 0.0",
                "45.675, 45.68"
        })
        @DisplayName("Should round to two decimals correctly")
        void shouldRoundToTwoDecimals(double input, double expected) {
            Double result = DashboardMetricsCalculator.roundToTwoDecimals(input);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return 0 for null value")
        void shouldReturnZeroForNull() {
            Double result = DashboardMetricsCalculator.roundToTwoDecimals(null);
            assertThat(result).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Duration Calculation Tests")
    class DurationTests {

        @Test
        @DisplayName("Should calculate duration in minutes")
        void shouldCalculateDurationMinutes() {
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 1, 10, 30);

            Long result = DashboardMetricsCalculator.calculateDurationMinutes(start, end);
            assertThat(result).isEqualTo(30L);
        }

        @Test
        @DisplayName("Should calculate duration in seconds")
        void shouldCalculateDurationSeconds() {
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 1, 10, 0, 45);

            Long result = DashboardMetricsCalculator.calculateDurationSeconds(start, end);
            assertThat(result).isEqualTo(45L);
        }

        @Test
        @DisplayName("Should return 0 for null timestamps")
        void shouldReturnZeroForNullTimestamps() {
            Long result = DashboardMetricsCalculator.calculateDurationMinutes(null, LocalDateTime.now());
            assertThat(result).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Duration Formatting Tests")
    class DurationFormattingTests {

        @ParameterizedTest
        @CsvSource({
                "0, 0m",
                "30, 30m",
                "60, 1h 0m",
                "90, 1h 30m",
                "1440, 1d 0h 0m",
                "1500, 1d 1h 0m"
        })
        @DisplayName("Should format duration correctly")
        void shouldFormatDuration(long minutes, String expected) {
            String result = DashboardMetricsCalculator.formatDuration(minutes);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should format null as 0m")
        void shouldFormatNullAsZero() {
            String result = DashboardMetricsCalculator.formatDuration((Long) null);
            assertThat(result).isEqualTo("0m");
        }
    }

    @Nested
    @DisplayName("Trend Determination Tests")
    class TrendTests {

        @ParameterizedTest
        @CsvSource({
                "10.0, UP",
                "-10.0, DOWN",
                "3.0, STABLE",
                "-3.0, STABLE",
                "0.0, STABLE"
        })
        @DisplayName("Should determine trend correctly")
        void shouldDetermineTrend(double change, String expected) {
            String result = DashboardMetricsCalculator.determineTrend(change);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return STABLE for null")
        void shouldReturnStableForNull() {
            String result = DashboardMetricsCalculator.determineTrend(null);
            assertThat(result).isEqualTo("STABLE");
        }
    }

    @Nested
    @DisplayName("Average Calculation Tests")
    class AverageTests {

        @Test
        @DisplayName("Should calculate average correctly")
        void shouldCalculateAverage() {
            List<Double> values = Arrays.asList(10.0, 20.0, 30.0, 40.0);
            Double result = DashboardMetricsCalculator.calculateAverage(values);
            assertThat(result).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Should return 0 for empty list")
        void shouldReturnZeroForEmptyList() {
            Double result = DashboardMetricsCalculator.calculateAverage(List.of());
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle null values in list")
        void shouldHandleNullValuesInList() {
            List<Double> values = Arrays.asList(10.0, null, 30.0);
            Double result = DashboardMetricsCalculator.calculateAverage(values);
            assertThat(result).isEqualTo(20.0);
        }
    }

    @Nested
    @DisplayName("Weighted Average Tests")
    class WeightedAverageTests {

        @Test
        @DisplayName("Should calculate weighted average correctly")
        void shouldCalculateWeightedAverage() {
            Map<Double, Double> valuesAndWeights = Map.of(
                    80.0, 3.0,  // 80 with weight 3
                    90.0, 2.0   // 90 with weight 2
            );
            Double result = DashboardMetricsCalculator.calculateWeightedAverage(valuesAndWeights);
            assertThat(result).isEqualTo(84.0); // (80*3 + 90*2) / 5 = 84
        }

        @Test
        @DisplayName("Should return 0 for empty map")
        void shouldReturnZeroForEmptyMap() {
            Double result = DashboardMetricsCalculator.calculateWeightedAverage(Map.of());
            assertThat(result).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Normalization Tests")
    class NormalizationTests {

        @Test
        @DisplayName("Should normalize value to 0-100 scale")
        void shouldNormalizeValue() {
            Double result = DashboardMetricsCalculator.normalizeToScale(50.0, 0.0, 100.0);
            assertThat(result).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should handle value at minimum")
        void shouldHandleMinValue() {
            Double result = DashboardMetricsCalculator.normalizeToScale(0.0, 0.0, 100.0);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle value at maximum")
        void shouldHandleMaxValue() {
            Double result = DashboardMetricsCalculator.normalizeToScale(100.0, 0.0, 100.0);
            assertThat(result).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should cap value above range")
        void shouldCapValueAboveRange() {
            Double result = DashboardMetricsCalculator.normalizeToScale(150.0, 0.0, 100.0);
            assertThat(result).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("Safe Division Tests")
    class SafeDivisionTests {

        @Test
        @DisplayName("Should divide correctly")
        void shouldDivideCorrectly() {
            Double result = DashboardMetricsCalculator.safeDivide(100.0, 4.0);
            assertThat(result).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Should return 0 for division by zero")
        void shouldReturnZeroForDivisionByZero() {
            Double result = DashboardMetricsCalculator.safeDivide(100.0, 0.0);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0 for null divisor")
        void shouldReturnZeroForNullDivisor() {
            Double result = DashboardMetricsCalculator.safeDivide(100.0, null);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle BigDecimal safe division")
        void shouldHandleBigDecimalSafeDivision() {
            BigDecimal result = DashboardMetricsCalculator.safeDivide(
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(3));
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(33.33));
        }
    }
}

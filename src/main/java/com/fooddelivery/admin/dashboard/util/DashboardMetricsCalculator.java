package com.fooddelivery.admin.dashboard.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;

/**
 * Utility class for calculating dashboard metrics.
 */
@UtilityClass
public class DashboardMetricsCalculator {

    private static final int DEFAULT_SCALE = 2;

    /**
     * Calculate percentage with null safety.
     *
     * @param part the numerator
     * @param total the denominator
     * @return percentage value (0-100)
     */
    public static Double calculatePercentage(Long part, Long total) {
        if (total == null || total == 0 || part == null) {
            return 0.0;
        }
        return roundToDecimals((part.doubleValue() / total.doubleValue()) * 100, DEFAULT_SCALE);
    }

    /**
     * Calculate percentage with BigDecimal precision.
     *
     * @param part the numerator
     * @param total the denominator
     * @return percentage as BigDecimal
     */
    public static BigDecimal calculatePercentagePrecise(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0 || part == null) {
            return BigDecimal.ZERO;
        }
        return part.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate percentage change between two values.
     *
     * @param previous the previous value
     * @param current the current value
     * @return percentage change
     */
    public static Double calculatePercentageChange(Double previous, Double current) {
        if (previous == null || previous == 0) {
            return current != null && current > 0 ? 100.0 : 0.0;
        }
        return roundToDecimals(((current - previous) / previous) * 100, DEFAULT_SCALE);
    }

    /**
     * Calculate percentage change between two BigDecimal values.
     *
     * @param previous the previous value
     * @param current the current value
     * @return percentage change
     */
    public static Double calculatePercentageChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(DEFAULT_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Round a double value to specified decimal places.
     *
     * @param value the value to round
     * @param decimals number of decimal places
     * @return rounded value
     */
    public static Double roundToDecimals(Double value, int decimals) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
                .setScale(decimals, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Round a double value to 2 decimal places.
     *
     * @param value the value to round
     * @return rounded value
     */
    public static Double roundToTwoDecimals(Double value) {
        return roundToDecimals(value, DEFAULT_SCALE);
    }

    /**
     * Calculate average from a collection of numbers.
     *
     * @param values collection of values
     * @return average value
     */
    public static Double calculateAverage(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double sum = values.stream()
                .filter(v -> v != null)
                .mapToDouble(Number::doubleValue)
                .sum();
        long count = values.stream().filter(v -> v != null).count();
        return count > 0 ? roundToDecimals(sum / count, DEFAULT_SCALE) : 0.0;
    }

    /**
     * Calculate duration in minutes between two timestamps.
     *
     * @param start start time
     * @param end end time
     * @return duration in minutes
     */
    public static Long calculateDurationMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * Calculate duration in seconds between two timestamps.
     *
     * @param start start time
     * @param end end time
     * @return duration in seconds
     */
    public static Long calculateDurationSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * Format duration in minutes to human-readable string.
     *
     * @param minutes duration in minutes
     * @return formatted string (e.g., "2h 30m", "1d 5h 15m")
     */
    public static String formatDuration(Long minutes) {
        if (minutes == null || minutes <= 0) {
            return "0m";
        }

        long days = minutes / (24 * 60);
        long hours = (minutes % (24 * 60)) / 60;
        long mins = minutes % 60;

        // Hierarchical format: show the largest non-zero unit and every smaller
        // unit down to minutes (e.g. 1440 -> "1d 0h 0m", 60 -> "1h 0m"). This keeps
        // the minutes component visible whenever a larger unit is present.
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ").append(hours).append("h ").append(mins).append("m");
        } else if (hours > 0) {
            sb.append(hours).append("h ").append(mins).append("m");
        } else {
            sb.append(mins).append("m");
        }

        return sb.toString();
    }

    /**
     * Format duration in minutes to human-readable string.
     *
     * @param minutes duration in minutes (as Double)
     * @return formatted string
     */
    public static String formatDuration(Double minutes) {
        if (minutes == null) {
            return "0m";
        }
        return formatDuration(Math.round(minutes));
    }

    /**
     * Determine trend based on percentage change.
     *
     * @param percentageChange the percentage change value
     * @return trend string ("UP", "DOWN", "STABLE")
     */
    public static String determineTrend(Double percentageChange) {
        if (percentageChange == null) {
            return "STABLE";
        }
        if (percentageChange > 5) {
            return "UP";
        }
        if (percentageChange < -5) {
            return "DOWN";
        }
        return "STABLE";
    }

    /**
     * Calculate weighted average.
     *
     * @param valuesAndWeights map of values to their weights
     * @return weighted average
     */
    public static Double calculateWeightedAverage(Map<Double, Double> valuesAndWeights) {
        if (valuesAndWeights == null || valuesAndWeights.isEmpty()) {
            return 0.0;
        }

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<Double, Double> entry : valuesAndWeights.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                weightedSum += entry.getKey() * entry.getValue();
                totalWeight += entry.getValue();
            }
        }

        return totalWeight > 0 ? roundToDecimals(weightedSum / totalWeight, DEFAULT_SCALE) : 0.0;
    }

    /**
     * Normalize a value to a 0-100 scale.
     *
     * @param value the value to normalize
     * @param min minimum expected value
     * @param max maximum expected value
     * @return normalized value (0-100)
     */
    public static Double normalizeToScale(Double value, Double min, Double max) {
        if (value == null || min == null || max == null || max.equals(min)) {
            return 0.0;
        }
        double normalized = ((value - min) / (max - min)) * 100;
        return Math.max(0, Math.min(100, roundToDecimals(normalized, DEFAULT_SCALE)));
    }

    /**
     * Calculate rate per hour.
     *
     * @param count the count of events
     * @param durationHours the duration in hours
     * @return rate per hour
     */
    public static Double calculateRatePerHour(Long count, Double durationHours) {
        if (count == null || durationHours == null || durationHours <= 0) {
            return 0.0;
        }
        return roundToDecimals(count / durationHours, DEFAULT_SCALE);
    }

    /**
     * Calculate hours between two timestamps.
     *
     * @param start start time
     * @param end end time
     * @return hours between (as double for partial hours)
     */
    public static Double calculateHoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0.0;
        }
        Duration duration = Duration.between(start, end);
        return roundToDecimals(duration.toMinutes() / 60.0, DEFAULT_SCALE);
    }

    /**
     * Safe division that returns 0 when divisor is 0.
     *
     * @param dividend the dividend
     * @param divisor the divisor
     * @return result of division or 0 if divisor is 0
     */
    public static Double safeDivide(Double dividend, Double divisor) {
        if (dividend == null || divisor == null || divisor == 0) {
            return 0.0;
        }
        return roundToDecimals(dividend / divisor, DEFAULT_SCALE);
    }

    /**
     * Safe division for BigDecimal.
     *
     * @param dividend the dividend
     * @param divisor the divisor
     * @return result of division or ZERO if divisor is 0
     */
    public static BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, DEFAULT_SCALE, RoundingMode.HALF_UP);
    }
}

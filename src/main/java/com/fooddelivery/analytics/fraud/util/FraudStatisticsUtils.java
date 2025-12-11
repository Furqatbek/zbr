package com.fooddelivery.analytics.fraud.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Utility class for statistical calculations in fraud detection.
 * Includes z-score calculations, anomaly detection, and trend analysis.
 */
public final class FraudStatisticsUtils {

    private static final int DEFAULT_SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private FraudStatisticsUtils() {
        // Prevent instantiation
    }

    // ============================================
    // Basic Statistics
    // ============================================

    /**
     * Calculate the mean (average) of a collection of numbers.
     */
    public static double mean(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream()
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate the sum of a collection of numbers.
     */
    public static double sum(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream()
                .mapToDouble(Number::doubleValue)
                .sum();
    }

    /**
     * Find the minimum value in a collection.
     */
    public static double min(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        OptionalDouble result = values.stream()
                .mapToDouble(Number::doubleValue)
                .min();
        return result.orElse(0.0);
    }

    /**
     * Find the maximum value in a collection.
     */
    public static double max(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        OptionalDouble result = values.stream()
                .mapToDouble(Number::doubleValue)
                .max();
        return result.orElse(0.0);
    }

    /**
     * Calculate the variance of a collection of numbers.
     */
    public static double variance(Collection<? extends Number> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        double mean = mean(values);
        return values.stream()
                .mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2))
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate the standard deviation of a collection of numbers.
     */
    public static double standardDeviation(Collection<? extends Number> values) {
        return Math.sqrt(variance(values));
    }

    /**
     * Calculate the median of a collection of numbers.
     */
    public static double median(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = values.stream()
                .map(Number::doubleValue)
                .sorted()
                .collect(Collectors.toList());

        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    // ============================================
    // Z-Score and Anomaly Detection
    // ============================================

    /**
     * Calculate z-score for a single value.
     * Z-score = (value - mean) / stdDev
     *
     * @param value The value to calculate z-score for
     * @param mean The mean of the distribution
     * @param stdDev The standard deviation
     * @return Z-score (number of standard deviations from mean)
     */
    public static double calculateZScore(double value, double mean, double stdDev) {
        if (stdDev == 0) {
            return 0.0;
        }
        return (value - mean) / stdDev;
    }

    /**
     * Calculate z-score for a value against a collection of reference values.
     */
    public static double calculateZScore(double value, Collection<? extends Number> referenceValues) {
        double mean = mean(referenceValues);
        double stdDev = standardDeviation(referenceValues);
        return calculateZScore(value, mean, stdDev);
    }

    /**
     * Check if a value is an anomaly based on z-score threshold.
     *
     * @param value The value to check
     * @param mean The mean of the distribution
     * @param stdDev The standard deviation
     * @param threshold Z-score threshold (typically 2 or 3)
     * @return true if the value is an anomaly
     */
    public static boolean isAnomaly(double value, double mean, double stdDev, double threshold) {
        double zScore = calculateZScore(value, mean, stdDev);
        return Math.abs(zScore) > threshold;
    }

    /**
     * Detect anomalies in a collection based on z-score.
     *
     * @param values Collection of values to analyze
     * @param threshold Z-score threshold
     * @return List of values that are anomalies
     */
    public static List<Double> detectAnomalies(Collection<? extends Number> values, double threshold) {
        double mean = mean(values);
        double stdDev = standardDeviation(values);

        return values.stream()
                .map(Number::doubleValue)
                .filter(v -> isAnomaly(v, mean, stdDev, threshold))
                .collect(Collectors.toList());
    }

    /**
     * Calculate the Interquartile Range (IQR) for outlier detection.
     */
    public static double[] calculateIQR(Collection<? extends Number> values) {
        if (values == null || values.size() < 4) {
            return new double[]{0, 0, 0}; // Q1, Q3, IQR
        }

        List<Double> sorted = values.stream()
                .map(Number::doubleValue)
                .sorted()
                .collect(Collectors.toList());

        int size = sorted.size();
        double q1 = sorted.get(size / 4);
        double q3 = sorted.get(3 * size / 4);
        double iqr = q3 - q1;

        return new double[]{q1, q3, iqr};
    }

    /**
     * Check if a value is an outlier using IQR method.
     * Outlier if: value < Q1 - 1.5*IQR or value > Q3 + 1.5*IQR
     */
    public static boolean isOutlierIQR(double value, Collection<? extends Number> values) {
        double[] iqrData = calculateIQR(values);
        double q1 = iqrData[0];
        double q3 = iqrData[1];
        double iqr = iqrData[2];

        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        return value < lowerBound || value > upperBound;
    }

    // ============================================
    // Rate Calculations
    // ============================================

    /**
     * Calculate percentage rate.
     *
     * @param count The count of specific events
     * @param total The total count
     * @return Percentage (0-100)
     */
    public static double calculateRate(long count, long total) {
        if (total == 0) {
            return 0.0;
        }
        return count * 100.0 / total;
    }

    /**
     * Calculate percentage rate with BigDecimal precision.
     */
    public static BigDecimal calculateRatePrecise(long count, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(count)
                .multiply(HUNDRED)
                .divide(new BigDecimal(total), DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate per-unit rate (e.g., orders per hour).
     */
    public static double calculatePerUnitRate(long count, double units) {
        if (units == 0) {
            return 0.0;
        }
        return count / units;
    }

    // ============================================
    // Trend Analysis
    // ============================================

    /**
     * Calculate simple linear trend from ordered values.
     * Returns slope (positive = increasing, negative = decreasing).
     */
    public static double calculateTrend(List<? extends Number> orderedValues) {
        if (orderedValues == null || orderedValues.size() < 2) {
            return 0.0;
        }

        int n = orderedValues.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = orderedValues.get(i).doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return 0.0;
        }

        return (n * sumXY - sumX * sumY) / denominator;
    }

    /**
     * Calculate percentage change between two values.
     */
    public static double calculatePercentageChange(double oldValue, double newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? 100.0 : 0.0;
        }
        return ((newValue - oldValue) / Math.abs(oldValue)) * 100.0;
    }

    /**
     * Determine trend direction based on percentage change.
     *
     * @param percentChange Percentage change
     * @param threshold Minimum threshold to consider a change significant
     * @return "INCREASING", "DECREASING", or "STABLE"
     */
    public static String getTrendDirection(double percentChange, double threshold) {
        if (percentChange > threshold) {
            return "INCREASING";
        } else if (percentChange < -threshold) {
            return "DECREASING";
        } else {
            return "STABLE";
        }
    }

    // ============================================
    // Spike Detection
    // ============================================

    /**
     * Detect if a value represents a spike compared to baseline.
     *
     * @param value Current value
     * @param baseline Expected baseline value
     * @param multiplier Multiplier threshold (e.g., 2.0 = 2x baseline)
     * @return true if the value is a spike
     */
    public static boolean isSpike(double value, double baseline, double multiplier) {
        if (baseline == 0) {
            return value > 0;
        }
        return value > baseline * multiplier;
    }

    /**
     * Calculate deviation factor from baseline.
     *
     * @param value Current value
     * @param baseline Expected baseline value
     * @return Deviation factor (e.g., 2.0 means value is 2x baseline)
     */
    public static double calculateDeviationFactor(double value, double baseline) {
        if (baseline == 0) {
            return value > 0 ? Double.MAX_VALUE : 0;
        }
        return value / baseline;
    }

    // ============================================
    // Utility Methods
    // ============================================

    /**
     * Round a double to specified decimal places.
     */
    public static double round(double value, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(value * factor) / factor;
    }

    /**
     * Round a double to 2 decimal places.
     */
    public static double round(double value) {
        return round(value, 2);
    }

    /**
     * Clamp a value between min and max bounds.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Normalize a value to 0-100 scale.
     *
     * @param value Value to normalize
     * @param min Minimum expected value
     * @param max Maximum expected value
     * @return Normalized value (0-100)
     */
    public static double normalizeToPercentile(double value, double min, double max) {
        if (max == min) {
            return 50.0;
        }
        double normalized = (value - min) / (max - min) * 100.0;
        return clamp(normalized, 0, 100);
    }

    /**
     * Calculate coefficient of variation (CV).
     * CV = stdDev / mean * 100
     */
    public static double coefficientOfVariation(Collection<? extends Number> values) {
        double mean = mean(values);
        if (mean == 0) {
            return 0.0;
        }
        double stdDev = standardDeviation(values);
        return (stdDev / mean) * 100.0;
    }
}

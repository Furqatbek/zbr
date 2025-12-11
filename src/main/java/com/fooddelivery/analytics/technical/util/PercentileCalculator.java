package com.fooddelivery.analytics.technical.util;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Utility for calculating percentiles from sorted data sets.
 */
@Component
public class PercentileCalculator {

    /**
     * Calculate a percentile value from a sorted list of values.
     *
     * @param sortedValues The list of values, must be sorted in ascending order
     * @param percentile   The percentile to calculate (0-100)
     * @return The percentile value
     */
    public Long calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0L;
        }

        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("Percentile must be between 0 and 100");
        }

        if (percentile == 0) {
            return sortedValues.get(0);
        }

        if (percentile == 100) {
            return sortedValues.get(sortedValues.size() - 1);
        }

        // Use linear interpolation
        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedValues.get(lowerIndex);
        }

        // Linear interpolation between the two values
        double fraction = index - lowerIndex;
        long lowerValue = sortedValues.get(lowerIndex);
        long upperValue = sortedValues.get(upperIndex);

        return Math.round(lowerValue + fraction * (upperValue - lowerValue));
    }

    /**
     * Calculate a percentile value from a sorted list of double values.
     *
     * @param sortedValues The list of values, must be sorted in ascending order
     * @param percentile   The percentile to calculate (0-100)
     * @return The percentile value
     */
    public Double calculatePercentileDouble(List<Double> sortedValues, int percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0.0;
        }

        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("Percentile must be between 0 and 100");
        }

        if (percentile == 0) {
            return sortedValues.get(0);
        }

        if (percentile == 100) {
            return sortedValues.get(sortedValues.size() - 1);
        }

        // Use linear interpolation
        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedValues.get(lowerIndex);
        }

        // Linear interpolation between the two values
        double fraction = index - lowerIndex;
        double lowerValue = sortedValues.get(lowerIndex);
        double upperValue = sortedValues.get(upperIndex);

        return lowerValue + fraction * (upperValue - lowerValue);
    }

    /**
     * Calculate multiple percentiles at once from an unsorted list.
     *
     * @param values      The list of values (will be sorted internally)
     * @param percentiles Array of percentiles to calculate
     * @return Array of percentile values in the same order as input percentiles
     */
    public long[] calculatePercentiles(List<Long> values, int... percentiles) {
        if (values == null || values.isEmpty()) {
            return new long[percentiles.length];
        }

        // Sort a copy to avoid modifying the original
        List<Long> sorted = new java.util.ArrayList<>(values);
        Collections.sort(sorted);

        long[] results = new long[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            results[i] = calculatePercentile(sorted, percentiles[i]);
        }

        return results;
    }

    /**
     * Calculate common latency percentiles (p50, p75, p90, p95, p99).
     *
     * @param values The list of latency values
     * @return LatencyPercentiles object with all common percentiles
     */
    public LatencyPercentiles calculateLatencyPercentiles(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return new LatencyPercentiles(0L, 0L, 0L, 0L, 0L);
        }

        List<Long> sorted = new java.util.ArrayList<>(values);
        Collections.sort(sorted);

        return new LatencyPercentiles(
                calculatePercentile(sorted, 50),
                calculatePercentile(sorted, 75),
                calculatePercentile(sorted, 90),
                calculatePercentile(sorted, 95),
                calculatePercentile(sorted, 99)
        );
    }

    /**
     * Record class for common latency percentiles.
     */
    public record LatencyPercentiles(Long p50, Long p75, Long p90, Long p95, Long p99) {}
}

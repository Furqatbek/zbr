package com.fooddelivery.analytics.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for behavioral fraud metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehavioralFraudMetricsDto {

    // Order velocity metrics
    private VelocityMetricsDto orderVelocity;

    // Order value anomalies
    private OrderValueAnomaliesDto orderValueAnomalies;

    // Refund abuse metrics
    private RefundAbuseMetricsDto refundAbuse;

    /**
     * Alias for orderVelocity.
     */
    public VelocityMetricsDto getVelocityMetrics() {
        return orderVelocity;
    }

    /**
     * Alias for refundAbuse.
     */
    public RefundAbuseMetricsDto getRefundMetrics() {
        return refundAbuse;
    }

    /**
     * Alias for orderValueAnomalies.
     */
    public OrderValueAnomaliesDto getOrderValueMetrics() {
        return orderValueAnomalies;
    }

    // Address fraud metrics
    private AddressFraudMetricsDto addressFraud;

    // Restaurant anomalies
    private List<RestaurantAnomalyDto> restaurantAnomalies;

    // Overall behavioral risk
    private Long usersWithAbnormalPatterns;
    private Long totalBehavioralFlags;
    private Map<String, Long> flagsByType;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityMetricsDto {
        private Double avgOrdersPerUserPerDay;
        private Double maxOrdersPerUserPerDay;
        private Long usersExceedingVelocityThreshold;
        private Integer velocityThresholdPerHour;
        private List<VelocityViolationDto> velocityViolations;
        private List<VelocityViolationDto> velocityViolationsList;
        private Map<Integer, Long> ordersPerHourDistribution;

        /**
         * Get count of velocity violations.
         */
        public Long getVelocityViolationsCount() {
            if (velocityViolations != null) return (long) velocityViolations.size();
            if (velocityViolationsList != null) return (long) velocityViolationsList.size();
            return 0L;
        }

        /**
         * Get list of velocity violations.
         */
        public List<VelocityViolationDto> getVelocityViolationsList() {
            return velocityViolationsList != null ? velocityViolationsList : velocityViolations;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityViolationDto {
        private Long userId;
        private Integer ordersInWindow;
        private Integer windowMinutes;
        private LocalDateTime windowStart;
        private LocalDateTime windowEnd;
        private Double normalBaseline;
        private Double deviationFactor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderValueAnomaliesDto {
        private BigDecimal platformMedianOrderValue;
        private BigDecimal platformAvgOrderValue;
        private BigDecimal platformStdDeviation;
        private Long ordersAboveThreshold;
        private Long ordersBelowThreshold;
        private Double anomalyRate;
        private List<OrderValueAnomalyDto> anomalies;
        private Double zScoreThreshold;

        /**
         * Get total anomalous order count (above + below threshold).
         */
        public Long getAnomalousOrderCount() {
            long above = ordersAboveThreshold != null ? ordersAboveThreshold : 0L;
            long below = ordersBelowThreshold != null ? ordersBelowThreshold : 0L;
            return above + below;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderValueAnomalyDto {
        private Long orderId;
        private Long userId;
        private BigDecimal orderValue;
        private Double zScore;
        private String anomalyType; // HIGH or LOW
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundAbuseMetricsDto {
        private Double platformRefundRate;
        private Long totalRefunds;
        private BigDecimal totalRefundAmount;
        private Long usersWithHighRefundRate;
        private Double highRefundRateThreshold;
        private List<RefundAbuserDto> topRefundAbusers;
        private Map<String, Long> refundsByReason;
        private Map<String, Double> refundRateByReason;

        /**
         * Alias for usersWithHighRefundRate.
         */
        public Long getHighRefundUsers() {
            return usersWithHighRefundRate;
        }

        /**
         * Alias for topRefundAbusers.
         */
        public List<RefundAbuserDto> getRefundAbusersList() {
            return topRefundAbusers;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundAbuserDto {
        private Long userId;
        private Long totalOrders;
        private Long refundedOrders;
        private Double refundRate;
        private BigDecimal totalRefundAmount;
        private String mostCommonReason;
        private Integer riskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressFraudMetricsDto {
        private Long uniqueAddresses;
        private Long suspiciousAddresses;
        private Long addressesUsedByMultipleUsers;
        private Integer multiUserThreshold;
        private List<SuspiciousAddressDto> suspiciousAddressList;
        private Map<String, Long> ordersByAddressCluster;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuspiciousAddressDto {
        private String addressHash;
        private String addressPartial; // Masked address
        private Long uniqueUserCount;
        private Long totalOrders;
        private List<Long> userIds;
        private Double suspicionScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantAnomalyDto {
        private Long restaurantId;
        private Double refundRate;
        private Double platformAvgRefundRate;
        private Long totalOrders;
        private Long refundedOrders;
        private BigDecimal totalRefundAmount;
        private String anomalyType;
        private Integer riskScore;
    }
}

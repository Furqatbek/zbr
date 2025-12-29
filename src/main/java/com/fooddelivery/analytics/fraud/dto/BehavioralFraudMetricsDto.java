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
    private VelocityMetricsDto velocityMetrics;

    // Order value anomalies
    private OrderValueAnomaliesDto orderValueAnomalies;
    private OrderValueAnomaliesDto orderValueMetrics;

    // Refund abuse metrics
    private RefundAbuseMetricsDto refundAbuse;
    private RefundAbuseMetricsDto refundMetrics;

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
        private Map<Integer, Long> ordersPerHourDistribution;
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

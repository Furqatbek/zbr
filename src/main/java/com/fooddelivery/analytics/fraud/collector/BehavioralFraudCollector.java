package com.fooddelivery.analytics.fraud.collector;

import com.fooddelivery.analytics.fraud.dto.BehavioralFraudMetricsDto;
import com.fooddelivery.analytics.fraud.repository.FraudOrderHistoryRepository;
import com.fooddelivery.analytics.fraud.util.FraudRiskCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for behavioral fraud metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BehavioralFraudCollector {

    private final FraudOrderHistoryRepository orderHistoryRepository;
    private final FraudRiskCalculator riskCalculator;

    public BehavioralFraudMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate,
                                              int velocityThreshold, int velocityWindowMinutes,
                                              double zScoreThreshold, double refundRateThreshold,
                                              int minOrdersForRefund, int addressThreshold,
                                              boolean includeDetailedLists, int maxListSize) {
        log.debug("Collecting behavioral fraud metrics from {} to {}", startDate, endDate);

        // Collect sub-metrics
        BehavioralFraudMetricsDto.VelocityMetricsDto velocity = collectVelocityMetrics(
                startDate, endDate, velocityThreshold, velocityWindowMinutes, includeDetailedLists, maxListSize);

        BehavioralFraudMetricsDto.OrderValueAnomaliesDto orderValueAnomalies = collectOrderValueAnomalies(
                startDate, endDate, zScoreThreshold, includeDetailedLists, maxListSize);

        BehavioralFraudMetricsDto.RefundAbuseMetricsDto refundAbuse = collectRefundMetrics(
                startDate, endDate, refundRateThreshold, minOrdersForRefund, includeDetailedLists, maxListSize);

        BehavioralFraudMetricsDto.AddressFraudMetricsDto addressFraud = collectAddressFraudMetrics(
                startDate, endDate, addressThreshold, includeDetailedLists, maxListSize);

        List<BehavioralFraudMetricsDto.RestaurantAnomalyDto> restaurantAnomalies =
                collectRestaurantAnomalies(startDate, endDate, minOrdersForRefund, maxListSize);

        // Count total anomalies
        long totalFlags = 0;
        if (velocity.getVelocityViolations() != null) totalFlags += velocity.getVelocityViolations().size();
        if (orderValueAnomalies.getAnomalies() != null) totalFlags += orderValueAnomalies.getAnomalies().size();
        if (refundAbuse.getTopRefundAbusers() != null) totalFlags += refundAbuse.getTopRefundAbusers().size();
        if (addressFraud.getSuspiciousAddressList() != null) totalFlags += addressFraud.getSuspiciousAddressList().size();

        Map<String, Long> flagsByType = new LinkedHashMap<>();
        flagsByType.put("VELOCITY", velocity.getUsersExceedingVelocityThreshold() != null ?
                velocity.getUsersExceedingVelocityThreshold() : 0L);
        flagsByType.put("ORDER_VALUE", orderValueAnomalies.getOrdersAboveThreshold() != null ?
                orderValueAnomalies.getOrdersAboveThreshold() + orderValueAnomalies.getOrdersBelowThreshold() : 0L);
        flagsByType.put("REFUND", refundAbuse.getUsersWithHighRefundRate() != null ?
                refundAbuse.getUsersWithHighRefundRate() : 0L);
        flagsByType.put("ADDRESS", addressFraud.getSuspiciousAddresses() != null ?
                addressFraud.getSuspiciousAddresses() : 0L);

        return BehavioralFraudMetricsDto.builder()
                .orderVelocity(velocity)
                .orderValueAnomalies(orderValueAnomalies)
                .refundAbuse(refundAbuse)
                .addressFraud(addressFraud)
                .restaurantAnomalies(restaurantAnomalies)
                .usersWithAbnormalPatterns(flagsByType.values().stream().mapToLong(Long::longValue).sum())
                .totalBehavioralFlags(totalFlags)
                .flagsByType(flagsByType)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    private BehavioralFraudMetricsDto.VelocityMetricsDto collectVelocityMetrics(
            LocalDateTime startDate, LocalDateTime endDate, int threshold, int windowMinutes,
            boolean includeDetails, int maxSize) {

        Double avgOrders = orderHistoryRepository.getAvgOrdersPerUser(startDate, endDate);
        if (avgOrders == null) avgOrders = 0.0;

        // Get hourly distribution
        Map<Integer, Long> hourlyDist = new LinkedHashMap<>();
        orderHistoryRepository.getHourlyOrderDistribution(startDate, endDate).forEach(row -> {
            hourlyDist.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        });

        // Detect velocity violations
        List<Object[]> violations = orderHistoryRepository.getUsersExceedingVelocity(
                startDate, endDate, threshold);

        List<BehavioralFraudMetricsDto.VelocityViolationDto> violationList = new ArrayList<>();
        if (includeDetails) {
            violationList = violations.stream()
                    .limit(maxSize)
                    .map(row -> BehavioralFraudMetricsDto.VelocityViolationDto.builder()
                            .userId(((Number) row[0]).longValue())
                            .ordersInWindow(((Number) row[1]).intValue())
                            .windowMinutes(windowMinutes)
                            .normalBaseline(avgOrders)
                            .deviationFactor(round(((Number) row[1]).doubleValue() / (avgOrders > 0 ? avgOrders : 1)))
                            .build())
                    .collect(Collectors.toList());
        }

        // Find max orders per user per day (approximation)
        double maxOrders = violations.isEmpty() ? 0 :
                violations.stream().mapToDouble(r -> ((Number) r[1]).doubleValue()).max().orElse(0);

        return BehavioralFraudMetricsDto.VelocityMetricsDto.builder()
                .avgOrdersPerUserPerDay(round(avgOrders))
                .maxOrdersPerUserPerDay(round(maxOrders))
                .usersExceedingVelocityThreshold((long) violations.size())
                .velocityThresholdPerHour(threshold)
                .velocityViolations(violationList)
                .ordersPerHourDistribution(hourlyDist)
                .build();
    }

    private BehavioralFraudMetricsDto.OrderValueAnomaliesDto collectOrderValueAnomalies(
            LocalDateTime startDate, LocalDateTime endDate, double zScoreThreshold,
            boolean includeDetails, int maxSize) {

        Object[] stats = orderHistoryRepository.getOrderValueStats(startDate, endDate);

        BigDecimal avg = stats[0] != null ? new BigDecimal(stats[0].toString()) : BigDecimal.ZERO;
        BigDecimal stdDev = stats[1] != null ? new BigDecimal(stats[1].toString()) : BigDecimal.ZERO;
        BigDecimal median = stats[2] != null ? new BigDecimal(stats[2].toString()) : BigDecimal.ZERO;

        // Calculate thresholds using z-score
        BigDecimal upperThreshold = avg.add(stdDev.multiply(BigDecimal.valueOf(zScoreThreshold)));
        BigDecimal lowerThreshold = avg.subtract(stdDev.multiply(BigDecimal.valueOf(zScoreThreshold)));
        if (lowerThreshold.compareTo(BigDecimal.ZERO) < 0) {
            lowerThreshold = BigDecimal.ZERO;
        }

        List<Object[]> abnormalOrders = orderHistoryRepository.getAbnormalValueOrders(
                startDate, endDate, upperThreshold, lowerThreshold);

        long aboveThreshold = abnormalOrders.stream()
                .filter(r -> ((BigDecimal) r[2]).compareTo(upperThreshold) > 0)
                .count();
        long belowThreshold = abnormalOrders.size() - aboveThreshold;

        List<BehavioralFraudMetricsDto.OrderValueAnomalyDto> anomalyList = new ArrayList<>();
        if (includeDetails) {
            final BigDecimal finalAvg = avg;
            final BigDecimal finalStdDev = stdDev.compareTo(BigDecimal.ZERO) == 0 ?
                    BigDecimal.ONE : stdDev;
            final BigDecimal finalUpperThreshold = upperThreshold;

            anomalyList = abnormalOrders.stream()
                    .limit(maxSize)
                    .map(row -> {
                        BigDecimal orderValue = (BigDecimal) row[2];
                        double zScore = orderValue.subtract(finalAvg)
                                .divide(finalStdDev, 2, RoundingMode.HALF_UP).doubleValue();
                        String type = orderValue.compareTo(finalUpperThreshold) > 0 ? "HIGH" : "LOW";

                        return BehavioralFraudMetricsDto.OrderValueAnomalyDto.builder()
                                .orderId(((Number) row[0]).longValue())
                                .userId(((Number) row[1]).longValue())
                                .orderValue(orderValue)
                                .zScore(round(zScore))
                                .anomalyType(type)
                                .createdAt((LocalDateTime) row[3])
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        double anomalyRate = abnormalOrders.isEmpty() ? 0.0 : abnormalOrders.size() * 100.0 / 1000; // Approximate

        return BehavioralFraudMetricsDto.OrderValueAnomaliesDto.builder()
                .platformMedianOrderValue(median.setScale(2, RoundingMode.HALF_UP))
                .platformAvgOrderValue(avg.setScale(2, RoundingMode.HALF_UP))
                .platformStdDeviation(stdDev.setScale(2, RoundingMode.HALF_UP))
                .ordersAboveThreshold(aboveThreshold)
                .ordersBelowThreshold(belowThreshold)
                .anomalyRate(round(anomalyRate))
                .anomalies(anomalyList)
                .zScoreThreshold(zScoreThreshold)
                .build();
    }

    private BehavioralFraudMetricsDto.RefundAbuseMetricsDto collectRefundMetrics(
            LocalDateTime startDate, LocalDateTime endDate, double refundRateThreshold,
            int minOrders, boolean includeDetails, int maxSize) {

        Object[] platformStats = orderHistoryRepository.getPlatformRefundStats(startDate, endDate);
        long totalOrders = platformStats[0] != null ? ((Number) platformStats[0]).longValue() : 0;
        long refundedOrders = platformStats[1] != null ? ((Number) platformStats[1]).longValue() : 0;
        BigDecimal totalRefundAmount = platformStats[2] != null ?
                new BigDecimal(platformStats[2].toString()) : BigDecimal.ZERO;

        double platformRefundRate = totalOrders > 0 ? refundedOrders * 100.0 / totalOrders : 0.0;

        // Get users with high refund rates
        List<Object[]> highRefundUsers = orderHistoryRepository.getUsersWithHighRefundRate(
                startDate, endDate, minOrders, refundRateThreshold);

        List<BehavioralFraudMetricsDto.RefundAbuserDto> abuserList = new ArrayList<>();
        if (includeDetails) {
            abuserList = highRefundUsers.stream()
                    .limit(maxSize)
                    .map(row -> {
                        long userOrders = ((Number) row[1]).longValue();
                        long userRefunds = ((Number) row[2]).longValue();
                        BigDecimal userRefundAmt = new BigDecimal(row[3].toString());
                        double rate = userOrders > 0 ? userRefunds * 100.0 / userOrders : 0.0;

                        return BehavioralFraudMetricsDto.RefundAbuserDto.builder()
                                .userId(((Number) row[0]).longValue())
                                .totalOrders(userOrders)
                                .refundedOrders(userRefunds)
                                .refundRate(round(rate))
                                .totalRefundAmount(userRefundAmt)
                                .riskScore(riskCalculator.calculateRefundRiskScore(rate, userRefunds))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        // Refund breakdown by reason
        Map<String, Long> refundsByReason = new LinkedHashMap<>();
        Map<String, Double> refundRateByReason = new LinkedHashMap<>();
        orderHistoryRepository.getRefundsByReason(startDate, endDate).forEach(row -> {
            String reason = row[0] != null ? (String) row[0] : "UNKNOWN";
            long count = ((Number) row[1]).longValue();
            refundsByReason.put(reason, count);
            refundRateByReason.put(reason, totalOrders > 0 ? round(count * 100.0 / totalOrders) : 0.0);
        });

        return BehavioralFraudMetricsDto.RefundAbuseMetricsDto.builder()
                .platformRefundRate(round(platformRefundRate))
                .totalRefunds(refundedOrders)
                .totalRefundAmount(totalRefundAmount)
                .usersWithHighRefundRate((long) highRefundUsers.size())
                .highRefundRateThreshold(refundRateThreshold)
                .topRefundAbusers(abuserList)
                .refundsByReason(refundsByReason)
                .refundRateByReason(refundRateByReason)
                .build();
    }

    private BehavioralFraudMetricsDto.AddressFraudMetricsDto collectAddressFraudMetrics(
            LocalDateTime startDate, LocalDateTime endDate, int threshold,
            boolean includeDetails, int maxSize) {

        Long uniqueAddresses = orderHistoryRepository.countUniqueAddresses(startDate, endDate);
        if (uniqueAddresses == null) uniqueAddresses = 0L;

        List<Object[]> suspiciousAddresses = orderHistoryRepository.getAddressesWithMultipleUsers(
                startDate, endDate, threshold);

        List<BehavioralFraudMetricsDto.SuspiciousAddressDto> suspiciousList = new ArrayList<>();
        if (includeDetails) {
            suspiciousList = suspiciousAddresses.stream()
                    .limit(maxSize)
                    .map(row -> {
                        String addressHash = (String) row[0];
                        long userCount = ((Number) row[1]).longValue();
                        long orderCount = ((Number) row[2]).longValue();
                        String userIdsStr = row[3] != null ? (String) row[3] : "";
                        List<Long> userIds = Arrays.stream(userIdsStr.split(","))
                                .filter(s -> !s.isEmpty())
                                .map(Long::parseLong)
                                .collect(Collectors.toList());

                        double suspicionScore = riskCalculator.calculateAddressSuspicionScore(
                                userCount, orderCount);

                        return BehavioralFraudMetricsDto.SuspiciousAddressDto.builder()
                                .addressHash(addressHash)
                                .addressPartial(maskAddress(addressHash))
                                .uniqueUserCount(userCount)
                                .totalOrders(orderCount)
                                .userIds(userIds)
                                .suspicionScore(round(suspicionScore))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return BehavioralFraudMetricsDto.AddressFraudMetricsDto.builder()
                .uniqueAddresses(uniqueAddresses)
                .suspiciousAddresses((long) suspiciousAddresses.size())
                .addressesUsedByMultipleUsers((long) suspiciousAddresses.size())
                .multiUserThreshold(threshold)
                .suspiciousAddressList(suspiciousList)
                .build();
    }

    private List<BehavioralFraudMetricsDto.RestaurantAnomalyDto> collectRestaurantAnomalies(
            LocalDateTime startDate, LocalDateTime endDate, int minOrders, int maxSize) {

        // Get platform average refund rate
        Object[] platformStats = orderHistoryRepository.getPlatformRefundStats(startDate, endDate);
        long totalOrders = platformStats[0] != null ? ((Number) platformStats[0]).longValue() : 0;
        long refundedOrders = platformStats[1] != null ? ((Number) platformStats[1]).longValue() : 0;
        double platformAvgRefundRate = totalOrders > 0 ? refundedOrders * 100.0 / totalOrders : 0.0;

        return orderHistoryRepository.getRestaurantRefundRates(startDate, endDate, minOrders)
                .stream()
                .filter(row -> {
                    double rate = ((Number) row[3]).doubleValue() * 100;
                    return rate > platformAvgRefundRate * 2; // More than 2x platform average
                })
                .limit(maxSize)
                .map(row -> {
                    double rate = ((Number) row[3]).doubleValue() * 100;
                    return BehavioralFraudMetricsDto.RestaurantAnomalyDto.builder()
                            .restaurantId(((Number) row[0]).longValue())
                            .refundRate(round(rate))
                            .platformAvgRefundRate(round(platformAvgRefundRate))
                            .totalOrders(((Number) row[1]).longValue())
                            .refundedOrders(((Number) row[2]).longValue())
                            .totalRefundAmount(new BigDecimal(row[4].toString()))
                            .anomalyType("HIGH_REFUND_RATE")
                            .riskScore(riskCalculator.calculateRestaurantRiskScore(rate, platformAvgRefundRate))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String maskAddress(String addressHash) {
        if (addressHash == null || addressHash.length() < 8) return "****";
        return addressHash.substring(0, 4) + "****" + addressHash.substring(addressHash.length() - 4);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

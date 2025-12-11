package com.fooddelivery.analytics.fraud.collector;

import com.fooddelivery.analytics.fraud.dto.PaymentFraudMetricsDto;
import com.fooddelivery.analytics.fraud.repository.PaymentAttemptRepository;
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
 * Collector for payment fraud metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFraudCollector {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final FraudRiskCalculator riskCalculator;

    public PaymentFraudMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate,
                                          int failedThreshold, int timeWindowMinutes,
                                          boolean includeDetailedLists, int maxListSize) {
        log.debug("Collecting payment fraud metrics from {} to {}", startDate, endDate);

        // Volume metrics
        Long totalAttempts = paymentAttemptRepository.countTotalAttempts(startDate, endDate);
        Long successful = paymentAttemptRepository.countSuccessfulPayments(startDate, endDate);
        Long failed = paymentAttemptRepository.countFailedPayments(startDate, endDate);
        Long declined = paymentAttemptRepository.countDeclinedPayments(startDate, endDate);
        Long timeout = paymentAttemptRepository.countTimeoutPayments(startDate, endDate);

        if (totalAttempts == null) totalAttempts = 0L;
        if (successful == null) successful = 0L;
        if (failed == null) failed = 0L;
        if (declined == null) declined = 0L;
        if (timeout == null) timeout = 0L;

        // Calculate rates
        double failedRate = totalAttempts > 0 ? (failed + declined) * 100.0 / totalAttempts : 0.0;
        double declineRate = totalAttempts > 0 ? declined * 100.0 / totalAttempts : 0.0;
        double timeoutRate = totalAttempts > 0 ? timeout * 100.0 / totalAttempts : 0.0;

        // User-level metrics
        Long usersWithFailed = paymentAttemptRepository.countUsersWithFailedPayments(startDate, endDate);
        if (usersWithFailed == null) usersWithFailed = 0L;

        // Amount metrics
        BigDecimal totalFailedAmount = paymentAttemptRepository.getTotalFailedAmount(startDate, endDate);
        Object[] amountStats = paymentAttemptRepository.getFailedAmountStats(startDate, endDate);
        BigDecimal avgFailed = amountStats[0] != null ? new BigDecimal(amountStats[0].toString()) : BigDecimal.ZERO;
        BigDecimal maxFailed = amountStats[1] != null ? new BigDecimal(amountStats[1].toString()) : BigDecimal.ZERO;

        // Failure breakdowns
        Map<String, Long> failuresByReason = new HashMap<>();
        paymentAttemptRepository.countFailuresByReason(startDate, endDate).forEach(row -> {
            String reason = row[0] != null ? (String) row[0] : "UNKNOWN";
            failuresByReason.put(reason, ((Number) row[1]).longValue());
        });

        Map<String, Long> failuresByMethod = new HashMap<>();
        paymentAttemptRepository.countFailuresByMethod(startDate, endDate).forEach(row -> {
            if (row[0] != null) {
                failuresByMethod.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        });

        Map<String, Double> failureRateByMethod = new HashMap<>();
        paymentAttemptRepository.getFailureRateByMethod(startDate, endDate).forEach(row -> {
            if (row[0] != null) {
                long methodFailed = ((Number) row[1]).longValue();
                long methodTotal = ((Number) row[2]).longValue();
                double rate = methodTotal > 0 ? methodFailed * 100.0 / methodTotal : 0.0;
                failureRateByMethod.put(row[0].toString(), rate);
            }
        });

        // Card metrics
        Long uniqueCards = paymentAttemptRepository.countUniqueCards(startDate, endDate);
        if (uniqueCards == null) uniqueCards = 0L;

        Map<String, Long> failuresByBin = new HashMap<>();
        paymentAttemptRepository.countFailuresByCardBin(startDate, endDate).forEach(row -> {
            if (row[0] != null) {
                failuresByBin.put((String) row[0], ((Number) row[1]).longValue());
            }
        });

        // Build DTO
        PaymentFraudMetricsDto.PaymentFraudMetricsDtoBuilder builder = PaymentFraudMetricsDto.builder()
                .totalPaymentAttempts(totalAttempts)
                .successfulPayments(successful)
                .failedPayments(failed)
                .declinedPayments(declined)
                .failedPaymentRate(round(failedRate))
                .declineRate(round(declineRate))
                .timeoutRate(round(timeoutRate))
                .usersWithFailedPayments(usersWithFailed)
                .totalFailedAmount(totalFailedAmount)
                .avgFailedAmount(avgFailed.setScale(2, RoundingMode.HALF_UP))
                .maxFailedAmount(maxFailed)
                .failuresByReason(failuresByReason)
                .failuresByMethod(failuresByMethod)
                .failureRateByMethod(failureRateByMethod)
                .uniqueCardsUsed(uniqueCards)
                .failuresByCardBin(failuresByBin)
                .failedAttemptThreshold(failedThreshold)
                .timeWindowMinutes(timeWindowMinutes)
                .periodStart(startDate)
                .periodEnd(endDate);

        // High risk users list
        if (includeDetailedLists) {
            List<PaymentFraudMetricsDto.UserPaymentRiskDto> highRiskUsers = collectHighRiskUsers(
                    startDate, endDate, failedThreshold, maxListSize);
            builder.highRiskUsersList(highRiskUsers);
            builder.highRiskUsers((long) highRiskUsers.size());

            // Calculate average failed per user
            if (usersWithFailed > 0) {
                builder.avgFailedAttemptsPerUser(round((failed + declined) * 1.0 / usersWithFailed));
            }

            // Hourly distribution
            List<PaymentFraudMetricsDto.HourlyPaymentMetricsDto> hourlyDist = collectHourlyDistribution(startDate, endDate);
            builder.hourlyDistribution(hourlyDist);

            // Detect payment spikes
            List<PaymentFraudMetricsDto.PaymentSpikeDto> spikes = detectPaymentSpikes(hourlyDist);
            builder.failedPaymentSpikes(spikes);

            // Cards with multiple failures
            Long cardsWithMultiple = paymentAttemptRepository.getCardsWithMultipleFailures(
                    startDate, endDate, failedThreshold).stream().count();
            builder.cardsWithMultipleFailures(cardsWithMultiple);
        }

        return builder.build();
    }

    private List<PaymentFraudMetricsDto.UserPaymentRiskDto> collectHighRiskUsers(
            LocalDateTime startDate, LocalDateTime endDate, int threshold, int maxSize) {

        return paymentAttemptRepository.getUsersWithHighFailedPayments(startDate, endDate, threshold)
                .stream()
                .limit(maxSize)
                .map(row -> {
                    Long userId = ((Number) row[0]).longValue();
                    Long failedCount = ((Number) row[1]).longValue();
                    Long totalCount = ((Number) row[2]).longValue();
                    double failureRate = totalCount > 0 ? failedCount * 100.0 / totalCount : 0.0;

                    // Get failure reasons for this user
                    List<Object[]> reasons = paymentAttemptRepository.getFailureReasonsByUser(
                            userId, startDate, endDate);
                    String lastReason = reasons.isEmpty() ? null :
                            (reasons.get(0)[0] != null ? (String) reasons.get(0)[0] : "UNKNOWN");

                    int riskScore = riskCalculator.calculatePaymentRiskScore(
                            failedCount.intValue(), failureRate, totalCount.intValue());

                    List<String> riskFlags = new ArrayList<>();
                    if (failedCount >= 5) riskFlags.add("HIGH_FAILURE_COUNT");
                    if (failureRate >= 50) riskFlags.add("HIGH_FAILURE_RATE");

                    return PaymentFraudMetricsDto.UserPaymentRiskDto.builder()
                            .userId(userId)
                            .failedAttempts(failedCount)
                            .totalAttempts(totalCount)
                            .failureRate(round(failureRate))
                            .lastFailureReason(lastReason)
                            .riskScore(riskScore)
                            .riskFlags(riskFlags)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<PaymentFraudMetricsDto.HourlyPaymentMetricsDto> collectHourlyDistribution(
            LocalDateTime startDate, LocalDateTime endDate) {

        Map<Integer, PaymentFraudMetricsDto.HourlyPaymentMetricsDto> hourlyMap = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) {
            hourlyMap.put(i, PaymentFraudMetricsDto.HourlyPaymentMetricsDto.builder()
                    .hour(i).totalAttempts(0L).failedAttempts(0L).failureRate(0.0).build());
        }

        paymentAttemptRepository.getHourlyFailureCounts(startDate, endDate).forEach(row -> {
            int hour = ((Number) row[0]).intValue();
            long failed = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            double rate = total > 0 ? failed * 100.0 / total : 0.0;

            hourlyMap.put(hour, PaymentFraudMetricsDto.HourlyPaymentMetricsDto.builder()
                    .hour(hour).totalAttempts(total).failedAttempts(failed).failureRate(round(rate)).build());
        });

        return new ArrayList<>(hourlyMap.values());
    }

    private List<PaymentFraudMetricsDto.PaymentSpikeDto> detectPaymentSpikes(
            List<PaymentFraudMetricsDto.HourlyPaymentMetricsDto> hourlyData) {

        List<PaymentFraudMetricsDto.PaymentSpikeDto> spikes = new ArrayList<>();

        // Calculate baseline (average)
        double avgFailed = hourlyData.stream()
                .mapToLong(PaymentFraudMetricsDto.HourlyPaymentMetricsDto::getFailedAttempts)
                .average().orElse(0.0);

        double stdDev = calculateStdDev(hourlyData.stream()
                .mapToLong(PaymentFraudMetricsDto.HourlyPaymentMetricsDto::getFailedAttempts)
                .boxed().collect(Collectors.toList()));

        // Detect spikes (more than 2 standard deviations above average)
        double threshold = avgFailed + (2 * stdDev);

        for (PaymentFraudMetricsDto.HourlyPaymentMetricsDto hourData : hourlyData) {
            if (hourData.getFailedAttempts() > threshold && threshold > 0) {
                double deviation = (hourData.getFailedAttempts() - avgFailed) / (stdDev > 0 ? stdDev : 1);
                spikes.add(PaymentFraudMetricsDto.PaymentSpikeDto.builder()
                        .failedCount(hourData.getFailedAttempts())
                        .normalBaseline((long) avgFailed)
                        .deviationFactor(round(deviation))
                        .build());
            }
        }

        return spikes;
    }

    private double calculateStdDev(List<Long> values) {
        if (values.isEmpty()) return 0.0;
        double avg = values.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

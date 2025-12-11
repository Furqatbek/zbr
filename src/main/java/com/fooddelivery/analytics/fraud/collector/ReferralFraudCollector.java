package com.fooddelivery.analytics.fraud.collector;

import com.fooddelivery.analytics.fraud.dto.ReferralFraudMetricsDto;
import com.fooddelivery.analytics.fraud.repository.PromoUsageLogRepository;
import com.fooddelivery.analytics.fraud.repository.ReferralEventRepository;
import com.fooddelivery.analytics.fraud.util.FraudRiskCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for referral fraud and promo abuse metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReferralFraudCollector {

    private final ReferralEventRepository referralEventRepository;
    private final PromoUsageLogRepository promoUsageLogRepository;
    private final FraudRiskCalculator riskCalculator;

    public ReferralFraudMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate,
                                           int deviceThreshold, int ipThreshold, int minOrdersForPromoAbuse,
                                           boolean includeDetailedLists, int maxListSize) {
        log.debug("Collecting referral fraud metrics from {} to {}", startDate, endDate);

        // Basic counts
        Long totalReferrals = referralEventRepository.countTotalReferrals(startDate, endDate);
        Long completed = referralEventRepository.countCompletedReferrals(startDate, endDate);
        Long suspicious = referralEventRepository.countSuspiciousReferrals(startDate, endDate);

        if (totalReferrals == null) totalReferrals = 0L;
        if (completed == null) completed = 0L;
        if (suspicious == null) suspicious = 0L;

        double fraudRate = totalReferrals > 0 ? suspicious * 100.0 / totalReferrals : 0.0;

        // Collect sub-metrics
        ReferralFraudMetricsDto.DeviceReferralFraudDto deviceFraud = collectDeviceFraud(
                startDate, endDate, deviceThreshold, includeDetailedLists, maxListSize);

        ReferralFraudMetricsDto.IpReferralFraudDto ipFraud = collectIpFraud(
                startDate, endDate, ipThreshold, includeDetailedLists, maxListSize);

        ReferralFraudMetricsDto.CircularReferralMetricsDto circularReferrals = collectCircularReferrals(
                startDate, endDate, includeDetailedLists, maxListSize);

        ReferralFraudMetricsDto.PromoAbuseMetricsDto promoAbuse = collectPromoAbuse(
                startDate, endDate, minOrdersForPromoAbuse, includeDetailedLists, maxListSize);

        List<ReferralFraudMetricsDto.FraudulentReferrerDto> topFraudulentReferrers =
                collectTopFraudulentReferrers(startDate, endDate, maxListSize);

        // Financial impact
        BigDecimal totalBonuses = referralEventRepository.getTotalReferralBonusesPaid(startDate, endDate);
        BigDecimal suspectedBonuses = referralEventRepository.getSuspectedFraudulentBonuses(startDate, endDate);
        BigDecimal promoAbused = promoUsageLogRepository.getTotalPromoCreditsUsed(startDate, endDate);

        return ReferralFraudMetricsDto.builder()
                .totalReferrals(totalReferrals)
                .completedReferrals(completed)
                .suspiciousReferrals(suspicious)
                .fraudulentReferralRate(round(fraudRate))
                .deviceFraud(deviceFraud)
                .ipFraud(ipFraud)
                .circularReferrals(circularReferrals)
                .promoAbuse(promoAbuse)
                .topFraudulentReferrers(topFraudulentReferrers)
                .totalReferralBonusesPaid(totalBonuses != null ? totalBonuses : BigDecimal.ZERO)
                .suspectedFraudulentBonuses(suspectedBonuses != null ? suspectedBonuses : BigDecimal.ZERO)
                .promoCreditsAbused(promoAbused != null ? promoAbused : BigDecimal.ZERO)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    private ReferralFraudMetricsDto.DeviceReferralFraudDto collectDeviceFraud(
            LocalDateTime startDate, LocalDateTime endDate, int threshold,
            boolean includeDetails, int maxSize) {

        Long devicesWithMultiple = referralEventRepository.countDevicesWithMultipleSignups(
                startDate, endDate, threshold);
        if (devicesWithMultiple == null) devicesWithMultiple = 0L;

        List<Object[]> deviceClusters = referralEventRepository.getDevicesWithMultipleSignups(
                startDate, endDate, threshold);

        long totalFromShared = deviceClusters.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        List<ReferralFraudMetricsDto.DeviceSignupClusterDto> clusterList = new ArrayList<>();
        if (includeDetails) {
            clusterList = deviceClusters.stream()
                    .limit(maxSize)
                    .map(row -> {
                        String deviceId = (String) row[0];
                        int signupCount = ((Number) row[1]).intValue();
                        String userIdsStr = row[2] != null ? (String) row[2] : "";
                        List<Long> userIds = Arrays.stream(userIdsStr.split(","))
                                .filter(s -> !s.isEmpty())
                                .map(Long::parseLong)
                                .collect(Collectors.toList());

                        return ReferralFraudMetricsDto.DeviceSignupClusterDto.builder()
                                .deviceId(maskIdentifier(deviceId))
                                .signupCount(signupCount)
                                .referredUserIds(userIds)
                                .riskScore(riskCalculator.calculateDeviceReferralRiskScore(signupCount))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return ReferralFraudMetricsDto.DeviceReferralFraudDto.builder()
                .devicesWithMultipleSignups(devicesWithMultiple)
                .totalSignupsFromSharedDevices(totalFromShared)
                .signupThresholdPerDevice(threshold)
                .deviceClusters(clusterList)
                .build();
    }

    private ReferralFraudMetricsDto.IpReferralFraudDto collectIpFraud(
            LocalDateTime startDate, LocalDateTime endDate, int threshold,
            boolean includeDetails, int maxSize) {

        Long ipsWithMultiple = referralEventRepository.countIpsWithMultipleSignups(startDate, endDate, threshold);
        if (ipsWithMultiple == null) ipsWithMultiple = 0L;

        List<Object[]> ipClusters = referralEventRepository.getIpsWithMultipleSignups(
                startDate, endDate, threshold);

        long totalFromShared = ipClusters.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        List<ReferralFraudMetricsDto.IpSignupClusterDto> clusterList = new ArrayList<>();
        if (includeDetails) {
            clusterList = ipClusters.stream()
                    .limit(maxSize)
                    .map(row -> {
                        String ip = (String) row[0];
                        int signupCount = ((Number) row[1]).intValue();
                        String userIdsStr = row[2] != null ? (String) row[2] : "";
                        List<Long> userIds = Arrays.stream(userIdsStr.split(","))
                                .filter(s -> !s.isEmpty())
                                .map(Long::parseLong)
                                .collect(Collectors.toList());

                        return ReferralFraudMetricsDto.IpSignupClusterDto.builder()
                                .ipAddressPartial(maskIp(ip))
                                .signupCount(signupCount)
                                .referredUserIds(userIds)
                                .riskScore(riskCalculator.calculateIpReferralRiskScore(signupCount))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return ReferralFraudMetricsDto.IpReferralFraudDto.builder()
                .ipsWithMultipleSignups(ipsWithMultiple)
                .totalSignupsFromSharedIps(totalFromShared)
                .signupThresholdPerIp(threshold)
                .ipClusters(clusterList)
                .vpnSignups(0L) // Would need separate query
                .vpnSignupRate(0.0)
                .build();
    }

    private ReferralFraudMetricsDto.CircularReferralMetricsDto collectCircularReferrals(
            LocalDateTime startDate, LocalDateTime endDate,
            boolean includeDetails, int maxSize) {

        // Get referral chains
        List<Object[]> chains = referralEventRepository.getReferralChains(startDate, endDate);

        // Build graph and detect cycles
        Map<Long, Set<Long>> referralGraph = new HashMap<>();
        for (Object[] row : chains) {
            Long referrer = ((Number) row[0]).longValue();
            Long referred = ((Number) row[1]).longValue();
            referralGraph.computeIfAbsent(referrer, k -> new HashSet<>()).add(referred);
        }

        List<List<Long>> detectedCycles = detectCycles(referralGraph);

        long usersInCycles = detectedCycles.stream()
                .flatMap(List::stream)
                .distinct()
                .count();

        List<ReferralFraudMetricsDto.CircularChainDto> chainList = new ArrayList<>();
        if (includeDetails) {
            int chainId = 0;
            for (List<Long> cycle : detectedCycles) {
                if (chainList.size() >= maxSize) break;
                chainList.add(ReferralFraudMetricsDto.CircularChainDto.builder()
                        .chainId("CYCLE-" + (++chainId))
                        .userIdsInChain(cycle)
                        .chainLength(cycle.size())
                        .build());
            }
        }

        return ReferralFraudMetricsDto.CircularReferralMetricsDto.builder()
                .circularReferralChains((long) detectedCycles.size())
                .usersInCircularChains(usersInCycles)
                .detectedChains(chainList)
                .build();
    }

    private List<List<Long>> detectCycles(Map<Long, Set<Long>> graph) {
        List<List<Long>> cycles = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Set<Long> recStack = new HashSet<>();

        for (Long node : graph.keySet()) {
            List<Long> path = new ArrayList<>();
            if (detectCycleDFS(node, graph, visited, recStack, path, cycles)) {
                // Cycle detected
            }
        }

        return cycles;
    }

    private boolean detectCycleDFS(Long node, Map<Long, Set<Long>> graph,
                                   Set<Long> visited, Set<Long> recStack,
                                   List<Long> path, List<List<Long>> cycles) {
        if (recStack.contains(node)) {
            // Found a cycle
            int startIdx = path.indexOf(node);
            if (startIdx >= 0) {
                cycles.add(new ArrayList<>(path.subList(startIdx, path.size())));
            }
            return true;
        }

        if (visited.contains(node)) {
            return false;
        }

        visited.add(node);
        recStack.add(node);
        path.add(node);

        Set<Long> neighbors = graph.getOrDefault(node, Collections.emptySet());
        for (Long neighbor : neighbors) {
            detectCycleDFS(neighbor, graph, visited, recStack, path, cycles);
        }

        path.remove(path.size() - 1);
        recStack.remove(node);
        return false;
    }

    private ReferralFraudMetricsDto.PromoAbuseMetricsDto collectPromoAbuse(
            LocalDateTime startDate, LocalDateTime endDate, int minOrders,
            boolean includeDetails, int maxSize) {

        Long totalPromoUsers = promoUsageLogRepository.countTotalPromoUsers(startDate, endDate);
        Long usersNoPayingOrders = promoUsageLogRepository.countUsersWithNoPayingOrders(startDate, endDate);
        BigDecimal totalPromoUsed = promoUsageLogRepository.getTotalPromoCreditsUsed(startDate, endDate);
        BigDecimal avgPromo = promoUsageLogRepository.getAvgPromoCreditsPerUsage(startDate, endDate);

        if (totalPromoUsers == null) totalPromoUsers = 0L;
        if (usersNoPayingOrders == null) usersNoPayingOrders = 0L;

        double promoOnlyRate = totalPromoUsers > 0 ? usersNoPayingOrders * 100.0 / totalPromoUsers : 0.0;

        List<Object[]> promoAbusers = promoUsageLogRepository.getTopPromoAbusers(startDate, endDate, minOrders);

        List<ReferralFraudMetricsDto.PromoAbuserDto> abuserList = new ArrayList<>();
        if (includeDetails) {
            abuserList = promoAbusers.stream()
                    .limit(maxSize)
                    .map(row -> {
                        Long userId = ((Number) row[0]).longValue();
                        long orders = ((Number) row[1]).longValue();
                        BigDecimal spent = new BigDecimal(row[2].toString());
                        BigDecimal promo = new BigDecimal(row[3].toString());
                        BigDecimal netPaid = new BigDecimal(row[4].toString());
                        double promoRate = spent.compareTo(BigDecimal.ZERO) > 0 ?
                                promo.doubleValue() * 100 / spent.doubleValue() : 100.0;

                        List<String> indicators = new ArrayList<>();
                        if (netPaid.compareTo(BigDecimal.ZERO) == 0) indicators.add("NO_PAID_ORDERS");
                        if (promoRate > 90) indicators.add("HIGH_PROMO_RATE");

                        return ReferralFraudMetricsDto.PromoAbuserDto.builder()
                                .userId(userId)
                                .totalOrders(orders)
                                .promoOrders(orders) // Approximation
                                .totalSpent(spent)
                                .totalPromoCredits(promo)
                                .netPaid(netPaid)
                                .promoOrderRate(round(promoRate))
                                .hasAnyPaidOrder(netPaid.compareTo(BigDecimal.ZERO) > 0)
                                .riskScore(riskCalculator.calculatePromoAbuseRiskScore(promoRate,
                                        netPaid.compareTo(BigDecimal.ZERO) == 0))
                                .abuseIndicators(indicators)
                                .build();
                    })
                    .filter(a -> !a.getHasAnyPaidOrder() || a.getPromoOrderRate() > 80)
                    .collect(Collectors.toList());
        }

        // Promo only users (completed >= minOrders with zero payment)
        List<Object[]> promoOnlyUsers = promoUsageLogRepository.getPromoOnlyUsers(startDate, endDate, minOrders);

        return ReferralFraudMetricsDto.PromoAbuseMetricsDto.builder()
                .totalPromoUsers(totalPromoUsers)
                .promoOnlyUsers((long) promoOnlyUsers.size())
                .promoOnlyUserRate(round(promoOnlyRate))
                .usersWithNoPayingOrders(usersNoPayingOrders)
                .totalPromoCreditsUsed(totalPromoUsed != null ? totalPromoUsed : BigDecimal.ZERO)
                .avgPromoCreditsPerUser(avgPromo != null ? avgPromo : BigDecimal.ZERO)
                .topPromoAbusers(abuserList)
                .minOrdersForPromoAbuse(minOrders)
                .build();
    }

    private List<ReferralFraudMetricsDto.FraudulentReferrerDto> collectTopFraudulentReferrers(
            LocalDateTime startDate, LocalDateTime endDate, int maxSize) {

        return referralEventRepository.getTopFraudulentReferrers(startDate, endDate)
                .stream()
                .limit(maxSize)
                .map(row -> {
                    Long referrerId = ((Number) row[0]).longValue();
                    long total = ((Number) row[1]).longValue();
                    long suspiciousCount = ((Number) row[2]).longValue();
                    long completedCount = ((Number) row[3]).longValue();
                    BigDecimal bonuses = new BigDecimal(row[4].toString());
                    double suspiciousRate = total > 0 ? suspiciousCount * 100.0 / total : 0.0;

                    List<String> indicators = new ArrayList<>();
                    if (suspiciousRate > 50) indicators.add("HIGH_SUSPICIOUS_RATE");
                    if (suspiciousCount > 5) indicators.add("MANY_SUSPICIOUS_REFERRALS");

                    return ReferralFraudMetricsDto.FraudulentReferrerDto.builder()
                            .referrerId(referrerId)
                            .totalReferrals(total)
                            .suspiciousReferrals(suspiciousCount)
                            .completedReferrals(completedCount)
                            .totalBonusesEarned(bonuses)
                            .suspiciousRate(round(suspiciousRate))
                            .fraudIndicators(indicators)
                            .riskScore(riskCalculator.calculateReferrerRiskScore(
                                    suspiciousRate, suspiciousCount))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 8) return "****";
        return identifier.substring(0, 4) + "****";
    }

    private String maskIp(String ip) {
        if (ip == null) return "****";
        String[] parts = ip.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1] + ".***";
        }
        return "****";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

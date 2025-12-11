package com.fooddelivery.analytics.fraud.collector;

import com.fooddelivery.analytics.fraud.dto.AccountIntegrityMetricsDto;
import com.fooddelivery.analytics.fraud.repository.DeviceFingerprintRepository;
import com.fooddelivery.analytics.fraud.repository.FraudOrderHistoryRepository;
import com.fooddelivery.analytics.fraud.repository.PaymentAttemptRepository;
import com.fooddelivery.analytics.fraud.util.FraudRiskCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for account integrity and multi-account fraud metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountIntegrityCollector {

    private final FraudOrderHistoryRepository orderHistoryRepository;
    private final DeviceFingerprintRepository deviceFingerprintRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final FraudRiskCalculator riskCalculator;

    public AccountIntegrityMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate,
                                               int fakeAccountAgeThreshold, int deviceThreshold,
                                               int ipThreshold, boolean includeDetailedLists, int maxListSize) {
        log.debug("Collecting account integrity metrics from {} to {}", startDate, endDate);

        // Collect sub-metrics
        AccountIntegrityMetricsDto.FakeAccountMetricsDto fakeAccounts = collectFakeAccountMetrics(
                startDate, endDate, fakeAccountAgeThreshold, includeDetailedLists, maxListSize);

        AccountIntegrityMetricsDto.MultiAccountMetricsDto multiAccounts = collectMultiAccountMetrics(
                startDate, endDate, deviceThreshold, ipThreshold, includeDetailedLists, maxListSize);

        AccountIntegrityMetricsDto.DeviceIntegrityMetricsDto deviceIntegrity = collectDeviceIntegrityMetrics(
                startDate, endDate, deviceThreshold, includeDetailedLists, maxListSize);

        AccountIntegrityMetricsDto.PaymentTokenSharingDto tokenSharing = collectPaymentTokenSharing(
                startDate, endDate, includeDetailedLists, maxListSize);

        // Calculate overall metrics
        long totalFlagged = (fakeAccounts.getSuspectedFakeAccounts() != null ? fakeAccounts.getSuspectedFakeAccounts() : 0) +
                (multiAccounts.getSuspectedMultiAccountUsers() != null ? multiAccounts.getSuspectedMultiAccountUsers() : 0);

        double integrityScore = calculateIntegrityScore(fakeAccounts, multiAccounts, deviceIntegrity);

        return AccountIntegrityMetricsDto.builder()
                .fakeAccounts(fakeAccounts)
                .multiAccounts(multiAccounts)
                .deviceIntegrity(deviceIntegrity)
                .paymentTokenSharing(tokenSharing)
                .totalFlaggedAccounts(totalFlagged)
                .accountsUnderReview(totalFlagged / 2) // Approximation
                .confirmedFraudAccounts(0L) // Would need actual confirmation data
                .accountIntegrityScore(round(integrityScore))
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    private AccountIntegrityMetricsDto.FakeAccountMetricsDto collectFakeAccountMetrics(
            LocalDateTime startDate, LocalDateTime endDate, int ageThreshold,
            boolean includeDetails, int maxSize) {

        Long suspectedFake = orderHistoryRepository.countSuspectedFakeAccounts(startDate, endDate, ageThreshold);
        if (suspectedFake == null) suspectedFake = 0L;

        List<Object[]> newAccountOrders = orderHistoryRepository.getNewAccountOrders(startDate, endDate, ageThreshold);

        // Count total new accounts (approximation from orders)
        long totalNew = newAccountOrders.stream()
                .map(r -> ((Number) r[0]).longValue())
                .distinct()
                .count();

        double fakeRate = totalNew > 0 ? suspectedFake * 100.0 / totalNew : 0.0;

        List<AccountIntegrityMetricsDto.FakeAccountDto> fakeList = new ArrayList<>();
        if (includeDetails) {
            Map<Long, AccountIntegrityMetricsDto.FakeAccountDto.FakeAccountDtoBuilder> userMap = new HashMap<>();

            for (Object[] row : newAccountOrders) {
                Long userId = ((Number) row[0]).longValue();
                Integer ageHours = row[1] != null ? ((Number) row[1]).intValue() : null;
                Boolean hasPromo = row[2] != null && (Boolean) row[2];

                if (!userMap.containsKey(userId)) {
                    List<String> riskIndicators = new ArrayList<>();
                    if (ageHours != null && ageHours < 1) riskIndicators.add("VERY_NEW_ACCOUNT");
                    if (hasPromo) riskIndicators.add("PROMO_ON_FIRST_ORDER");

                    int riskScore = riskCalculator.calculateFakeAccountRiskScore(
                            ageHours != null ? ageHours : 0, hasPromo);

                    userMap.put(userId, AccountIntegrityMetricsDto.FakeAccountDto.builder()
                            .userId(userId)
                            .accountAgeHours(ageHours)
                            .hasCompletedOrder(true)
                            .usedPromoOnFirstOrder(hasPromo)
                            .riskScore(riskScore)
                            .riskIndicators(riskIndicators));
                }
            }

            fakeList = userMap.values().stream()
                    .map(AccountIntegrityMetricsDto.FakeAccountDto.FakeAccountDtoBuilder::build)
                    .sorted((a, b) -> Integer.compare(b.getRiskScore(), a.getRiskScore()))
                    .limit(maxSize)
                    .collect(Collectors.toList());
        }

        return AccountIntegrityMetricsDto.FakeAccountMetricsDto.builder()
                .totalNewAccounts(totalNew)
                .suspectedFakeAccounts(suspectedFake)
                .fakeAccountRate(round(fakeRate))
                .accountAgeThresholdHours(ageThreshold)
                .fakeAccountsList(fakeList)
                .build();
    }

    private AccountIntegrityMetricsDto.MultiAccountMetricsDto collectMultiAccountMetrics(
            LocalDateTime startDate, LocalDateTime endDate, int deviceThreshold, int ipThreshold,
            boolean includeDetails, int maxSize) {

        // Devices with multiple users
        Long devicesWithMultiUsers = deviceFingerprintRepository.countDevicesSharedAcrossUsers(
                startDate, endDate, deviceThreshold);
        if (devicesWithMultiUsers == null) devicesWithMultiUsers = 0L;

        // Get device clusters
        List<Object[]> deviceClusters = orderHistoryRepository.getDevicesWithMultipleUsers(
                startDate, endDate, deviceThreshold);

        List<AccountIntegrityMetricsDto.MultiAccountClusterDto> clusterList = new ArrayList<>();
        if (includeDetails) {
            clusterList = deviceClusters.stream()
                    .limit(maxSize)
                    .map(row -> {
                        String deviceId = (String) row[0];
                        long userCount = ((Number) row[1]).longValue();
                        long orderCount = ((Number) row[2]).longValue();
                        String userIdsStr = row[3] != null ? (String) row[3] : "";
                        List<Long> userIds = Arrays.stream(userIdsStr.split(","))
                                .filter(s -> !s.isEmpty())
                                .map(Long::parseLong)
                                .collect(Collectors.toList());

                        return AccountIntegrityMetricsDto.MultiAccountClusterDto.builder()
                                .clusterId("DEVICE-" + deviceId.hashCode())
                                .clusterType("DEVICE")
                                .clusterIdentifier(maskIdentifier(deviceId))
                                .userIds(userIds)
                                .userCount((int) userCount)
                                .totalOrders(orderCount)
                                .riskScore(riskCalculator.calculateMultiAccountRiskScore(
                                        (int) userCount, orderCount))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        // Count suspected multi-account users
        long suspectedMultiAccount = deviceClusters.stream()
                .flatMap(row -> {
                    String userIdsStr = row[3] != null ? (String) row[3] : "";
                    return Arrays.stream(userIdsStr.split(","))
                            .filter(s -> !s.isEmpty())
                            .map(Long::parseLong);
                })
                .distinct()
                .count();

        Map<String, Long> byIndicator = new LinkedHashMap<>();
        byIndicator.put("DEVICE", devicesWithMultiUsers);
        byIndicator.put("IP", 0L); // Would need IP correlation query

        return AccountIntegrityMetricsDto.MultiAccountMetricsDto.builder()
                .devicesWithMultipleUsers(devicesWithMultiUsers)
                .ipsWithMultipleUsers(0L) // Placeholder
                .phonesWithMultipleAccounts(0L) // Placeholder
                .suspectedMultiAccountUsers(suspectedMultiAccount)
                .multiAccountThreshold(deviceThreshold)
                .multiAccountClusters(clusterList)
                .multiAccountsByIndicator(byIndicator)
                .build();
    }

    private AccountIntegrityMetricsDto.DeviceIntegrityMetricsDto collectDeviceIntegrityMetrics(
            LocalDateTime startDate, LocalDateTime endDate, int shareThreshold,
            boolean includeDetails, int maxSize) {

        Long totalDevices = deviceFingerprintRepository.countTotalDevices(startDate, endDate);
        Long emulators = deviceFingerprintRepository.countEmulatorDevices(startDate, endDate);
        Long rooted = deviceFingerprintRepository.countRootedDevices(startDate, endDate);
        Long lowTrust = deviceFingerprintRepository.countLowTrustDevices(startDate, endDate, 50);
        Long shared = deviceFingerprintRepository.countDevicesSharedAcrossUsers(startDate, endDate, shareThreshold);

        if (totalDevices == null) totalDevices = 0L;
        if (emulators == null) emulators = 0L;
        if (rooted == null) rooted = 0L;
        if (lowTrust == null) lowTrust = 0L;
        if (shared == null) shared = 0L;

        // Device reuse rate
        Object[] reuseStats = deviceFingerprintRepository.getDeviceReuseStats(startDate, endDate);
        long totalDev = reuseStats[0] != null ? ((Number) reuseStats[0]).longValue() : 0;
        long reusedDev = reuseStats[1] != null ? ((Number) reuseStats[1]).longValue() : 0;
        double reuseRate = totalDev > 0 ? reusedDev * 100.0 / totalDev : 0.0;

        // Suspicious devices
        Long suspicious = deviceFingerprintRepository.countSuspiciousDevices(startDate, endDate, 50);
        if (suspicious == null) suspicious = 0L;

        List<AccountIntegrityMetricsDto.SuspiciousDeviceDto> suspiciousList = new ArrayList<>();
        if (includeDetails) {
            suspiciousList = deviceFingerprintRepository.getSuspiciousDevices(startDate, endDate, 50)
                    .stream()
                    .limit(maxSize)
                    .map(row -> AccountIntegrityMetricsDto.SuspiciousDeviceDto.builder()
                            .deviceId((String) row[0])
                            .userId(((Number) row[1]).longValue())
                            .deviceType((String) row[2])
                            .isEmulator(row[3] != null && (Boolean) row[3])
                            .isRooted(row[4] != null && (Boolean) row[4])
                            .trustScore(row[5] != null ? ((Number) row[5]).intValue() : 0)
                            .firstSeenAt((LocalDateTime) row[6])
                            .build())
                    .collect(Collectors.toList());
        }

        // Trust level distribution
        Map<String, Long> byTrustLevel = new LinkedHashMap<>();
        deviceFingerprintRepository.getDevicesByTrustLevel(startDate, endDate).forEach(row -> {
            byTrustLevel.put((String) row[0], ((Number) row[1]).longValue());
        });

        return AccountIntegrityMetricsDto.DeviceIntegrityMetricsDto.builder()
                .totalDevices(totalDevices)
                .suspiciousDevices(suspicious)
                .emulatorDevices(emulators)
                .rootedDevices(rooted)
                .lowTrustDevices(lowTrust)
                .deviceReuseRate(round(reuseRate))
                .devicesSharedAcrossUsers(shared)
                .deviceShareThreshold(shareThreshold)
                .suspiciousDevicesList(suspiciousList)
                .devicesByTrustLevel(byTrustLevel)
                .build();
    }

    private AccountIntegrityMetricsDto.PaymentTokenSharingDto collectPaymentTokenSharing(
            LocalDateTime startDate, LocalDateTime endDate, boolean includeDetails, int maxSize) {

        List<Object[]> sharedTokens = paymentAttemptRepository.detectSharedPaymentTokens(startDate, endDate);

        List<AccountIntegrityMetricsDto.SharedTokenDto> sharedList = new ArrayList<>();
        if (includeDetails) {
            sharedList = sharedTokens.stream()
                    .limit(maxSize)
                    .map(row -> {
                        String tokenHash = (String) row[0];
                        int userCount = ((Number) row[1]).intValue();
                        long txCount = ((Number) row[2]).longValue();

                        return AccountIntegrityMetricsDto.SharedTokenDto.builder()
                                .tokenHashPartial(maskIdentifier(tokenHash))
                                .userCount(userCount)
                                .totalTransactions(txCount)
                                .riskScore(riskCalculator.calculateTokenSharingRiskScore(userCount, txCount))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return AccountIntegrityMetricsDto.PaymentTokenSharingDto.builder()
                .uniquePaymentTokens(0L) // Would need separate query
                .sharedTokens((long) sharedTokens.size())
                .tokenSharingRate(0.0) // Would need total tokens
                .sharedTokensList(sharedList)
                .build();
    }

    private double calculateIntegrityScore(
            AccountIntegrityMetricsDto.FakeAccountMetricsDto fakeAccounts,
            AccountIntegrityMetricsDto.MultiAccountMetricsDto multiAccounts,
            AccountIntegrityMetricsDto.DeviceIntegrityMetricsDto deviceIntegrity) {

        double score = 100.0;

        // Deduct for fake accounts
        if (fakeAccounts.getFakeAccountRate() != null) {
            score -= fakeAccounts.getFakeAccountRate() * 2;
        }

        // Deduct for multi-accounts
        if (multiAccounts.getSuspectedMultiAccountUsers() != null) {
            score -= Math.min(multiAccounts.getSuspectedMultiAccountUsers() * 0.5, 20);
        }

        // Deduct for suspicious devices
        if (deviceIntegrity.getSuspiciousDevices() != null && deviceIntegrity.getTotalDevices() != null
                && deviceIntegrity.getTotalDevices() > 0) {
            double suspiciousRate = deviceIntegrity.getSuspiciousDevices() * 100.0 / deviceIntegrity.getTotalDevices();
            score -= suspiciousRate;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 8) return "****";
        return identifier.substring(0, 4) + "****" + identifier.substring(identifier.length() - 4);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

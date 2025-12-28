package com.fooddelivery.analytics.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for account integrity and multi-account fraud metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountIntegrityMetricsDto {

    // Fake account metrics
    private FakeAccountMetricsDto fakeAccounts;

    // Multi-account metrics
    private MultiAccountMetricsDto multiAccounts;

    // Device integrity metrics
    private DeviceIntegrityMetricsDto deviceIntegrity;

    // Payment token sharing
    private PaymentTokenSharingDto paymentTokenSharing;

    // Overall integrity metrics
    private Long totalFlaggedAccounts;
    private Long accountsUnderReview;
    private Long confirmedFraudAccounts;
    private Double accountIntegrityScore;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FakeAccountMetricsDto {
        private Long totalNewAccounts;
        private Long suspectedFakeAccounts;
        private Double fakeAccountRate;
        private Integer accountAgeThresholdHours;
        private List<FakeAccountDto> fakeAccountsList;
        private Map<Integer, Long> newAccountsByHour;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FakeAccountDto {
        private Long userId;
        private Integer accountAgeHours;
        private Boolean hasCompletedOrder;
        private Boolean usedPromoOnFirstOrder;
        private String deviceId;
        private String ipAddress;
        private Boolean isVpnIp;
        private Integer riskScore;
        private List<String> riskIndicators;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultiAccountMetricsDto {
        private Long devicesWithMultipleUsers;
        private Long ipsWithMultipleUsers;
        private Long phonesWithMultipleAccounts;
        private Long suspectedMultiAccountUsers;
        private Integer multiAccountThreshold;
        private List<MultiAccountClusterDto> multiAccountClusters;
        private Map<String, Long> multiAccountsByIndicator;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultiAccountClusterDto {
        private String clusterId;
        private String clusterType; // DEVICE, IP, PHONE, PAYMENT_TOKEN
        private String clusterIdentifier; // The shared value (masked)
        private List<Long> userIds;
        private Integer userCount;
        private Long totalOrders;
        private Long promoOrders;
        private Double promoOrderRate;
        private Integer riskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceIntegrityMetricsDto {
        private Long totalDevices;
        private Long suspiciousDevices;
        private Long emulatorDevices;
        private Long rootedDevices;
        private Long lowTrustDevices;
        private Double deviceReuseRate;
        private Long devicesSharedAcrossUsers;
        private Integer deviceShareThreshold;
        private List<SuspiciousDeviceDto> suspiciousDevicesList;
        private Map<String, Long> devicesByTrustLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuspiciousDeviceDto {
        private String deviceId;
        private String deviceFingerprint;
        private Long userId; // Single user for mapping from query
        private Integer userCount;
        private List<Long> userIds;
        private Boolean isEmulator;
        private Boolean isRooted;
        private Integer trustScore;
        private Long totalOrders;
        private String deviceType;
        private LocalDateTime firstSeenAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentTokenSharingDto {
        private Long uniquePaymentTokens;
        private Long sharedTokens;
        private Double tokenSharingRate;
        private List<SharedTokenDto> sharedTokensList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedTokenDto {
        private String tokenHashPartial; // Masked
        private Integer userCount;
        private List<Long> userIds;
        private Long totalTransactions;
        private Integer riskScore;
    }
}

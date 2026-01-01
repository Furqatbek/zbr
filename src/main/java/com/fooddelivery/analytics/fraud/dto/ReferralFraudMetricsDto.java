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
 * DTO for referral fraud and promo abuse metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralFraudMetricsDto {

    // Referral volume metrics
    private Long totalReferrals;
    private Long completedReferrals;
    private Long suspiciousReferrals;
    private Long totalFraudulentReferrals;
    private Double fraudulentReferralRate;
    private Double overallReferralFraudRate;

    // Device-based fraud
    private DeviceReferralFraudDto deviceFraud;

    // IP-based fraud
    private IpReferralFraudDto ipFraud;

    // Circular referral detection
    private CircularReferralMetricsDto circularReferrals;
    private CircularReferralMetricsDto circularReferralMetrics;

    // Promo abuse metrics
    private PromoAbuseMetricsDto promoAbuse;
    private PromoAbuseMetricsDto promoAbuseMetrics;

    public Double getOverallReferralFraudRate() {
        return overallReferralFraudRate != null ? overallReferralFraudRate : fraudulentReferralRate;
    }

    public Long getTotalFraudulentReferrals() {
        return totalFraudulentReferrals != null ? totalFraudulentReferrals : suspiciousReferrals;
    }

    public CircularReferralMetricsDto getCircularReferralMetrics() {
        return circularReferralMetrics != null ? circularReferralMetrics : circularReferrals;
    }

    public PromoAbuseMetricsDto getPromoAbuseMetrics() {
        return promoAbuseMetrics != null ? promoAbuseMetrics : promoAbuse;
    }

    // Top fraudulent referrers
    private List<FraudulentReferrerDto> topFraudulentReferrers;

    // Financial impact
    private BigDecimal totalReferralBonusesPaid;
    private BigDecimal suspectedFraudulentBonuses;
    private BigDecimal promoCreditsAbused;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceReferralFraudDto {
        private Long devicesWithMultipleSignups;
        private Long totalSignupsFromSharedDevices;
        private Integer signupThresholdPerDevice;
        private List<DeviceSignupClusterDto> deviceClusters;
        private Map<String, Long> signupsByDeviceType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceSignupClusterDto {
        private String deviceId;
        private String deviceFingerprint;
        private Integer signupCount;
        private List<Long> referredUserIds;
        private List<Long> referrerIds;
        private BigDecimal totalBonusesClaimed;
        private Integer riskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IpReferralFraudDto {
        private Long ipsWithMultipleSignups;
        private Long totalSignupsFromSharedIps;
        private Integer signupThresholdPerIp;
        private List<IpSignupClusterDto> ipClusters;
        private Long vpnSignups;
        private Double vpnSignupRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IpSignupClusterDto {
        private String ipAddressPartial; // Masked
        private String geoLocation;
        private Integer signupCount;
        private List<Long> referredUserIds;
        private Boolean isVpn;
        private Boolean isTor;
        private Integer riskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircularReferralMetricsDto {
        private Long circularReferralChains;
        private Long usersInCircularChains;
        private BigDecimal bonusesInCircularChains;
        private List<CircularChainDto> detectedChains;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircularChainDto {
        private String chainId;
        private List<Long> userIdsInChain;
        private Integer chainLength;
        private BigDecimal totalBonusesClaimed;
        private LocalDateTime firstEventAt;
        private LocalDateTime lastEventAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromoAbuseMetricsDto {
        private Long totalPromoUsers;
        private Long promoOnlyUsers;
        private Double promoOnlyUserRate;
        private Long usersWithNoPayingOrders;
        private BigDecimal totalPromoCreditsUsed;
        private BigDecimal avgPromoCreditsPerUser;
        private List<PromoAbuserDto> topPromoAbusers;
        private Map<String, Long> abuseByPromoType;
        private Integer minOrdersForPromoAbuse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromoAbuserDto {
        private Long userId;
        private Long totalOrders;
        private Long promoOrders;
        private BigDecimal totalSpent;
        private BigDecimal totalPromoCredits;
        private BigDecimal netPaid;
        private Double promoOrderRate;
        private Boolean hasAnyPaidOrder;
        private Integer riskScore;
        private List<String> abuseIndicators;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudulentReferrerDto {
        private Long referrerId;
        private Long totalReferrals;
        private Long successfulReferrals;
        private Long fraudulentReferrals;
        private Long suspiciousReferrals;
        private Long completedReferrals;
        private BigDecimal totalBonusesEarned;
        private BigDecimal earningsTotal;
        private Double suspiciousRate;
        private Double fraudRate;
        private List<String> fraudIndicators;
        private Integer riskScore;
    }
}

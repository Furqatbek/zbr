package com.fooddelivery.analytics.fraud.mapper;

import com.fooddelivery.analytics.fraud.dto.*;
import com.fooddelivery.analytics.fraud.model.*;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MapStruct mapper for fraud analytics entities to DTOs.
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FraudEntityMapper {

    // ============================================
    // Payment Attempt Mappings
    // ============================================

    @Mapping(target = "failedAttempts", source = "failureCount")
    @Mapping(target = "totalAttempts", source = "totalCount")
    @Mapping(target = "failureRate", expression = "java(calculateRate(source.getFailureCount(), source.getTotalCount()))")
    @Mapping(target = "lastFailureReason", source = "failureReason")
    PaymentFraudMetricsDto.UserPaymentRiskDto toUserPaymentRisk(PaymentAttemptAggregation source);

    List<PaymentFraudMetricsDto.UserPaymentRiskDto> toUserPaymentRiskList(List<PaymentAttemptAggregation> sources);

    // ============================================
    // Security Flag Mappings
    // ============================================

    @Mapping(target = "flagId", source = "id")
    @Mapping(target = "flagType", source = "flagType")
    @Mapping(target = "severityLevel", source = "severity")
    @Mapping(target = "flaggedAt", source = "flaggedAt")
    @Mapping(target = "description", source = "description")
    SecurityLogMetricsDto.SecurityFlagDto toSecurityFlagDto(SecurityFlag flag);

    List<SecurityLogMetricsDto.SecurityFlagDto> toSecurityFlagDtoList(List<SecurityFlag> flags);

    // ============================================
    // Device Fingerprint Mappings
    // ============================================

    @Mapping(target = "deviceId", source = "deviceId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "deviceType", source = "deviceType")
    @Mapping(target = "isEmulator", source = "emulator")
    @Mapping(target = "isRooted", source = "rooted")
    @Mapping(target = "trustScore", source = "trustScore")
    @Mapping(target = "firstSeenAt", source = "firstSeenAt")
    AccountIntegrityMetricsDto.SuspiciousDeviceDto toSuspiciousDeviceDto(DeviceFingerprint device);

    List<AccountIntegrityMetricsDto.SuspiciousDeviceDto> toSuspiciousDeviceDtoList(List<DeviceFingerprint> devices);

    // ============================================
    // Auth Log Mappings
    // ============================================

    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "failedAttempts", source = "failedCount")
    @Mapping(target = "uniqueUsersTargeted", source = "uniqueUsers")
    @Mapping(target = "isVpn", source = "vpn")
    @Mapping(target = "isTor", source = "tor")
    @Mapping(target = "isProxy", source = "proxy")
    @Mapping(target = "lastAttemptAt", source = "lastAttempt")
    SecurityLogMetricsDto.SuspiciousIpDto toSuspiciousIpDto(AuthLogAggregation source);

    List<SecurityLogMetricsDto.SuspiciousIpDto> toSuspiciousIpDtoList(List<AuthLogAggregation> sources);

    // ============================================
    // Referral Event Mappings
    // ============================================

    @Mapping(target = "referrerId", source = "referrerId")
    @Mapping(target = "totalReferrals", source = "totalReferrals")
    @Mapping(target = "successfulReferrals", source = "successfulReferrals")
    @Mapping(target = "fraudulentReferrals", source = "fraudulentReferrals")
    @Mapping(target = "earningsTotal", source = "totalEarnings")
    @Mapping(target = "fraudRate", expression = "java(calculateRate(source.getFraudulentReferrals(), source.getTotalReferrals()))")
    ReferralFraudMetricsDto.FraudulentReferrerDto toFraudulentReferrerDto(ReferralAggregation source);

    List<ReferralFraudMetricsDto.FraudulentReferrerDto> toFraudulentReferrerDtoList(List<ReferralAggregation> sources);

    // ============================================
    // Order History Mappings
    // ============================================

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "totalOrders", source = "totalOrders")
    @Mapping(target = "refundedOrders", source = "refundedOrders")
    @Mapping(target = "refundRate", expression = "java(calculateRate(source.getRefundedOrders(), source.getTotalOrders()))")
    @Mapping(target = "totalRefundAmount", source = "refundAmount")
    BehavioralFraudMetricsDto.RefundAbuserDto toRefundAbuserDto(OrderHistoryAggregation source);

    List<BehavioralFraudMetricsDto.RefundAbuserDto> toRefundAbuserDtoList(List<OrderHistoryAggregation> sources);

    // ============================================
    // Helper Methods
    // ============================================

    default double calculateRate(Long count, Long total) {
        if (total == null || total == 0) {
            return 0.0;
        }
        return Math.round(count * 10000.0 / total) / 100.0;
    }

    default double calculateRate(Integer count, Integer total) {
        if (total == null || total == 0) {
            return 0.0;
        }
        return Math.round(count * 10000.0 / total) / 100.0;
    }

    // ============================================
    // Aggregation Support Classes
    // ============================================

    /**
     * Aggregation class for payment attempt statistics.
     */
    class PaymentAttemptAggregation {
        private Long userId;
        private Long failureCount;
        private Long totalCount;
        private String failureReason;
        private Integer riskScore;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getFailureCount() { return failureCount; }
        public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        public Integer getRiskScore() { return riskScore; }
        public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    }

    /**
     * Aggregation class for auth log statistics.
     */
    class AuthLogAggregation {
        private String ipAddress;
        private Long failedCount;
        private Long uniqueUsers;
        private Boolean vpn;
        private Boolean tor;
        private Boolean proxy;
        private LocalDateTime lastAttempt;
        private Integer threatScore;

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public Long getFailedCount() { return failedCount; }
        public void setFailedCount(Long failedCount) { this.failedCount = failedCount; }
        public Long getUniqueUsers() { return uniqueUsers; }
        public void setUniqueUsers(Long uniqueUsers) { this.uniqueUsers = uniqueUsers; }
        public Boolean getVpn() { return vpn; }
        public void setVpn(Boolean vpn) { this.vpn = vpn; }
        public Boolean getTor() { return tor; }
        public void setTor(Boolean tor) { this.tor = tor; }
        public Boolean getProxy() { return proxy; }
        public void setProxy(Boolean proxy) { this.proxy = proxy; }
        public LocalDateTime getLastAttempt() { return lastAttempt; }
        public void setLastAttempt(LocalDateTime lastAttempt) { this.lastAttempt = lastAttempt; }
        public Integer getThreatScore() { return threatScore; }
        public void setThreatScore(Integer threatScore) { this.threatScore = threatScore; }
    }

    /**
     * Aggregation class for referral statistics.
     */
    class ReferralAggregation {
        private Long referrerId;
        private Long totalReferrals;
        private Long successfulReferrals;
        private Long fraudulentReferrals;
        private java.math.BigDecimal totalEarnings;
        private Integer riskScore;

        public Long getReferrerId() { return referrerId; }
        public void setReferrerId(Long referrerId) { this.referrerId = referrerId; }
        public Long getTotalReferrals() { return totalReferrals; }
        public void setTotalReferrals(Long totalReferrals) { this.totalReferrals = totalReferrals; }
        public Long getSuccessfulReferrals() { return successfulReferrals; }
        public void setSuccessfulReferrals(Long successfulReferrals) { this.successfulReferrals = successfulReferrals; }
        public Long getFraudulentReferrals() { return fraudulentReferrals; }
        public void setFraudulentReferrals(Long fraudulentReferrals) { this.fraudulentReferrals = fraudulentReferrals; }
        public java.math.BigDecimal getTotalEarnings() { return totalEarnings; }
        public void setTotalEarnings(java.math.BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }
        public Integer getRiskScore() { return riskScore; }
        public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    }

    /**
     * Aggregation class for order history statistics.
     */
    class OrderHistoryAggregation {
        private Long userId;
        private Long totalOrders;
        private Long refundedOrders;
        private java.math.BigDecimal refundAmount;
        private Integer riskScore;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
        public Long getRefundedOrders() { return refundedOrders; }
        public void setRefundedOrders(Long refundedOrders) { this.refundedOrders = refundedOrders; }
        public java.math.BigDecimal getRefundAmount() { return refundAmount; }
        public void setRefundAmount(java.math.BigDecimal refundAmount) { this.refundAmount = refundAmount; }
        public Integer getRiskScore() { return riskScore; }
        public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    }
}

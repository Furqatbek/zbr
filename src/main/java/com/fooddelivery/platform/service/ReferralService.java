package com.fooddelivery.platform.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.util.SlugUtils;
import com.fooddelivery.platform.dto.ReferralDto;
import com.fooddelivery.platform.entity.Referral;
import com.fooddelivery.platform.entity.ReferralStatus;
import com.fooddelivery.platform.repository.ReferralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for referral program operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private final ReferralRepository referralRepository;

    /**
     * Absolute, shareable base URL. The link is pasted into messengers, so it
     * must be absolute and on the canonical apex — a relative path is unusable
     * and www. only 308-redirects. Was hardcoded to app.fooddelivery.com, a
     * domain this platform does not own, so every invite link was dead.
     */
    @org.springframework.beans.factory.annotation.Value("${app.public-url:https://zbrr.uz}")
    private String publicBaseUrl;
    private final UserService userService;

    @Value("${app.referral.reward-amount:10.00}")
    private BigDecimal rewardAmount;

    @Value("${app.referral.expiry-days:30}")
    private int expiryDays;

    /**
     * Generate a new referral code for a user.
     */
    @Transactional
    public ReferralDto generateReferralCode(Long userId) {
        User user = userService.getUserEntityById(userId);

        // Check for existing active referral
        LocalDateTime now = LocalDateTime.now();
        referralRepository.findActiveReferralByUser(userId, now)
                .ifPresent(r -> {
                    throw new BusinessException("You already have an active referral code: " + r.getCode());
                });

        // Generate unique code
        String code;
        do {
            code = SlugUtils.generateReferralCode();
        } while (referralRepository.existsByCode(code));

        Referral referral = Referral.builder()
                .code(code)
                .referrer(user)
                .rewardAmount(rewardAmount)
                .expiresAt(LocalDateTime.now().plusDays(expiryDays))
                .build();

        referral = referralRepository.save(referral);
        log.info("Referral code generated: {} for user: {}", code, userId);

        return toDto(referral);
    }

    /**
     * Process a referral code during signup.
     */
    @Transactional
    public void processReferral(String code, Long referredUserId) {
        Referral referral = referralRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", "code", code));

        if (!referral.isValid()) {
            throw new BusinessException("Referral code is invalid or expired");
        }

        if (referralRepository.existsByReferredId(referredUserId)) {
            throw new BusinessException("User has already used a referral code");
        }

        if (referral.getReferrer().getId().equals(referredUserId)) {
            throw new BusinessException("Cannot use your own referral code");
        }

        User referredUser = userService.getUserEntityById(referredUserId);
        referral.setReferred(referredUser);
        referral.setStatus(ReferralStatus.USED);
        referralRepository.save(referral);

        log.info("Referral {} used by user {}", code, referredUserId);
    }

    /**
     * Complete a referral (e.g., after first order).
     */
    @Transactional
    public void completeReferral(Long referredUserId) {
        Referral referral = referralRepository.findByReferredId(referredUserId)
                .orElse(null);

        if (referral == null || referral.getStatus() != ReferralStatus.USED) {
            return;
        }

        referral.complete();
        referral.setReferrerRewarded(true);
        referral.setReferredRewarded(true);
        referralRepository.save(referral);

        log.info("Referral {} completed. Rewards: referrer={}, referred={}",
                referral.getCode(), rewardAmount, rewardAmount);

        // In production: Credit rewards to user wallets/accounts
    }

    /**
     * Get referral by code.
     */
    @Transactional(readOnly = true)
    public ReferralDto getReferralByCode(String code) {
        Referral referral = referralRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", "code", code));
        return toDto(referral);
    }

    /**
     * Get referrals by user.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReferralDto> getReferralsByUser(Long userId, Pageable pageable) {
        Page<Referral> referrals = referralRepository.findByReferrerId(userId, pageable);
        return PagedResponse.from(referrals, referrals.getContent().stream()
                .map(this::toDto)
                .toList());
    }

    /**
     * Get all referrals (admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReferralDto> getAllReferrals(Pageable pageable) {
        Page<Referral> referrals = referralRepository.findAll(pageable);
        return PagedResponse.from(referrals, referrals.getContent().stream()
                .map(this::toDto)
                .toList());
    }

    /**
     * Get user's referral statistics.
     */
    @Transactional(readOnly = true)
    public ReferralStatsDto getUserReferralStats(Long userId) {
        long completedCount = referralRepository.countCompletedByReferrer(userId);
        BigDecimal totalRewards = referralRepository.sumRewardsByReferrer(userId);

        return ReferralStatsDto.builder()
                .userId(userId)
                .completedReferrals(completedCount)
                .totalRewards(totalRewards != null ? totalRewards : BigDecimal.ZERO)
                .rewardPerReferral(rewardAmount)
                .build();
    }

    /**
     * Expire old referrals.
     */
    @Transactional
    public int expireOldReferrals() {
        List<Referral> expired = referralRepository.findExpiredReferrals(LocalDateTime.now());
        int count = 0;
        for (Referral referral : expired) {
            referral.setStatus(ReferralStatus.EXPIRED);
            referralRepository.save(referral);
            count++;
        }
        log.info("Expired {} referral codes", count);
        return count;
    }

    private ReferralDto toDto(Referral referral) {
        return ReferralDto.builder()
                .id(referral.getId())
                .code(referral.getCode())
                .referrerId(referral.getReferrer().getId())
                .referrerName(referral.getReferrer().getFullName())
                .referredId(referral.getReferred() != null ? referral.getReferred().getId() : null)
                .referredName(referral.getReferred() != null ? referral.getReferred().getFullName() : null)
                .status(referral.getStatus())
                .rewardAmount(referral.getRewardAmount())
                .currency(referral.getCurrency())
                .referrerRewarded(referral.getReferrerRewarded())
                .referredRewarded(referral.getReferredRewarded())
                .expiresAt(referral.getExpiresAt())
                .completedAt(referral.getCompletedAt())
                .createdAt(referral.getCreatedAt())
                .build();
    }

    /**
     * Get referral info for the mobile app (code + stats in one call).
     */
    // NOT readOnly: this now creates a code on first read, and a read-only
    // transaction would reject the insert.
    @Transactional
    public MyReferralInfoDto getMyReferralInfo(Long userId) {
        // Generate on first read rather than returning nothing. A null code is
        // dropped from the response entirely by Jackson's non_null inclusion, so
        // the client saw a payload with no referralCode field at all and could
        // not tell "you have no code yet" from "the field does not exist".
        String referralCode = referralRepository.findActiveReferralByUser(userId, LocalDateTime.now())
                .map(Referral::getCode)
                .orElseGet(() -> generateReferralCode(userId).getCode());

        long totalReferrals = referralRepository.countCompletedByReferrer(userId);
        BigDecimal earnedCredits = referralRepository.sumRewardsByReferrer(userId);
        long pendingCount = referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.PENDING);
        BigDecimal pendingCredits = BigDecimal.valueOf(pendingCount).multiply(rewardAmount);

        return MyReferralInfoDto.builder()
                .referralCode(referralCode)
                .referralLink(publicBaseUrl + "/invite/" + referralCode)
                .totalReferrals(totalReferrals)
                .earnedCredits(earnedCredits != null ? earnedCredits : BigDecimal.ZERO)
                .pendingCredits(pendingCredits)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class MyReferralInfoDto {
        private String referralCode;
        private String referralLink;
        private long totalReferrals;
        private BigDecimal earnedCredits;
        private BigDecimal pendingCredits;
    }

    @lombok.Data
    @lombok.Builder
    public static class ReferralStatsDto {
        private Long userId;
        private long completedReferrals;
        private BigDecimal totalRewards;
        private BigDecimal rewardPerReferral;
    }
}

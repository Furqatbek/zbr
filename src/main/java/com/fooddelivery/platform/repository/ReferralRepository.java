package com.fooddelivery.platform.repository;

import com.fooddelivery.platform.entity.Referral;
import com.fooddelivery.platform.entity.ReferralStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Referral entity operations.
 */
@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    Optional<Referral> findByCode(String code);

    Optional<Referral> findByCodeAndStatus(String code, ReferralStatus status);

    List<Referral> findByReferrerId(Long referrerId);

    Page<Referral> findByReferrerId(Long referrerId, Pageable pageable);

    Optional<Referral> findByReferredId(Long referredId);

    boolean existsByCode(String code);

    boolean existsByReferredId(Long referredId);

    @Query("SELECT r FROM Referral r WHERE r.referrer.id = :userId AND r.status = 'PENDING' " +
            "AND (r.expiresAt IS NULL OR r.expiresAt > :now)")
    Optional<Referral> findActiveReferralByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(r) FROM Referral r WHERE r.referrer.id = :userId AND r.status = 'COMPLETED'")
    long countCompletedByReferrer(@Param("userId") Long userId);

    long countByReferrerIdAndStatus(Long referrerId, com.fooddelivery.platform.entity.ReferralStatus status);

    @Query("SELECT SUM(r.rewardAmount) FROM Referral r WHERE r.referrer.id = :userId AND r.referrerRewarded = true")
    java.math.BigDecimal sumRewardsByReferrer(@Param("userId") Long userId);

    Page<Referral> findByStatus(ReferralStatus status, Pageable pageable);

    @Query("SELECT r FROM Referral r WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    List<Referral> findExpiredReferrals(@Param("now") LocalDateTime now);
}

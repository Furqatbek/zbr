package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for OTP code operations.
 */
@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    /**
     * Find the latest valid OTP for a phone number.
     */
    @Query(value = "SELECT * FROM otp_codes WHERE phone = :phone " +
           "AND is_used = false AND expires_at > :now " +
           "ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<OtpCode> findLatestValidOtp(@Param("phone") String phone, @Param("now") LocalDateTime now);

    /**
     * Find OTP by phone and code.
     */
    @Query("SELECT o FROM OtpCode o WHERE o.phone = :phone AND o.code = :code " +
           "AND o.isUsed = false AND o.expiresAt > :now")
    Optional<OtpCode> findByPhoneAndCode(@Param("phone") String phone,
                                          @Param("code") String code,
                                          @Param("now") LocalDateTime now);

    /**
     * Count recent OTPs for rate limiting.
     */
    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.phone = :phone " +
           "AND o.createdAt > :since")
    long countRecentOtps(@Param("phone") String phone, @Param("since") LocalDateTime since);

    /**
     * Invalidate all unused OTPs for a phone number.
     */
    @Modifying
    @Query("UPDATE OtpCode o SET o.isUsed = true WHERE o.phone = :phone AND o.isUsed = false")
    void invalidateAllForPhone(@Param("phone") String phone);

    /**
     * Delete expired OTPs (cleanup).
     */
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :before")
    int deleteExpiredOtps(@Param("before") LocalDateTime before);
}

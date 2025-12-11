package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for PasswordResetToken entity operations.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user.id = :userId AND prt.used = false " +
            "AND prt.expiresAt > :now ORDER BY prt.createdAt DESC")
    Optional<PasswordResetToken> findValidTokenByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true, prt.usedAt = :now " +
            "WHERE prt.user.id = :userId AND prt.used = false")
    int invalidateUserTokens(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :before")
    int deleteExpiredTokens(@Param("before") LocalDateTime before);
}

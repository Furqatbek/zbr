package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefreshToken entity operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, rt.revokedReason = :reason " +
            "WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllUserTokens(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("reason") String reason);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, rt.revokedReason = 'expired' " +
            "WHERE rt.expiresAt < :now AND rt.revoked = false")
    int revokeExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :before")
    int deleteExpiredTokens(@Param("before") LocalDateTime before);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false")
    int countActiveTokensByUser(@Param("userId") Long userId);

    boolean existsByTokenAndRevokedFalse(String token);
}

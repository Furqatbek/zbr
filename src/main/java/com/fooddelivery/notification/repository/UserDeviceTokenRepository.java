package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for UserDeviceToken entity.
 */
@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    /**
     * Find all active device tokens for a user.
     */
    List<UserDeviceToken> findByUserIdAndActiveTrue(Long userId);

    /**
     * Find device token by token string.
     */
    Optional<UserDeviceToken> findByDeviceToken(String deviceToken);

    /**
     * Check if a device token exists for a user.
     */
    boolean existsByUserIdAndDeviceToken(Long userId, String deviceToken);

    /**
     * Deactivate all tokens for a user (e.g., on logout from all devices).
     */
    @Modifying
    @Query("UPDATE UserDeviceToken t SET t.active = false WHERE t.userId = :userId")
    int deactivateAllForUser(@Param("userId") Long userId);

    /**
     * Deactivate a specific token.
     */
    @Modifying
    @Query("UPDATE UserDeviceToken t SET t.active = false WHERE t.deviceToken = :token")
    int deactivateToken(@Param("token") String deviceToken);

    /**
     * Update last used timestamp.
     */
    @Modifying
    @Query("UPDATE UserDeviceToken t SET t.lastUsedAt = :timestamp WHERE t.deviceToken = :token")
    int updateLastUsed(@Param("token") String deviceToken, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Delete inactive tokens older than specified date.
     */
    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.active = false AND t.updatedAt < :before")
    int deleteInactiveTokensOlderThan(@Param("before") LocalDateTime before);

    /**
     * Find all tokens for a user.
     */
    List<UserDeviceToken> findByUserId(Long userId);
}

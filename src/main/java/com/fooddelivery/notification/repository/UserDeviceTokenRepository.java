package com.fooddelivery.notification.repository;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.notification.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
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

    /** One row per physical device: registration upserts on (user, deviceId). */
    Optional<UserDeviceToken> findByUserIdAndDeviceId(Long userId, String deviceId);

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
     * Deactivate a specific token belonging to the given user.
     *
     * <p>Scoped by userId deliberately. Keyed on the token alone, any
     * authenticated caller who learned another user's device token could
     * silently switch off their push notifications — for a courier that is
     * missed order offers, for a vendor missed orders.
     */
    @Modifying
    @Query("UPDATE UserDeviceToken t SET t.active = false "
            + "WHERE t.deviceToken = :token AND t.userId = :userId")
    int deactivateToken(@Param("userId") Long userId, @Param("token") String deviceToken);

    /**
     * Deactivate a token the push transport itself rejected (APNs
     * BadDeviceToken, FCM UNREGISTERED, an Expo DeviceNotRegistered receipt).
     *
     * <p>Unscoped ON PURPOSE, and only for that: the provider has told us this
     * token is dead, and there is no user in context. Never call this for a
     * user-initiated removal — use {@link #deactivateToken(Long, String)}, or a
     * caller can switch off push for an account that is not theirs.
     */
    @Modifying
    @Query("UPDATE UserDeviceToken t SET t.active = false WHERE t.deviceToken = :token")
    int deactivateRejectedToken(@Param("token") String deviceToken);

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

    /**
     * Find all active device tokens for users with a specific role.
     * Used for broadcast push notifications to all users of a role.
     */
    @Query("SELECT t FROM UserDeviceToken t " +
           "JOIN User u ON t.userId = u.id " +
           "WHERE u.role = :role AND t.active = true AND u.status = 'ACTIVE'")
    List<UserDeviceToken> findActiveTokensByUserRole(@Param("role") Role role);

    /**
     * Find all active device tokens for users with any of the specified roles.
     * Used for broadcast push notifications to multiple roles.
     */
    @Query("SELECT t FROM UserDeviceToken t " +
           "JOIN User u ON t.userId = u.id " +
           "WHERE u.role IN :roles AND t.active = true AND u.status = 'ACTIVE'")
    List<UserDeviceToken> findActiveTokensByUserRoles(@Param("roles") Collection<Role> roles);

    /**
     * Find all active device tokens for all active users.
     * Used for system-wide broadcast push notifications.
     */
    @Query("SELECT t FROM UserDeviceToken t " +
           "JOIN User u ON t.userId = u.id " +
           "WHERE t.active = true AND u.status = 'ACTIVE'")
    List<UserDeviceToken> findAllActiveTokens();
}

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
     * One row per app per physical device.
     *
     * <p>Two of our apps on one phone report the same {@code deviceId} — Android's
     * ANDROID_ID is per signing key and iOS's identifierForVendor is per vendor,
     * so apps from the same developer share both. Keying on (user, deviceId)
     * alone therefore made the second app's registration overwrite the first
     * app's row, leaving one token where there are two apps.
     *
     * <p>Split into two derived queries rather than one with a nullable
     * parameter: Postgres cannot infer the type of a null bind parameter, and
     * {@code appId = null} never matches anything in SQL regardless.
     */
    Optional<UserDeviceToken> findByUserIdAndDeviceIdAndAppId(Long userId, String deviceId, String appId);

    /** Legacy rows, registered before the apps sent an appId. */
    Optional<UserDeviceToken> findByUserIdAndDeviceIdAndAppIdIsNull(Long userId, String deviceId);

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
    @Query("UPDATE UserDeviceToken t SET t.active = false "
            + "WHERE t.userId = :userId AND t.deviceId = :deviceId")
    int deactivateByDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

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
    // A user's roles live in TWO places: the singular User.role column and the
    // user_roles collection. Matching only the column meant every broadcast
    // reached nobody, because the paths that grant COURIER and RESTAURANT_OWNER
    // add to the COLLECTION and leave the column at its CONSUMER default. The
    // LEFT JOIN plus OR covers both; DISTINCT because the join multiplies rows
    // for a user holding several roles.
    @Query("SELECT DISTINCT t FROM UserDeviceToken t " +
           "JOIN User u ON t.userId = u.id " +
           "LEFT JOIN u.roles r " +
           "WHERE t.active = true AND u.status = 'ACTIVE' " +
           "AND (u.role IN :roles OR r IN :roles)")
    List<UserDeviceToken> findActiveTokensByUserRoles(@Param("roles") Collection<Role> roles);

    /**
     * Devices of couriers who can actually take the job right now.
     *
     * <p>Broadcasting a delivery offer to every courier wakes people who are
     * off shift for work they cannot accept, and their app suppresses the offer
     * anyway — so the push is pure noise and pure battery. Verified because an
     * unapproved courier cannot see orders at all.
     */
    @Query("SELECT DISTINCT t FROM UserDeviceToken t " +
           "JOIN Courier c ON c.user.id = t.userId " +
           "JOIN User u ON u.id = t.userId " +
           "WHERE t.active = true AND u.status = 'ACTIVE' " +
           "AND c.verified = true " +
           "AND c.status = com.fooddelivery.courier.entity.CourierStatus.AVAILABLE")
    List<UserDeviceToken> findActiveTokensForAvailableCouriers();

    /**
     * Find all active device tokens for all active users.
     * Used for system-wide broadcast push notifications.
     */
    @Query("SELECT t FROM UserDeviceToken t " +
           "JOIN User u ON t.userId = u.id " +
           "WHERE t.active = true AND u.status = 'ACTIVE'")
    List<UserDeviceToken> findAllActiveTokens();
}

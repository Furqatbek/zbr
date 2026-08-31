package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmailOrPhone(String email, String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE u.status = :status")
    Page<User> findByStatus(@Param("status") UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    Page<User> findByRole(@Param("role") Role role, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.status = :status")
    Page<User> findByRoleAndStatus(@Param("role") Role role, @Param("status") UserStatus status, Pageable pageable);

    /**
     * Admin user search.
     *
     * <p>Case-insensitive, and matches the full name as well as its halves —
     * the previous version compared with a bare LIKE against firstName and
     * lastName separately, so "asad" missed "Asad" and "Asad Karimov" matched
     * nobody at all. Both made the admin panel's owner picker look empty when
     * the user was right there.
     *
     * <p>{@code phoneTerm} is the caller's query reduced to digits, so a pasted
     * "+998 90 123 45 67" matches the stored "998901234567". It is a separate
     * parameter rather than a second use of {@code search} because stripping
     * the punctuation out of a NAME would corrupt it. See
     * {@code UserService.phoneTerm} for when the reduction applies — a query
     * containing letters is passed through whole, so it contributes nothing
     * here rather than matching every phone.
     */
    @Query("SELECT u FROM User u WHERE "
            + "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) "
            + "   LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR u.phone LIKE CONCAT('%', :phoneTerm, '%')")
    Page<User> searchUsers(@Param("search") String search,
                           @Param("phoneTerm") String phoneTerm,
                           Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countNewUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role")
    long countByRole(@Param("role") Role role);

    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") UserStatus status);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt, u.failedLoginAttempts = 0 WHERE u.id = :id")
    int updateLastLogin(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * Touch last-online without loading or versioning the row.
     *
     * <p>A bulk UPDATE, not {@code save()}, on purpose: this runs on ordinary
     * authenticated traffic, and an entity save would bump the @Version column,
     * turning every concurrent profile edit into an OptimisticLockException.
     * The guard also keeps it monotonic, so an out-of-order write (a request
     * that queued behind a later one) cannot move the timestamp backwards.
     */
    @Modifying
    @Query("UPDATE User u SET u.lastSeenAt = :seenAt "
            + "WHERE u.id = :id AND (u.lastSeenAt IS NULL OR u.lastSeenAt < :seenAt)")
    int updateLastSeenAt(@Param("id") Long id, @Param("seenAt") LocalDateTime seenAt);

    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil < :now")
    List<User> findExpiredLocks(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = null, u.failedLoginAttempts = 0 WHERE u.id IN :ids")
    int unlockUsers(@Param("ids") List<Long> ids);
}

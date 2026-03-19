package com.fooddelivery.analytics.repository;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for User analytics with optimized queries.
 */
@Repository
public interface AnalyticsUserRepository extends JpaRepository<User, Long> {

    // ============ User Registration Queries ============

    /**
     * Count new user registrations since a given time.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    Long countNewUsersSince(@Param("since") LocalDateTime since);

    /**
     * Count new user registrations between two dates.
     */
    @Query("SELECT COUNT(u) FROM User u " +
            "WHERE u.createdAt >= :startDate AND u.createdAt < :endDate")
    Long countNewUsersBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count active users (status = ACTIVE).
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    Long countUsersByStatus(@Param("status") UserStatus status);

    /**
     * Get daily registration counts for a date range.
     * Returns [date, count].
     */
    @Query(value = "SELECT DATE(created_at) as reg_date, COUNT(*) as count " +
            "FROM users " +
            "WHERE created_at >= :startDate AND created_at < :endDate " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY reg_date", nativeQuery = true)
    List<Object[]> getDailyRegistrationsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ============ User Activity Queries ============

    /**
     * Count users who logged in since a given time.
     */
    @Query("SELECT COUNT(u) FROM User u " +
            "WHERE u.lastLoginAt IS NOT NULL AND u.lastLoginAt >= :since")
    Long countUsersLoggedInSince(@Param("since") LocalDateTime since);

    /**
     * Count distinct users with activity (based on last login).
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u " +
            "WHERE u.lastLoginAt >= :since AND u.status = 'ACTIVE'")
    Long countActiveUsersSince(@Param("since") LocalDateTime since);

    // ============ Referral Queries ============

    /**
     * Count users who signed up with a referral code.
     * This assumes referral tracking is stored in user metadata or referrals table.
     */
    @Query(value = "SELECT COUNT(DISTINCT r.referred_user_id) " +
            "FROM referrals r " +
            "WHERE r.created_at >= :since AND r.status = 'COMPLETED'",
            nativeQuery = true)
    Long countReferralSignupsSince(@Param("since") LocalDateTime since);

    /**
     * Get user registration by cohort (month).
     * Returns [cohortMonth, userCount].
     */
    @Query(value = "SELECT TO_CHAR(created_at, 'YYYY-MM') as cohort_month, COUNT(*) as user_count " +
            "FROM users " +
            "WHERE created_at >= :since " +
            "GROUP BY TO_CHAR(created_at, 'YYYY-MM') " +
            "ORDER BY cohort_month", nativeQuery = true)
    List<Object[]> getUserRegistrationsByCohort(@Param("since") LocalDateTime since);

    // ============ User Retention Queries ============

    /**
     * Get retention cohort data.
     * Returns monthly cohorts with retention at 30, 60, 90 days.
     */
    @Query(value = "WITH cohorts AS (" +
            "  SELECT " +
            "    u.id as user_id, " +
            "    TO_CHAR(u.created_at, 'YYYY-MM') as cohort_month, " +
            "    u.created_at as registration_date " +
            "  FROM users u " +
            "  WHERE u.created_at >= :since " +
            "), " +
            "user_activity AS (" +
            "  SELECT " +
            "    c.user_id, " +
            "    c.cohort_month, " +
            "    c.registration_date, " +
            "    MAX(o.created_at) as last_order_date " +
            "  FROM cohorts c " +
            "  LEFT JOIN orders o ON c.user_id = o.consumer_id " +
            "  GROUP BY c.user_id, c.cohort_month, c.registration_date " +
            ") " +
            "SELECT " +
            "  cohort_month, " +
            "  COUNT(*) as cohort_size, " +
            "  COUNT(CASE WHEN last_order_date >= registration_date + INTERVAL '30 days' THEN 1 END) as active_30d, " +
            "  COUNT(CASE WHEN last_order_date >= registration_date + INTERVAL '60 days' THEN 1 END) as active_60d, " +
            "  COUNT(CASE WHEN last_order_date >= registration_date + INTERVAL '90 days' THEN 1 END) as active_90d " +
            "FROM user_activity " +
            "GROUP BY cohort_month " +
            "ORDER BY cohort_month", nativeQuery = true)
    List<Object[]> getUserRetentionCohorts(@Param("since") LocalDateTime since);

    // ============ Activation Time Queries ============

    /**
     * Get activation time distribution.
     * Returns buckets: < 1 hour, 1-24 hours, 1-7 days, > 7 days.
     */
    @Query(value = "WITH activation_times AS (" +
            "  SELECT " +
            "    u.id, " +
            "    EXTRACT(EPOCH FROM (MIN(o.created_at) - u.created_at)) / 3600 as hours_to_first_order " +
            "  FROM users u " +
            "  JOIN orders o ON u.id = o.consumer_id " +
            "  WHERE u.created_at >= :since " +
            "  GROUP BY u.id, u.created_at " +
            ") " +
            "SELECT " +
            "  CASE " +
            "    WHEN hours_to_first_order < 1 THEN '< 1 hour' " +
            "    WHEN hours_to_first_order < 24 THEN '1-24 hours' " +
            "    WHEN hours_to_first_order < 168 THEN '1-7 days' " +
            "    ELSE '> 7 days' " +
            "  END as bucket, " +
            "  COUNT(*) as user_count " +
            "FROM activation_times " +
            "GROUP BY " +
            "  CASE " +
            "    WHEN hours_to_first_order < 1 THEN '< 1 hour' " +
            "    WHEN hours_to_first_order < 24 THEN '1-24 hours' " +
            "    WHEN hours_to_first_order < 168 THEN '1-7 days' " +
            "    ELSE '> 7 days' " +
            "  END " +
            "ORDER BY " +
            "  MIN(CASE " +
            "    WHEN hours_to_first_order < 1 THEN 1 " +
            "    WHEN hours_to_first_order < 24 THEN 2 " +
            "    WHEN hours_to_first_order < 168 THEN 3 " +
            "    ELSE 4 " +
            "  END)", nativeQuery = true)
    List<Object[]> getActivationTimeDistribution(@Param("since") LocalDateTime since);
}

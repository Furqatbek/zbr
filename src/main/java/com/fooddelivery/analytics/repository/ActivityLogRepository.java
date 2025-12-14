package com.fooddelivery.analytics.repository;

import com.fooddelivery.analytics.model.ActivityEventType;
import com.fooddelivery.analytics.model.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ActivityLog entity with optimized queries for analytics metrics.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // ============ DAU/WAU/MAU Queries ============

    /**
     * Count distinct active users since a given time.
     * Uses index on (user_id, created_at) for optimization.
     *
     * @param since Start time for the query
     * @return Count of distinct active users
     */
    @Query("SELECT COUNT(DISTINCT a.userId) FROM ActivityLog a " +
            "WHERE a.userId IS NOT NULL AND a.createdAt >= :since")
    Long countDistinctActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * Count distinct active users for a specific date range.
     *
     * @param startDate Start of the date range
     * @param endDate   End of the date range
     * @return Count of distinct active users
     */
    @Query("SELECT COUNT(DISTINCT a.userId) FROM ActivityLog a " +
            "WHERE a.userId IS NOT NULL " +
            "AND a.createdAt >= :startDate AND a.createdAt < :endDate")
    Long countDistinctActiveUsersBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ============ Conversion Funnel Queries ============

    /**
     * Count events by type since a given time.
     * Used for conversion funnel calculation.
     *
     * @param eventType Type of event to count
     * @param since     Start time for the query
     * @return Count of events
     */
    @Query("SELECT COUNT(a) FROM ActivityLog a " +
            "WHERE a.eventType = :eventType AND a.createdAt >= :since")
    Long countEventsSince(
            @Param("eventType") ActivityEventType eventType,
            @Param("since") LocalDateTime since);

    /**
     * Count distinct users who performed a specific event type.
     *
     * @param eventType Type of event
     * @param since     Start time
     * @return Count of distinct users
     */
    @Query("SELECT COUNT(DISTINCT a.userId) FROM ActivityLog a " +
            "WHERE a.eventType = :eventType AND a.userId IS NOT NULL " +
            "AND a.createdAt >= :since")
    Long countDistinctUsersByEventSince(
            @Param("eventType") ActivityEventType eventType,
            @Param("since") LocalDateTime since);

    /**
     * Get funnel event counts for multiple event types in one query.
     * Returns array of [eventType, count] for each funnel step.
     */
    @Query("SELECT a.eventType, COUNT(DISTINCT a.userId) FROM ActivityLog a " +
            "WHERE a.eventType IN :eventTypes AND a.userId IS NOT NULL " +
            "AND a.createdAt >= :since " +
            "GROUP BY a.eventType")
    List<Object[]> countDistinctUsersByEventTypesSince(
            @Param("eventTypes") List<ActivityEventType> eventTypes,
            @Param("since") LocalDateTime since);

    /**
     * Count events by type and restaurant.
     *
     * @param eventType    Type of event
     * @param restaurantId Restaurant ID
     * @param since        Start time
     * @return Count of events
     */
    @Query("SELECT COUNT(a) FROM ActivityLog a " +
            "WHERE a.eventType = :eventType " +
            "AND a.restaurantId = :restaurantId " +
            "AND a.createdAt >= :since")
    Long countEventsByRestaurantSince(
            @Param("eventType") ActivityEventType eventType,
            @Param("restaurantId") Long restaurantId,
            @Param("since") LocalDateTime since);

    // ============ Activity Tracking Queries ============

    /**
     * Get recent activities for a user.
     *
     * @param userId User ID
     * @param limit  Max number of activities
     * @return List of recent activities
     */
    @Query("SELECT a FROM ActivityLog a WHERE a.userId = :userId " +
            "ORDER BY a.createdAt DESC")
    List<ActivityLog> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Check if user has performed a specific event type.
     *
     * @param userId    User ID
     * @param eventType Event type
     * @return true if user has performed the event
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM ActivityLog a " +
            "WHERE a.userId = :userId AND a.eventType = :eventType")
    boolean hasUserPerformedEvent(
            @Param("userId") Long userId,
            @Param("eventType") ActivityEventType eventType);

    /**
     * Count referral events since a given time.
     *
     * @param since Start time
     * @return Count of referral signups
     */
    @Query("SELECT COUNT(a) FROM ActivityLog a " +
            "WHERE a.eventType = 'REFERRAL_USED' AND a.createdAt >= :since")
    Long countReferralSignupsSince(@Param("since") LocalDateTime since);

    // ============ Hourly Distribution Queries ============

    /**
     * Get event count distribution by hour for a specific date.
     * Returns array of [hour, count].
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM created_at) as hour, COUNT(*) as count " +
            "FROM activity_logs " +
            "WHERE event_type = :eventType " +
            "AND created_at >= :startDate AND created_at < :endDate " +
            "GROUP BY EXTRACT(HOUR FROM created_at) " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getEventDistributionByHour(
            @Param("eventType") String eventType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ============ Session Analytics ============

    /**
     * Count unique sessions since a given time.
     *
     * @param since Start time
     * @return Count of unique sessions
     */
    @Query("SELECT COUNT(DISTINCT a.sessionId) FROM ActivityLog a " +
            "WHERE a.sessionId IS NOT NULL AND a.createdAt >= :since")
    Long countUniqueSessionsSince(@Param("since") LocalDateTime since);

    /**
     * Get average session duration in milliseconds.
     */
    @Query(value = "SELECT AVG(session_duration) FROM (" +
            "SELECT session_id, MAX(duration_ms) as session_duration " +
            "FROM activity_logs " +
            "WHERE session_id IS NOT NULL AND duration_ms IS NOT NULL " +
            "AND created_at >= :since " +
            "GROUP BY session_id) as sessions", nativeQuery = true)
    Double getAverageSessionDurationSince(@Param("since") LocalDateTime since);
}

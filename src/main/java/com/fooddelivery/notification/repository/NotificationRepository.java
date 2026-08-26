package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for notification persistence operations.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    // ===== Basic Queries =====

    /**
     * Find notifications by user ID with pagination.
     */
    Page<Notification> findByUserIdAndDismissedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find notifications by role with pagination.
     */
    Page<Notification> findByRoleAndDismissedFalseOrderByCreatedAtDesc(NotificationRole role, Pageable pageable);

    /**
     * Find notifications by user ID and role.
     */
    Page<Notification> findByUserIdAndRoleAndDismissedFalseOrderByCreatedAtDesc(
            Long userId, NotificationRole role, Pageable pageable);

    /**
     * Find unread notifications by user ID.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL " +
            "AND n.dismissed = false ORDER BY n.createdAt DESC")
    Page<Notification> findUnreadByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find unread notifications by role.
     */
    @Query("SELECT n FROM Notification n WHERE n.role = :role AND n.readAt IS NULL " +
            "AND n.dismissed = false ORDER BY n.createdAt DESC")
    Page<Notification> findUnreadByRole(@Param("role") NotificationRole role, Pageable pageable);

    // ===== Count Queries =====

    /**
     * Count total notifications by user ID.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.dismissed = false")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * Count unread notifications by user ID.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL " +
            "AND n.dismissed = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Count unread notifications by user ID and role.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.role = :role " +
            "AND n.readAt IS NULL AND n.dismissed = false")
    Long countUnreadByUserIdAndRole(@Param("userId") Long userId, @Param("role") NotificationRole role);

    /**
     * Count unread notifications by user ID and category.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.category = :category " +
            "AND n.readAt IS NULL AND n.dismissed = false")
    Long countUnreadByUserIdAndCategory(@Param("userId") Long userId, @Param("category") NotificationCategory category);

    /**
     * Count unread by user and priority.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.priority = :priority " +
            "AND n.readAt IS NULL AND n.dismissed = false")
    Long countUnreadByUserIdAndPriority(@Param("userId") Long userId, @Param("priority") NotificationPriority priority);

    /**
     * Count unread by category for user.
     */
    @Query("SELECT n.category, COUNT(n) FROM Notification n WHERE n.userId = :userId " +
            "AND n.readAt IS NULL AND n.dismissed = false GROUP BY n.category")
    List<Object[]> countUnreadByCategoryForUser(@Param("userId") Long userId);

    /**
     * Count unread by priority for user.
     */
    @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.userId = :userId " +
            "AND n.readAt IS NULL AND n.dismissed = false GROUP BY n.priority")
    List<Object[]> countUnreadByPriorityForUser(@Param("userId") Long userId);

    // ===== Update Queries =====

    /**
     * Mark notification as read.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.id = :id AND n.readAt IS NULL")
    int markAsRead(@Param("id") Long id, @Param("readAt") LocalDateTime readAt);

    /**
     * Mark all notifications as read for a user.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.userId = :userId " +
            "AND n.readAt IS NULL AND n.dismissed = false")
    int markAllAsReadForUser(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    /**
     * Mark all notifications as read for a user and role.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.userId = :userId AND n.role = :role " +
            "AND n.readAt IS NULL AND n.dismissed = false")
    int markAllAsReadForUserAndRole(@Param("userId") Long userId, @Param("role") NotificationRole role,
                                    @Param("readAt") LocalDateTime readAt);

    /**
     * Mark notifications as read by IDs.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.id IN :ids AND n.readAt IS NULL")
    int markAsReadByIds(@Param("ids") List<Long> ids, @Param("readAt") LocalDateTime readAt);

    /**
     * Narrow a caller-supplied id list to the ones that user actually owns.
     *
     * <p>The batch endpoints take arbitrary ids from the request body. Acting on
     * them directly lets any authenticated caller mark read, dismiss or DELETE
     * another user's notifications, so every batch operation filters through
     * this first.
     */
    @Query("SELECT n.id FROM Notification n WHERE n.id IN :ids AND n.userId = :userId")
    List<Long> findOwnedIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /**
     * Dismiss notification.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.dismissed = true, n.dismissedAt = :dismissedAt WHERE n.id = :id")
    int dismiss(@Param("id") Long id, @Param("dismissedAt") LocalDateTime dismissedAt);

    /**
     * Dismiss notifications by IDs.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.dismissed = true, n.dismissedAt = :dismissedAt WHERE n.id IN :ids")
    int dismissByIds(@Param("ids") List<Long> ids, @Param("dismissedAt") LocalDateTime dismissedAt);

    /**
     * Dismiss all notifications for user.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.dismissed = true, n.dismissedAt = :dismissedAt " +
            "WHERE n.userId = :userId AND n.dismissed = false")
    int dismissAllForUser(@Param("userId") Long userId, @Param("dismissedAt") LocalDateTime dismissedAt);

    // ===== Delete Queries =====

    /**
     * Delete notifications by user ID.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * Delete expired notifications.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    /**
     * Delete dismissed notifications older than a date.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.dismissed = true AND n.dismissedAt < :before")
    int deleteDismissedBefore(@Param("before") LocalDateTime before);

    /**
     * Delete old read notifications.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.readAt IS NOT NULL AND n.readAt < :before")
    int deleteReadBefore(@Param("before") LocalDateTime before);

    // ===== Filter Queries =====

    /**
     * Find notifications by order ID.
     */
    List<Notification> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * Find notifications by user and category.
     */
    Page<Notification> findByUserIdAndCategoryAndDismissedFalseOrderByCreatedAtDesc(
            Long userId, NotificationCategory category, Pageable pageable);

    /**
     * Find notifications by user and notification type.
     */
    List<Notification> findByUserIdAndNotificationTypeAndDismissedFalseOrderByCreatedAtDesc(
            Long userId, NotificationType notificationType);

    /**
     * Find high priority unread notifications.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL " +
            "AND n.dismissed = false AND n.priority IN :priorities ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notification> findHighPriorityUnread(@Param("userId") Long userId,
                                               @Param("priorities") List<NotificationPriority> priorities);

    /**
     * Find notifications by related entity.
     */
    List<Notification> findByRelatedEntityIdAndRelatedEntityTypeOrderByCreatedAtDesc(
            Long relatedEntityId, String relatedEntityType);

    /**
     * Find recent notifications for user.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.dismissed = false " +
            "AND n.createdAt > :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentForUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Find notifications by user with date range.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.dismissed = false " +
            "AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 Pageable pageable);

    /**
     * Search notifications by title or message.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.dismissed = false " +
            "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(n.message) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> searchByUserIdAndTerm(@Param("userId") Long userId,
                                              @Param("searchTerm") String searchTerm,
                                              Pageable pageable);

    // ===== Broadcast Queries =====

    /**
     * Find broadcast notifications (no specific user) by role.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId IS NULL AND n.role = :role " +
            "AND n.dismissed = false ORDER BY n.createdAt DESC")
    Page<Notification> findBroadcastsByRole(@Param("role") NotificationRole role, Pageable pageable);

    /**
     * Find all notifications for user (including role broadcasts).
     */
    @Query("SELECT n FROM Notification n WHERE (n.userId = :userId OR (n.userId IS NULL AND n.role = :role)) " +
            "AND n.dismissed = false ORDER BY n.createdAt DESC")
    Page<Notification> findAllForUserIncludingBroadcasts(@Param("userId") Long userId,
                                                          @Param("role") NotificationRole role,
                                                          Pageable pageable);

    /**
     * Count unread including broadcasts.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.userId = :userId OR (n.userId IS NULL AND n.role = :role)) " +
            "AND n.readAt IS NULL AND n.dismissed = false")
    Long countUnreadIncludingBroadcasts(@Param("userId") Long userId, @Param("role") NotificationRole role);

    // ===== Statistics Queries =====

    /**
     * Get notification statistics for user.
     */
    @Query("SELECT " +
            "COUNT(n) as total, " +
            "SUM(CASE WHEN n.readAt IS NULL THEN 1 ELSE 0 END) as unread, " +
            "SUM(CASE WHEN n.readAt IS NOT NULL THEN 1 ELSE 0 END) as read " +
            "FROM Notification n WHERE n.userId = :userId AND n.dismissed = false")
    Object[] getStatisticsForUser(@Param("userId") Long userId);

    /**
     * Check if notification exists for order and type.
     */
    boolean existsByOrderIdAndNotificationTypeAndUserId(Long orderId, NotificationType notificationType, Long userId);
}

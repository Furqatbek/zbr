package com.fooddelivery.notification.service;

import com.fooddelivery.notification.dto.*;
import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.model.NotificationType;

import java.util.List;
import java.util.Map;

/**
 * Service interface for persistent notification operations.
 * Handles database-stored notifications with read/unread tracking.
 */
public interface PersistentNotificationService {

    // ===== CRUD Operations =====

    /**
     * Create a new notification.
     */
    NotificationResponseDto createNotification(NotificationCreateDto createDto);

    /**
     * Get notification by ID.
     */
    NotificationResponseDto getNotificationById(Long id);

    /**
     * Get notifications with filters and pagination.
     */
    NotificationListDto getNotifications(NotificationFilterDto filter);

    /**
     * Mark notification as read.
     */
    NotificationResponseDto markAsRead(Long id);

    /**
     * Mark multiple notifications as read.
     */
    int markAsReadByIds(List<Long> ids);

    /**
     * Mark all notifications as read for a user.
     */
    int markAllAsRead(Long userId);

    /**
     * Mark all notifications as read for a user and role.
     */
    int markAllAsRead(Long userId, NotificationRole role);

    /**
     * Delete notification by ID.
     */
    void deleteNotification(Long id);

    /**
     * Delete all notifications for a user.
     */
    int deleteAllForUser(Long userId);

    /**
     * Dismiss notification (soft delete).
     */
    NotificationResponseDto dismissNotification(Long id);

    /**
     * Perform bulk action on notifications.
     */
    int performBulkAction(NotificationBulkActionDto bulkAction);

    // ===== User-Specific Operations =====

    /**
     * Create notification for a specific user.
     */
    NotificationResponseDto createForUser(Long userId, NotificationRole role, String title,
                                          String message, NotificationType type, Map<String, Object> metadata);

    /**
     * Create broadcast notification for all users of a role.
     */
    NotificationResponseDto createForRole(NotificationRole role, String title,
                                          String message, NotificationType type, Map<String, Object> metadata);

    /**
     * Get notifications for a user (including role broadcasts).
     */
    NotificationListDto getUserNotifications(Long userId, NotificationRole role, NotificationFilterDto filter);

    /**
     * Get unread notifications for a user.
     */
    NotificationListDto getUnreadNotifications(Long userId, NotificationRole role, Integer page, Integer pageSize);

    /**
     * Get notification counts for a user.
     */
    NotificationCountDto getNotificationCounts(Long userId, NotificationRole role);

    /**
     * Get unread count for a user.
     */
    Long getUnreadCount(Long userId);

    /**
     * Get unread count for a user and role.
     */
    Long getUnreadCount(Long userId, NotificationRole role);

    // ===== Order Event Operations =====

    /**
     * Create notification for an order event.
     */
    void createForOrderEvent(OrderNotificationRequest request);

    /**
     * Notify when order is created.
     */
    void notifyOrderCreated(OrderNotificationRequest request);

    /**
     * Notify when order is accepted by restaurant.
     */
    void notifyOrderAccepted(OrderNotificationRequest request);

    /**
     * Notify when order is rejected by restaurant.
     */
    void notifyOrderRejected(OrderNotificationRequest request);

    /**
     * Notify when order preparation starts.
     */
    void notifyOrderPreparing(OrderNotificationRequest request);

    /**
     * Notify when order is ready for pickup.
     */
    void notifyOrderReady(OrderNotificationRequest request);

    /**
     * Notify when courier is assigned.
     */
    void notifyCourierAssigned(OrderNotificationRequest request);

    /**
     * Notify when order is picked up by courier.
     */
    void notifyOrderPickedUp(OrderNotificationRequest request);

    /**
     * Notify when order is in transit.
     */
    void notifyOrderInTransit(OrderNotificationRequest request);

    /**
     * Notify when order is delivered.
     */
    void notifyOrderDelivered(OrderNotificationRequest request);

    /**
     * Notify when order is completed (final confirmation after delivery).
     */
    void notifyOrderCompleted(OrderNotificationRequest request);

    /**
     * Notify when order is cancelled.
     */
    void notifyOrderCancelled(OrderNotificationRequest request);

    /**
     * Notify when payment fails.
     */
    void notifyPaymentFailed(OrderNotificationRequest request);

    /**
     * Notify when payment is received.
     */
    void notifyPaymentReceived(OrderNotificationRequest request);

    /**
     * Notify when refund is processed.
     */
    void notifyRefundProcessed(OrderNotificationRequest request);

    /**
     * Notify when payout is issued.
     */
    void notifyPayoutIssued(Long userId, NotificationRole role, String amount, Map<String, Object> metadata);

    // ===== Support Ticket Operations =====

    /**
     * Notify when support ticket is created.
     */
    void notifySupportTicketCreated(Long userId, Long ticketId, String subject);

    /**
     * Notify when support ticket is updated.
     */
    void notifySupportTicketUpdated(Long userId, Long ticketId, String updateMessage);

    /**
     * Notify when support ticket is resolved.
     */
    void notifySupportTicketResolved(Long userId, Long ticketId);

    // ===== Courier Issue Operations =====

    /**
     * Notify consumer, restaurant owner, and admin when courier reports an issue with an order.
     */
    void notifyCourierIssueReported(Long orderId, Long consumerId, Long restaurantId,
                                     String orderNumber, String issueType, String description, String courierName);

    // ===== Reassignment Operations =====

    /**
     * Notify customer that their order is being reassigned.
     */
    void notifyOrderReassigned(com.fooddelivery.order.entity.Order order, String message);

    /**
     * Broadcast order availability to couriers in the area.
     */
    void notifyAvailableCouriers(com.fooddelivery.order.entity.Order order);

    // ===== Cleanup Operations =====

    /**
     * Clean up expired notifications.
     */
    int cleanupExpired();

    /**
     * Clean up old dismissed notifications.
     */
    int cleanupDismissed(int daysOld);

    /**
     * Clean up old read notifications.
     */
    int cleanupOldRead(int daysOld);
}

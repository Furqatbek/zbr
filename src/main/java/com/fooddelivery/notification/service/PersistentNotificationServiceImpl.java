package com.fooddelivery.notification.service;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.*;
import com.fooddelivery.notification.mapper.NotificationMapper;
import com.fooddelivery.notification.model.*;
import com.fooddelivery.notification.repository.NotificationRepository;
import com.fooddelivery.notification.repository.NotificationSpecifications;
import com.fooddelivery.notification.util.NotificationConstants;
import com.fooddelivery.notification.util.NotificationMessageBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of PersistentNotificationService.
 * Handles database-stored notifications with read/unread tracking.
 */
@Slf4j
@Service("persistentNotificationService")
@RequiredArgsConstructor
@Transactional
public class PersistentNotificationServiceImpl implements PersistentNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitTemplate rabbitTemplate;

    // ===== CRUD Operations =====

    @Override
    public NotificationResponseDto createNotification(NotificationCreateDto createDto) {
        log.debug("Creating notification: {}", createDto);

        Notification notification = notificationMapper.toEntity(createDto);
        notification = notificationRepository.save(notification);

        log.info("Created notification with ID: {}", notification.getId());

        NotificationResponseDto responseDto = notificationMapper.toResponseDto(notification);

        // Send real-time notification via WebSocket
        sendWebSocketNotification(responseDto);

        // Queue push notification for mobile delivery
        queuePushNotification(createDto, responseDto);

        return responseDto;
    }

    /**
     * Send notification via WebSocket for real-time delivery to connected clients.
     */
    private void sendWebSocketNotification(NotificationResponseDto notification) {
        if (notification.getUserId() == null) {
            log.debug("Skipping WebSocket notification: no userId");
            return;
        }

        try {
            // Send to user-specific topic
            String userTopic = "/topic/users/" + notification.getUserId() + "/notifications";
            messagingTemplate.convertAndSend(userTopic, notification);
            log.debug("Sent WebSocket notification to user {}: {}", notification.getUserId(), notification.getId());

            // Also send to role-specific topic if applicable
            if (notification.getRole() != null) {
                String roleTopic = "/topic/roles/" + notification.getRole().name().toLowerCase() + "/notifications";
                messagingTemplate.convertAndSend(roleTopic, notification);
            }
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification: {}", e.getMessage());
            // Don't throw - WebSocket failure shouldn't fail the notification creation
        }
    }

    /**
     * Queue push notification for mobile delivery via FCM/APNS.
     */
    private void queuePushNotification(NotificationCreateDto createDto, NotificationResponseDto savedNotification) {
        if (createDto.getUserId() == null) {
            log.debug("Skipping push notification: no userId");
            return;
        }

        try {
            NotificationRequest pushRequest = NotificationRequest.builder()
                    .userId(createDto.getUserId())
                    .subject(createDto.getTitle())
                    .body(createDto.getMessage())
                    .channel("push")
                    .priority(createDto.getPriority() != null ? createDto.getPriority().ordinal() + 1 : 5)
                    .referenceId(savedNotification.getId().toString())
                    .referenceType("notification")
                    .templateData(createDto.getMetadata())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_PUSH_KEY,
                    pushRequest
            );
            log.debug("Queued push notification for user {}", createDto.getUserId());
        } catch (Exception e) {
            log.warn("Failed to queue push notification: {}", e.getMessage());
            // Don't throw - push queue failure shouldn't fail the notification creation
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponseDto getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + id));
        return notificationMapper.toResponseDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListDto getNotifications(NotificationFilterDto filter) {
        log.debug("Getting notifications with filter: {}", filter);

        Pageable pageable = createPageable(filter);
        Specification<Notification> spec = NotificationSpecifications.fromFilter(filter);

        Page<Notification> page = notificationRepository.findAll(spec, pageable);

        List<NotificationResponseDto> notifications = notificationMapper.toResponseDtoList(page.getContent());

        // Get counts for the filter
        Long unreadCount = countUnreadForFilter(filter);
        Map<String, Long> categoryBreakdown = getCategoryBreakdown(filter);
        Map<String, Long> priorityBreakdown = getPriorityBreakdown(filter);

        return NotificationListDto.builder()
                .notifications(notifications)
                .page(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .unreadCount(unreadCount)
                .categoryBreakdown(categoryBreakdown)
                .priorityBreakdown(priorityBreakdown)
                .build();
    }

    @Override
    @CacheEvict(value = NotificationConstants.CACHE_UNREAD_COUNT, key = "#result.userId")
    public NotificationResponseDto markAsRead(Long id) {
        log.debug("Marking notification {} as read", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + id));

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            log.info("Marked notification {} as read", id);
        }

        return notificationMapper.toResponseDto(notification);
    }

    @Override
    public int markAsReadByIds(List<Long> ids) {
        log.debug("Marking {} notifications as read", ids.size());
        return notificationRepository.markAsReadByIds(ids, LocalDateTime.now());
    }

    @Override
    @CacheEvict(value = NotificationConstants.CACHE_UNREAD_COUNT, key = "#userId")
    public int markAllAsRead(Long userId) {
        log.debug("Marking all notifications as read for user {}", userId);
        return notificationRepository.markAllAsReadForUser(userId, LocalDateTime.now());
    }

    @Override
    @CacheEvict(value = NotificationConstants.CACHE_UNREAD_COUNT, key = "#userId")
    public int markAllAsRead(Long userId, NotificationRole role) {
        log.debug("Marking all {} notifications as read for user {}", role, userId);
        return notificationRepository.markAllAsReadForUserAndRole(userId, role, LocalDateTime.now());
    }

    @Override
    public void deleteNotification(Long id) {
        log.debug("Deleting notification {}", id);

        if (!notificationRepository.existsById(id)) {
            throw new EntityNotFoundException("Notification not found with ID: " + id);
        }

        notificationRepository.deleteById(id);
        log.info("Deleted notification {}", id);
    }

    @Override
    @CacheEvict(value = NotificationConstants.CACHE_UNREAD_COUNT, key = "#userId")
    public int deleteAllForUser(Long userId) {
        log.debug("Deleting all notifications for user {}", userId);
        return notificationRepository.deleteByUserId(userId);
    }

    @Override
    public NotificationResponseDto dismissNotification(Long id) {
        log.debug("Dismissing notification {}", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + id));

        notification.dismiss();
        notification = notificationRepository.save(notification);

        log.info("Dismissed notification {}", id);
        return notificationMapper.toResponseDto(notification);
    }

    @Override
    public int performBulkAction(NotificationBulkActionDto bulkAction) {
        log.debug("Performing bulk action {} on {} notifications",
                bulkAction.getAction(), bulkAction.getNotificationIds().size());

        LocalDateTime now = LocalDateTime.now();

        return switch (bulkAction.getAction()) {
            case MARK_READ -> notificationRepository.markAsReadByIds(bulkAction.getNotificationIds(), now);
            case DISMISS -> notificationRepository.dismissByIds(bulkAction.getNotificationIds(), now);
            case DELETE -> {
                notificationRepository.deleteAllById(bulkAction.getNotificationIds());
                yield bulkAction.getNotificationIds().size();
            }
            case MARK_UNREAD -> {
                // Mark as unread by setting readAt to null - need custom query
                List<Notification> notifications = notificationRepository.findAllById(bulkAction.getNotificationIds());
                notifications.forEach(n -> n.setReadAt(null));
                notificationRepository.saveAll(notifications);
                yield notifications.size();
            }
        };
    }

    // ===== User-Specific Operations =====

    @Override
    public NotificationResponseDto createForUser(Long userId, NotificationRole role, String title,
                                                  String message, NotificationType type, Map<String, Object> metadata) {
        NotificationCreateDto createDto = NotificationCreateDto.builder()
                .userId(userId)
                .role(role)
                .title(title)
                .message(message)
                .category(type.getDefaultCategory())
                .notificationType(type)
                .priority(type.getDefaultPriority())
                .metadata(metadata)
                .icon(NotificationMessageBuilder.getIcon(type))
                .build();

        return createNotification(createDto);
    }

    @Override
    public NotificationResponseDto createForRole(NotificationRole role, String title,
                                                  String message, NotificationType type, Map<String, Object> metadata) {
        NotificationCreateDto createDto = NotificationCreateDto.builder()
                .userId(null) // Broadcast to all users of role
                .role(role)
                .title(title)
                .message(message)
                .category(type.getDefaultCategory())
                .notificationType(type)
                .priority(type.getDefaultPriority())
                .metadata(metadata)
                .icon(NotificationMessageBuilder.getIcon(type))
                .build();

        return createNotification(createDto);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListDto getUserNotifications(Long userId, NotificationRole role, NotificationFilterDto filter) {
        filter.setUserId(userId);
        if (role != null) {
            filter.setRole(role);
        }
        return getNotifications(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListDto getUnreadNotifications(Long userId, NotificationRole role, Integer page, Integer pageSize) {
        NotificationFilterDto filter = NotificationFilterDto.builder()
                .userId(userId)
                .role(role)
                .isRead(false)
                .page(page != null ? page : 0)
                .pageSize(pageSize != null ? pageSize : NotificationConstants.DEFAULT_PAGE_SIZE)
                .build();

        return getNotifications(filter);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = NotificationConstants.CACHE_UNREAD_COUNT, key = "#userId + '-' + #role")
    public NotificationCountDto getNotificationCounts(Long userId, NotificationRole role) {
        log.debug("Getting notification counts for user {} role {}", userId, role);

        Long total = notificationRepository.countByUserId(userId);
        Long unread = role != null
                ? notificationRepository.countUnreadByUserIdAndRole(userId, role)
                : notificationRepository.countUnreadByUserId(userId);
        Long read = total - unread;

        // Get unread by category
        Map<String, Long> unreadByCategory = new HashMap<>();
        List<Object[]> categoryData = notificationRepository.countUnreadByCategoryForUser(userId);
        for (Object[] row : categoryData) {
            NotificationCategory category = (NotificationCategory) row[0];
            Long count = (Long) row[1];
            unreadByCategory.put(category.name(), count);
        }

        // Get unread by priority
        Map<String, Long> unreadByPriority = new HashMap<>();
        List<Object[]> priorityData = notificationRepository.countUnreadByPriorityForUser(userId);
        for (Object[] row : priorityData) {
            NotificationPriority priority = (NotificationPriority) row[0];
            Long count = (Long) row[1];
            unreadByPriority.put(priority.name(), count);
        }

        Long urgentCount = unreadByPriority.getOrDefault(NotificationPriority.URGENT.name(), 0L);
        Long highPriorityCount = unreadByPriority.getOrDefault(NotificationPriority.HIGH.name(), 0L);

        return NotificationCountDto.builder()
                .total(total)
                .unread(unread)
                .read(read)
                .unreadByCategory(unreadByCategory)
                .unreadByPriority(unreadByPriority)
                .urgentCount(urgentCount)
                .highPriorityCount(highPriorityCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId, NotificationRole role) {
        return notificationRepository.countUnreadByUserIdAndRole(userId, role);
    }

    // ===== Order Event Operations =====

    @Override
    public void createForOrderEvent(OrderNotificationRequest request) {
        log.debug("Creating notifications for order event: {} orderId: {}",
                request.getEventType(), request.getOrderId());

        switch (request.getEventType()) {
            case ORDER_CREATED -> notifyOrderCreated(request);
            case ORDER_ACCEPTED -> notifyOrderAccepted(request);
            case ORDER_REJECTED -> notifyOrderRejected(request);
            case ORDER_PREPARING -> notifyOrderPreparing(request);
            case ORDER_READY -> notifyOrderReady(request);
            case COURIER_ASSIGNED -> notifyCourierAssigned(request);
            case ORDER_PICKED_UP -> notifyOrderPickedUp(request);
            case ORDER_IN_TRANSIT -> notifyOrderInTransit(request);
            case ORDER_DELIVERED -> notifyOrderDelivered(request);
            case ORDER_CANCELLED -> notifyOrderCancelled(request);
            case PAYMENT_FAILED -> notifyPaymentFailed(request);
            case PAYMENT_RECEIVED -> notifyPaymentReceived(request);
            case PAYMENT_REFUNDED -> notifyRefundProcessed(request);
            default -> log.warn("Unhandled order event type: {}", request.getEventType());
        }
    }

    @Override
    public void notifyOrderCreated(OrderNotificationRequest request) {
        request.setEventType(NotificationType.ORDER_CREATED);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }

        // Notify restaurant
        if (request.getRestaurantUserId() != null) {
            request.setEventType(NotificationType.NEW_ORDER_RECEIVED);
            createOrderNotification(request, request.getRestaurantUserId(), NotificationRole.RESTAURANT);
        }
    }

    @Override
    public void notifyOrderAccepted(OrderNotificationRequest request) {
        request.setEventType(NotificationType.ORDER_ACCEPTED);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }
    }

    @Override
    public void notifyOrderRejected(OrderNotificationRequest request) {
        request.setEventType(NotificationType.ORDER_REJECTED);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }

        // Notify admin for monitoring
        createOrderNotification(request, null, NotificationRole.ADMIN);
    }

    @Override
    public void notifyOrderPreparing(OrderNotificationRequest request) {
        request.setEventType(NotificationType.ORDER_PREPARING);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }
    }

    @Override
    public void notifyOrderReady(OrderNotificationRequest request) {
        request.setEventType(NotificationType.ORDER_READY);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }

        // Notify courier
        if (request.getCourierUserId() != null) {
            createOrderNotification(request, request.getCourierUserId(), NotificationRole.COURIER);
        }
    }

    @Override
    public void notifyCourierAssigned(OrderNotificationRequest request) {
        request.setEventType(NotificationType.COURIER_ASSIGNED);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }

        // Notify courier
        if (request.getCourierUserId() != null) {
            request.setEventType(NotificationType.NEW_DELIVERY_AVAILABLE);
            createOrderNotification(request, request.getCourierUserId(), NotificationRole.COURIER);
        }

        // Notify restaurant
        if (request.getRestaurantUserId() != null) {
            request.setEventType(NotificationType.COURIER_ASSIGNED);
            createOrderNotification(request, request.getRestaurantUserId(), NotificationRole.RESTAURANT);
        }
    }

    @Override
    public void notifyOrderPickedUp(OrderNotificationRequest request) {
        request.setEventType(NotificationType.ORDER_PICKED_UP);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }

        // Notify restaurant
        if (request.getRestaurantUserId() != null) {
            createOrderNotification(request, request.getRestaurantUserId(), NotificationRole.RESTAURANT);
        }
    }

    @Override
    public void notifyOrderInTransit(OrderNotificationRequest request) {
        request.setEventType(NotificationType.ORDER_IN_TRANSIT);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }
    }

    @Override
    public void notifyOrderDelivered(OrderNotificationRequest request) {
        request.setEventType(NotificationType.ORDER_DELIVERED);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }

        // Notify restaurant
        if (request.getRestaurantUserId() != null) {
            createOrderNotification(request, request.getRestaurantUserId(), NotificationRole.RESTAURANT);
        }

        // Notify courier
        if (request.getCourierUserId() != null) {
            request.setEventType(NotificationType.DELIVERY_COMPLETED);
            createOrderNotification(request, request.getCourierUserId(), NotificationRole.COURIER);
        }
    }

    @Override
    public void notifyOrderCancelled(OrderNotificationRequest request) {
        request.setEventType(NotificationType.ORDER_CANCELLED);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }

        // Notify restaurant
        if (request.getRestaurantUserId() != null) {
            createOrderNotification(request, request.getRestaurantUserId(), NotificationRole.RESTAURANT);
        }

        // Notify courier if assigned
        if (request.getCourierUserId() != null) {
            createOrderNotification(request, request.getCourierUserId(), NotificationRole.COURIER);
        }

        // Notify admin for monitoring
        createOrderNotification(request, null, NotificationRole.ADMIN);
    }

    @Override
    public void notifyPaymentFailed(OrderNotificationRequest request) {
        request.setEventType(NotificationType.PAYMENT_FAILED);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }

        // Notify admin for monitoring
        createOrderNotification(request, null, NotificationRole.ADMIN);
    }

    @Override
    public void notifyPaymentReceived(OrderNotificationRequest request) {
        request.setEventType(NotificationType.PAYMENT_RECEIVED);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }
    }

    @Override
    public void notifyRefundProcessed(OrderNotificationRequest request) {
        request.setEventType(NotificationType.PAYMENT_REFUNDED);

        // Notify consumer
        if (request.getCustomerId() != null) {
            createOrderNotification(request, request.getCustomerId(), NotificationRole.CONSUMER);
        }
    }

    @Override
    public void notifyPayoutIssued(Long userId, NotificationRole role, String amount, Map<String, Object> metadata) {
        NotificationCreateDto createDto = NotificationCreateDto.builder()
                .userId(userId)
                .role(role)
                .title("Payout Received")
                .message(String.format("Your payout of %s has been deposited to your account.", amount))
                .category(NotificationCategory.FINANCE)
                .notificationType(NotificationType.PAYOUT_COMPLETED)
                .priority(NotificationPriority.NORMAL)
                .metadata(metadata)
                .icon(NotificationConstants.ICON_PAYMENT)
                .build();

        createNotification(createDto);
    }

    // ===== Support Ticket Operations =====

    @Override
    public void notifySupportTicketCreated(Long userId, Long ticketId, String subject) {
        NotificationCreateDto createDto = NotificationCreateDto.builder()
                .userId(userId)
                .role(NotificationRole.CONSUMER)
                .title("Support Ticket Created")
                .message(String.format("Your support ticket '%s' has been created. We'll respond shortly.", subject))
                .category(NotificationCategory.SUPPORT)
                .notificationType(NotificationType.SUPPORT_TICKET_CREATED)
                .priority(NotificationPriority.NORMAL)
                .relatedEntityId(ticketId)
                .relatedEntityType(NotificationConstants.ENTITY_TYPE_TICKET)
                .icon(NotificationConstants.ICON_SUPPORT)
                .actionUrl(NotificationConstants.ACTION_SUPPORT_TICKET.replace("{ticketId}", ticketId.toString()))
                .build();

        createNotification(createDto);
    }

    @Override
    public void notifySupportTicketUpdated(Long userId, Long ticketId, String updateMessage) {
        NotificationCreateDto createDto = NotificationCreateDto.builder()
                .userId(userId)
                .role(NotificationRole.CONSUMER)
                .title("Support Ticket Updated")
                .message(updateMessage)
                .category(NotificationCategory.SUPPORT)
                .notificationType(NotificationType.SUPPORT_TICKET_UPDATED)
                .priority(NotificationPriority.NORMAL)
                .relatedEntityId(ticketId)
                .relatedEntityType(NotificationConstants.ENTITY_TYPE_TICKET)
                .icon(NotificationConstants.ICON_SUPPORT)
                .actionUrl(NotificationConstants.ACTION_SUPPORT_TICKET.replace("{ticketId}", ticketId.toString()))
                .build();

        createNotification(createDto);
    }

    @Override
    public void notifySupportTicketResolved(Long userId, Long ticketId) {
        NotificationCreateDto createDto = NotificationCreateDto.builder()
                .userId(userId)
                .role(NotificationRole.CONSUMER)
                .title("Support Ticket Resolved")
                .message("Your support ticket has been resolved. Thank you for your patience.")
                .category(NotificationCategory.SUPPORT)
                .notificationType(NotificationType.SUPPORT_TICKET_RESOLVED)
                .priority(NotificationPriority.NORMAL)
                .relatedEntityId(ticketId)
                .relatedEntityType(NotificationConstants.ENTITY_TYPE_TICKET)
                .icon(NotificationConstants.ICON_SUCCESS)
                .actionUrl(NotificationConstants.ACTION_SUPPORT_TICKET.replace("{ticketId}", ticketId.toString()))
                .build();

        createNotification(createDto);
    }

    // ===== Cleanup Operations =====

    @Override
    public int cleanupExpired() {
        log.info("Cleaning up expired notifications");
        return notificationRepository.deleteExpired(LocalDateTime.now());
    }

    @Override
    public int cleanupDismissed(int daysOld) {
        log.info("Cleaning up dismissed notifications older than {} days", daysOld);
        LocalDateTime before = LocalDateTime.now().minusDays(daysOld);
        return notificationRepository.deleteDismissedBefore(before);
    }

    @Override
    public int cleanupOldRead(int daysOld) {
        log.info("Cleaning up read notifications older than {} days", daysOld);
        LocalDateTime before = LocalDateTime.now().minusDays(daysOld);
        return notificationRepository.deleteReadBefore(before);
    }

    // ===== Helper Methods =====

    private void createOrderNotification(OrderNotificationRequest request, Long userId, NotificationRole role) {
        Map<String, String> messageContent = switch (role) {
            case CONSUMER, CUSTOMER -> NotificationMessageBuilder.buildCustomerMessage(request);
            case RESTAURANT -> NotificationMessageBuilder.buildRestaurantMessage(request);
            case COURIER -> NotificationMessageBuilder.buildCourierMessage(request);
            case ADMIN -> NotificationMessageBuilder.buildAdminMessage(request);
            default -> Map.of("title", request.getEventType().getDisplayName(),
                    "message", "Order update for #" + request.getOrderId());
        };

        Map<String, Object> metadata = new HashMap<>();
        if (request.getAdditionalData() != null) {
            metadata.putAll(request.getAdditionalData());
        }
        metadata.put("orderNumber", request.getOrderNumber());
        metadata.put("restaurantName", request.getRestaurantName());
        metadata.put("courierName", request.getCourierName());

        NotificationCreateDto createDto = NotificationCreateDto.builder()
                .userId(userId)
                .role(role)
                .title(messageContent.get("title"))
                .message(messageContent.get("message"))
                .category(request.getEventType().getDefaultCategory())
                .notificationType(request.getEventType())
                .priority(request.getEventType().getDefaultPriority())
                .orderId(request.getOrderId())
                .relatedEntityId(request.getOrderId())
                .relatedEntityType(NotificationConstants.ENTITY_TYPE_ORDER)
                .metadata(metadata)
                .icon(NotificationMessageBuilder.getIcon(request.getEventType()))
                .actionUrl(NotificationMessageBuilder.getActionUrl(request.getEventType(), request.getOrderId(), null))
                .build();

        createNotification(createDto);
    }

    private Pageable createPageable(NotificationFilterDto filter) {
        int page = filter.getPage() != null ? filter.getPage() : 0;
        int size = filter.getPageSize() != null
                ? Math.min(filter.getPageSize(), NotificationConstants.MAX_PAGE_SIZE)
                : NotificationConstants.DEFAULT_PAGE_SIZE;

        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    private Long countUnreadForFilter(NotificationFilterDto filter) {
        NotificationFilterDto unreadFilter = NotificationFilterDto.builder()
                .userId(filter.getUserId())
                .role(filter.getRole())
                .isRead(false)
                .includeDismissed(false)
                .includeExpired(false)
                .build();

        Specification<Notification> spec = NotificationSpecifications.fromFilter(unreadFilter);
        return notificationRepository.count(spec);
    }

    private Map<String, Long> getCategoryBreakdown(NotificationFilterDto filter) {
        if (filter.getUserId() == null) {
            return Collections.emptyMap();
        }

        Map<String, Long> breakdown = new HashMap<>();
        for (NotificationCategory category : NotificationCategory.values()) {
            Long count = notificationRepository.countUnreadByUserIdAndCategory(filter.getUserId(), category);
            if (count > 0) {
                breakdown.put(category.name(), count);
            }
        }
        return breakdown;
    }

    private Map<String, Long> getPriorityBreakdown(NotificationFilterDto filter) {
        if (filter.getUserId() == null) {
            return Collections.emptyMap();
        }

        Map<String, Long> breakdown = new HashMap<>();
        for (NotificationPriority priority : NotificationPriority.values()) {
            Long count = notificationRepository.countUnreadByUserIdAndPriority(filter.getUserId(), priority);
            if (count > 0) {
                breakdown.put(priority.name(), count);
            }
        }
        return breakdown;
    }
}

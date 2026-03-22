package com.fooddelivery.notification.controller;

import com.fooddelivery.notification.dto.*;
import com.fooddelivery.notification.model.NotificationCategory;
import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.service.PersistentNotificationService;
import com.fooddelivery.notification.util.NotificationConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for notification operations.
 * Provides endpoints for creating, reading, updating, and deleting notifications.
 */
@Slf4j
@RestController
@RequestMapping(NotificationConstants.API_BASE_PATH)
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final PersistentNotificationService notificationService;

    // ===== Create Operations =====

    /**
     * Create a new notification.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    @Operation(summary = "Create notification",
            description = "Create a new notification for a user or role. Admin/System only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<NotificationResponseDto> createNotification(
            @Valid @RequestBody NotificationCreateDto createDto) {
        log.info("Creating notification: {}", createDto.getTitle());
        NotificationResponseDto response = notificationService.createNotification(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ===== Read Operations =====

    /**
     * Get notification by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notification by ID",
            description = "Retrieve a specific notification by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponseDto> getNotificationById(
            @PathVariable Long id) {
        log.debug("Getting notification by ID: {}", id);
        NotificationResponseDto response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get notifications with filters (GET method with query params).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Get notifications",
            description = "Get notifications with optional filters and pagination. Admin/Platform only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    })
    public ResponseEntity<NotificationListDto> getNotifications(
            @Parameter(description = "User ID to filter by")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "Role to filter by")
            @RequestParam(required = false) NotificationRole role,

            @Parameter(description = "Filter by read status (true=read, false=unread)")
            @RequestParam(required = false) Boolean isRead,

            @Parameter(description = "Filter by category")
            @RequestParam(required = false) NotificationCategory category,

            @Parameter(description = "Filter by order ID")
            @RequestParam(required = false) Long orderId,

            @Parameter(description = "Start date for filtering")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,

            @Parameter(description = "End date for filtering")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,

            @Parameter(description = "Search term for title/message")
            @RequestParam(required = false) String searchTerm,

            @Parameter(description = "Include dismissed notifications")
            @RequestParam(defaultValue = "false") Boolean includeDismissed,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") Integer pageSize,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        NotificationFilterDto filter = NotificationFilterDto.builder()
                .userId(userId)
                .role(role)
                .isRead(isRead)
                .category(category)
                .orderId(orderId)
                .createdFrom(createdFrom)
                .createdTo(createdTo)
                .searchTerm(searchTerm)
                .includeDismissed(includeDismissed)
                .page(page)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        log.debug("Getting notifications with filter: {}", filter);
        NotificationListDto response = notificationService.getNotifications(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Get notifications with POST body filter.
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Search notifications",
            description = "Search notifications with advanced filter in request body. Admin/Platform only.")
    public ResponseEntity<NotificationListDto> searchNotifications(
            @Valid @RequestBody NotificationFilterDto filter) {
        log.debug("Searching notifications with filter: {}", filter);
        NotificationListDto response = notificationService.getNotifications(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user's notifications.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my notifications",
            description = "Get notifications for the currently authenticated user")
    public ResponseEntity<NotificationListDto> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,

            @RequestParam(required = false) NotificationRole role,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationCategory category,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Long userId = extractUserId(userDetails);

        // Use provided role or extract from user's auth role
        NotificationRole effectiveRole = role != null ? role : extractNotificationRole(userDetails);

        NotificationFilterDto filter = NotificationFilterDto.builder()
                .userId(userId)
                .role(effectiveRole)
                .isRead(isRead)
                .category(category)
                .page(page)
                .pageSize(pageSize)
                .build();

        log.debug("Getting notifications for user {} with role {}", userId, effectiveRole);
        NotificationListDto response = notificationService.getNotifications(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Get unread notifications for a user.
     */
    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Get unread notifications",
            description = "Get all unread notifications for a specific user")
    public ResponseEntity<NotificationListDto> getUnreadNotifications(
            @PathVariable Long userId,
            @RequestParam(required = false) NotificationRole role,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        log.debug("Getting unread notifications for user: {}", userId);
        NotificationListDto response = notificationService.getUnreadNotifications(userId, role, page, pageSize);
        return ResponseEntity.ok(response);
    }

    /**
     * Get notification counts for a user.
     */
    @GetMapping("/user/{userId}/counts")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Get notification counts",
            description = "Get notification counts (total, unread, by category) for a user")
    public ResponseEntity<NotificationCountDto> getNotificationCounts(
            @PathVariable Long userId,
            @RequestParam(required = false) NotificationRole role) {

        log.debug("Getting notification counts for user: {} role: {}", userId, role);
        NotificationCountDto response = notificationService.getNotificationCounts(userId, role);
        return ResponseEntity.ok(response);
    }

    /**
     * Get unread count only.
     */
    @GetMapping("/user/{userId}/unread-count")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Get unread count",
            description = "Get the count of unread notifications for a user")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long userId,
            @RequestParam(required = false) NotificationRole role) {

        Long count = role != null
                ? notificationService.getUnreadCount(userId, role)
                : notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Get unread count for current user.
     */
    @GetMapping("/unread/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my unread count",
            description = "Get the count of unread notifications for the current user")
    public ResponseEntity<Map<String, Long>> getMyUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) NotificationRole role) {

        Long userId = extractUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("unreadCount", 0L));
        }

        // Use provided role or extract from user's auth role
        NotificationRole effectiveRole = role != null ? role : extractNotificationRole(userDetails);

        Long count = effectiveRole != null
                ? notificationService.getUnreadCount(userId, effectiveRole)
                : notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ===== Update Operations =====

    /**
     * Mark notification as read.
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark as read",
            description = "Mark a specific notification as read")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable Long id) {
        log.debug("Marking notification {} as read", id);
        NotificationResponseDto response = notificationService.markAsRead(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark all notifications as read for a user.
     */
    @PatchMapping("/read-all")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Mark all as read",
            description = "Mark all notifications as read for a user")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @RequestParam Long userId,
            @RequestParam(required = false) NotificationRole role) {

        log.info("Marking all notifications as read for user: {} role: {}", userId, role);

        int count = role != null
                ? notificationService.markAllAsRead(userId, role)
                : notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "markedCount", count
        ));
    }

    /**
     * Mark multiple notifications as read.
     */
    @PatchMapping("/read-batch")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark batch as read",
            description = "Mark multiple notifications as read by IDs")
    public ResponseEntity<Map<String, Object>> markBatchAsRead(
            @RequestBody List<Long> notificationIds) {

        log.debug("Marking {} notifications as read", notificationIds.size());
        int count = notificationService.markAsReadByIds(notificationIds);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "markedCount", count
        ));
    }

    /**
     * Dismiss notification.
     */
    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Dismiss notification",
            description = "Dismiss (soft delete) a notification")
    public ResponseEntity<NotificationResponseDto> dismissNotification(@PathVariable Long id) {
        log.debug("Dismissing notification {}", id);
        NotificationResponseDto response = notificationService.dismissNotification(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Perform bulk action on notifications.
     */
    @PostMapping("/bulk-action")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bulk action",
            description = "Perform bulk actions (mark read, dismiss, delete) on multiple notifications")
    public ResponseEntity<Map<String, Object>> performBulkAction(
            @Valid @RequestBody NotificationBulkActionDto bulkAction) {

        log.info("Performing bulk action {} on {} notifications",
                bulkAction.getAction(), bulkAction.getNotificationIds().size());

        int count = notificationService.performBulkAction(bulkAction);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "affectedCount", count
        ));
    }

    // ===== Delete Operations =====

    /**
     * Delete notification by ID.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete notification",
            description = "Permanently delete a notification. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notification deleted"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        log.info("Deleting notification {}", id);
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete all notifications for a user.
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Delete user notifications",
            description = "Delete all notifications for a specific user")
    public ResponseEntity<Map<String, Object>> deleteAllForUser(@PathVariable Long userId) {
        log.info("Deleting all notifications for user {}", userId);
        int count = notificationService.deleteAllForUser(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "deletedCount", count
        ));
    }

    // ===== Admin Operations =====

    /**
     * Cleanup expired notifications.
     */
    @PostMapping("/admin/cleanup/expired")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cleanup expired",
            description = "Remove all expired notifications. Admin only.")
    public ResponseEntity<Map<String, Object>> cleanupExpired() {
        log.info("Running expired notifications cleanup");
        int count = notificationService.cleanupExpired();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "deletedCount", count
        ));
    }

    /**
     * Cleanup old dismissed notifications.
     */
    @PostMapping("/admin/cleanup/dismissed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cleanup dismissed",
            description = "Remove old dismissed notifications. Admin only.")
    public ResponseEntity<Map<String, Object>> cleanupDismissed(
            @RequestParam(defaultValue = "7") Integer daysOld) {

        log.info("Running dismissed notifications cleanup (older than {} days)", daysOld);
        int count = notificationService.cleanupDismissed(daysOld);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "deletedCount", count
        ));
    }

    /**
     * Cleanup old read notifications.
     */
    @PostMapping("/admin/cleanup/read")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cleanup old read",
            description = "Remove old read notifications. Admin only.")
    public ResponseEntity<Map<String, Object>> cleanupOldRead(
            @RequestParam(defaultValue = "90") Integer daysOld) {

        log.info("Running old read notifications cleanup (older than {} days)", daysOld);
        int count = notificationService.cleanupOldRead(daysOld);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "deletedCount", count
        ));
    }

    // ===== Helper Methods =====

    /**
     * Extract user ID from UserDetails.
     */
    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof com.fooddelivery.auth.security.UserPrincipal principal) {
            return principal.getId();
        }
        // Fallback for other UserDetails implementations
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            log.warn("Could not extract user ID from username: {}", userDetails.getUsername());
            return null;
        }
    }

    /**
     * Extract notification role from UserDetails based on user's auth role.
     */
    private NotificationRole extractNotificationRole(UserDetails userDetails) {
        if (userDetails instanceof com.fooddelivery.auth.security.UserPrincipal principal) {
            return mapAuthRoleToNotificationRole(principal.getRole());
        }
        return null;
    }

    /**
     * Map auth system Role to notification system NotificationRole.
     */
    private NotificationRole mapAuthRoleToNotificationRole(com.fooddelivery.auth.entity.Role authRole) {
        if (authRole == null) {
            return null;
        }
        return switch (authRole) {
            case CONSUMER -> NotificationRole.CONSUMER;
            case COURIER -> NotificationRole.COURIER;
            case RESTAURANT_OWNER, RESTAURANT_STAFF -> NotificationRole.RESTAURANT;
            case ADMIN, PLATFORM -> NotificationRole.ADMIN;
            case SUPPORT_AGENT, SUPPORT_MANAGER -> NotificationRole.SUPPORT;
            case FINANCE_MANAGER, PAYMENT_ANALYST -> NotificationRole.FINANCE;
            case OPERATIONS_MANAGER, FLEET_MANAGER, RESTAURANT_MANAGER -> NotificationRole.OPERATIONS;
            default -> null;
        };
    }
}

package com.fooddelivery.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.notification.dto.*;
import com.fooddelivery.notification.model.*;
import com.fooddelivery.notification.service.PersistentNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController Unit Tests")
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PersistentNotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private ObjectMapper objectMapper;
    private NotificationResponseDto testResponse;
    private NotificationListDto testListResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testResponse = createTestResponse();
        testListResponse = createTestListResponse();
    }

    @Nested
    @DisplayName("POST /api/v1/notifications - Create Notification")
    class CreateNotificationTests {

        @Test
        @DisplayName("Should create notification and return 201")
        void shouldCreateNotificationAndReturn201() throws Exception {
            // Arrange
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(1L)
                    .role(NotificationRole.CUSTOMER)
                    .title("Test Notification")
                    .message("Test message")
                    .category(NotificationCategory.ORDER)
                    .notificationType(NotificationType.ORDER_CREATED)
                    .build();

            when(notificationService.createNotification(any(NotificationCreateDto.class)))
                    .thenReturn(testResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Test Notification"));

            verify(notificationService).createNotification(any(NotificationCreateDto.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/{id} - Get Notification by ID")
    class GetNotificationByIdTests {

        @Test
        @DisplayName("Should get notification by ID")
        void shouldGetNotificationById() throws Exception {
            // Arrange
            when(notificationService.getNotificationById(1L)).thenReturn(testResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Test Notification"));

            verify(notificationService).getNotificationById(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications - Get Notifications with Filters")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should get notifications with default parameters")
        void shouldGetNotificationsWithDefaultParameters() throws Exception {
            // Arrange
            when(notificationService.getNotifications(any(NotificationFilterDto.class)))
                    .thenReturn(testListResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(notificationService).getNotifications(any(NotificationFilterDto.class));
        }

        @Test
        @DisplayName("Should get notifications with all filter parameters")
        void shouldGetNotificationsWithAllFilterParameters() throws Exception {
            // Arrange
            when(notificationService.getNotifications(any(NotificationFilterDto.class)))
                    .thenReturn(testListResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications")
                            .param("userId", "1")
                            .param("role", "CUSTOMER")
                            .param("isRead", "false")
                            .param("category", "ORDER")
                            .param("page", "0")
                            .param("pageSize", "10")
                            .param("sortBy", "createdAt")
                            .param("sortDirection", "DESC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray());

            verify(notificationService).getNotifications(any(NotificationFilterDto.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/notifications/search - Search Notifications")
    class SearchNotificationsTests {

        @Test
        @DisplayName("Should search notifications with filter body")
        void shouldSearchNotificationsWithFilterBody() throws Exception {
            // Arrange
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(1L)
                    .role(NotificationRole.CUSTOMER)
                    .isRead(false)
                    .build();

            when(notificationService.getNotifications(any(NotificationFilterDto.class)))
                    .thenReturn(testListResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray());

            verify(notificationService).getNotifications(any(NotificationFilterDto.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/user/{userId}/unread - Get Unread Notifications")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("Should get unread notifications for user")
        void shouldGetUnreadNotificationsForUser() throws Exception {
            // Arrange
            when(notificationService.getUnreadNotifications(eq(1L), isNull(), anyInt(), anyInt()))
                    .thenReturn(testListResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/user/1/unread"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray());

            verify(notificationService).getUnreadNotifications(eq(1L), isNull(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should get unread notifications for user with role filter")
        void shouldGetUnreadNotificationsForUserWithRoleFilter() throws Exception {
            // Arrange
            when(notificationService.getUnreadNotifications(eq(1L), eq(NotificationRole.CUSTOMER), anyInt(), anyInt()))
                    .thenReturn(testListResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/user/1/unread")
                            .param("role", "CUSTOMER"))
                    .andExpect(status().isOk());

            verify(notificationService).getUnreadNotifications(eq(1L), eq(NotificationRole.CUSTOMER), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/user/{userId}/counts - Get Notification Counts")
    class GetNotificationCountsTests {

        @Test
        @DisplayName("Should get notification counts for user")
        void shouldGetNotificationCountsForUser() throws Exception {
            // Arrange
            NotificationCountDto countDto = NotificationCountDto.builder()
                    .total(10L)
                    .unread(5L)
                    .read(5L)
                    .urgentCount(1L)
                    .highPriorityCount(2L)
                    .build();

            when(notificationService.getNotificationCounts(1L, null)).thenReturn(countDto);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/user/1/counts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(10))
                    .andExpect(jsonPath("$.unread").value(5));

            verify(notificationService).getNotificationCounts(1L, null);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/user/{userId}/unread-count - Get Unread Count")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should get unread count for user")
        void shouldGetUnreadCountForUser() throws Exception {
            // Arrange
            when(notificationService.getUnreadCount(1L)).thenReturn(5L);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/user/1/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(5));

            verify(notificationService).getUnreadCount(1L);
        }

        @Test
        @DisplayName("Should get unread count for user with role")
        void shouldGetUnreadCountForUserWithRole() throws Exception {
            // Arrange
            when(notificationService.getUnreadCount(1L, NotificationRole.COURIER)).thenReturn(3L);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/user/1/unread-count")
                            .param("role", "COURIER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(3));

            verify(notificationService).getUnreadCount(1L, NotificationRole.COURIER);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/{id}/read - Mark as Read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkNotificationAsRead() throws Exception {
            // Arrange
            NotificationResponseDto readResponse = createTestResponse();

            when(notificationService.markAsRead(1L)).thenReturn(readResponse);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/notifications/1/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(notificationService).markAsRead(1L);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/read-all - Mark All as Read")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should mark all notifications as read for user")
        void shouldMarkAllAsReadForUser() throws Exception {
            // Arrange
            when(notificationService.markAllAsRead(1L)).thenReturn(5);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/notifications/read-all")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.markedCount").value(5));

            verify(notificationService).markAllAsRead(1L);
        }

        @Test
        @DisplayName("Should mark all notifications as read for user with role")
        void shouldMarkAllAsReadForUserWithRole() throws Exception {
            // Arrange
            when(notificationService.markAllAsRead(1L, NotificationRole.CUSTOMER)).thenReturn(3);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/notifications/read-all")
                            .param("userId", "1")
                            .param("role", "CUSTOMER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.markedCount").value(3));

            verify(notificationService).markAllAsRead(1L, NotificationRole.CUSTOMER);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/read-batch - Mark Batch as Read")
    class MarkBatchAsReadTests {

        @Test
        @DisplayName("Should mark batch of notifications as read")
        void shouldMarkBatchAsRead() throws Exception {
            // Arrange
            List<Long> ids = List.of(1L, 2L, 3L);
            when(notificationService.markAsReadByIds(ids)).thenReturn(3);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/notifications/read-batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ids)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.markedCount").value(3));

            verify(notificationService).markAsReadByIds(ids);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/{id}/dismiss - Dismiss Notification")
    class DismissNotificationTests {

        @Test
        @DisplayName("Should dismiss notification")
        void shouldDismissNotification() throws Exception {
            // Arrange
            when(notificationService.dismissNotification(1L)).thenReturn(testResponse);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/notifications/1/dismiss"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(notificationService).dismissNotification(1L);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/notifications/bulk-action - Bulk Action")
    class BulkActionTests {

        @Test
        @DisplayName("Should perform bulk mark read action")
        void shouldPerformBulkMarkReadAction() throws Exception {
            // Arrange
            NotificationBulkActionDto bulkAction = NotificationBulkActionDto.builder()
                    .notificationIds(List.of(1L, 2L, 3L))
                    .action(NotificationBulkActionDto.BulkAction.MARK_READ)
                    .build();

            when(notificationService.performBulkAction(any(NotificationBulkActionDto.class))).thenReturn(3);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications/bulk-action")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bulkAction)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.affectedCount").value(3));

            verify(notificationService).performBulkAction(any(NotificationBulkActionDto.class));
        }

        @Test
        @DisplayName("Should perform bulk dismiss action")
        void shouldPerformBulkDismissAction() throws Exception {
            // Arrange
            NotificationBulkActionDto bulkAction = NotificationBulkActionDto.builder()
                    .notificationIds(List.of(1L, 2L))
                    .action(NotificationBulkActionDto.BulkAction.DISMISS)
                    .build();

            when(notificationService.performBulkAction(any(NotificationBulkActionDto.class))).thenReturn(2);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications/bulk-action")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bulkAction)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.affectedCount").value(2));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/notifications/{id} - Delete Notification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification and return 204")
        void shouldDeleteNotificationAndReturn204() throws Exception {
            // Arrange
            doNothing().when(notificationService).deleteNotification(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/notifications/1"))
                    .andExpect(status().isNoContent());

            verify(notificationService).deleteNotification(1L);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/notifications/user/{userId} - Delete User Notifications")
    class DeleteUserNotificationsTests {

        @Test
        @DisplayName("Should delete all notifications for user")
        void shouldDeleteAllNotificationsForUser() throws Exception {
            // Arrange
            when(notificationService.deleteAllForUser(1L)).thenReturn(10);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/notifications/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.deletedCount").value(10));

            verify(notificationService).deleteAllForUser(1L);
        }
    }

    @Nested
    @DisplayName("Admin Cleanup Operations")
    class AdminCleanupTests {

        @Test
        @DisplayName("Should cleanup expired notifications")
        void shouldCleanupExpiredNotifications() throws Exception {
            // Arrange
            when(notificationService.cleanupExpired()).thenReturn(15);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications/admin/cleanup/expired"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.deletedCount").value(15));

            verify(notificationService).cleanupExpired();
        }

        @Test
        @DisplayName("Should cleanup old dismissed notifications")
        void shouldCleanupOldDismissedNotifications() throws Exception {
            // Arrange
            when(notificationService.cleanupDismissed(7)).thenReturn(5);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications/admin/cleanup/dismissed")
                            .param("daysOld", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedCount").value(5));

            verify(notificationService).cleanupDismissed(7);
        }

        @Test
        @DisplayName("Should cleanup old read notifications")
        void shouldCleanupOldReadNotifications() throws Exception {
            // Arrange
            when(notificationService.cleanupOldRead(90)).thenReturn(100);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications/admin/cleanup/read")
                            .param("daysOld", "90"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedCount").value(100));

            verify(notificationService).cleanupOldRead(90);
        }
    }

    // Helper methods

    private NotificationResponseDto createTestResponse() {
        return NotificationResponseDto.builder()
                .id(1L)
                .userId(1L)
                .role(NotificationRole.CUSTOMER)
                .title("Test Notification")
                .message("Test message")
                .category(NotificationCategory.ORDER)
                .notificationType(NotificationType.ORDER_CREATED)
                .priority(NotificationPriority.NORMAL)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private NotificationListDto createTestListResponse() {
        return NotificationListDto.builder()
                .notifications(List.of(createTestResponse()))
                .page(0)
                .pageSize(20)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .unreadCount(1L)
                .categoryBreakdown(Map.of("ORDER", 1L))
                .priorityBreakdown(Map.of("NORMAL", 1L))
                .build();
    }
}

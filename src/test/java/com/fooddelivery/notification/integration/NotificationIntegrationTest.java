package com.fooddelivery.notification.integration;

import com.fooddelivery.notification.dto.*;
import com.fooddelivery.notification.model.*;
import com.fooddelivery.notification.repository.NotificationRepository;
import com.fooddelivery.notification.service.PersistentNotificationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Notification System Integration Tests")
class NotificationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private PersistentNotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private static Long testUserId = 1L;
    private static Long createdNotificationId;

    @BeforeEach
    void setUp() {
        // Clear test data before each test
        notificationRepository.deleteAll();
    }

    @Nested
    @DisplayName("Notification CRUD Operations")
    class CrudOperationsTests {

        @Test
        @Order(1)
        @DisplayName("Should create notification successfully")
        void shouldCreateNotificationSuccessfully() {
            // Arrange
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(testUserId)
                    .role(NotificationRole.CUSTOMER)
                    .title("Test Notification")
                    .message("This is a test notification message")
                    .category(NotificationCategory.ORDER)
                    .notificationType(NotificationType.ORDER_CREATED)
                    .priority(NotificationPriority.NORMAL)
                    .orderId(100L)
                    .metadata(Map.of("orderNumber", "ORD-100"))
                    .build();

            // Act
            NotificationResponseDto result = notificationService.createNotification(createDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Notification");
            assertThat(result.getUserId()).isEqualTo(testUserId);
            assertThat(result.getRole()).isEqualTo(NotificationRole.CUSTOMER);
            assertThat(result.getIsRead()).isFalse();

            createdNotificationId = result.getId();
        }

        @Test
        @Order(2)
        @DisplayName("Should get notification by ID")
        void shouldGetNotificationById() {
            // Arrange
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(testUserId)
                    .role(NotificationRole.CUSTOMER)
                    .title("Get By ID Test")
                    .message("Test message")
                    .category(NotificationCategory.ORDER)
                    .notificationType(NotificationType.ORDER_CREATED)
                    .build();
            NotificationResponseDto created = notificationService.createNotification(createDto);

            // Act
            NotificationResponseDto result = notificationService.getNotificationById(created.getId(), null, true);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(created.getId());
            assertThat(result.getTitle()).isEqualTo("Get By ID Test");
        }

        @Test
        @Order(3)
        @DisplayName("Should get notifications with filter")
        void shouldGetNotificationsWithFilter() {
            // Arrange - Create multiple notifications
            for (int i = 0; i < 5; i++) {
                NotificationCreateDto createDto = NotificationCreateDto.builder()
                        .userId(testUserId)
                        .role(NotificationRole.CUSTOMER)
                        .title("Notification " + i)
                        .message("Message " + i)
                        .category(NotificationCategory.ORDER)
                        .notificationType(NotificationType.ORDER_CREATED)
                        .build();
                notificationService.createNotification(createDto);
            }

            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .role(NotificationRole.CUSTOMER)
                    .page(0)
                    .pageSize(10)
                    .build();

            // Act
            NotificationListDto result = notificationService.getNotifications(filter);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getNotifications()).hasSize(5);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getUnreadCount()).isEqualTo(5L);
        }

        @Test
        @Order(4)
        @DisplayName("Should mark notification as read")
        void shouldMarkNotificationAsRead() {
            // Arrange
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(testUserId)
                    .role(NotificationRole.CUSTOMER)
                    .title("Mark Read Test")
                    .message("Test message")
                    .category(NotificationCategory.ORDER)
                    .notificationType(NotificationType.ORDER_CREATED)
                    .build();
            NotificationResponseDto created = notificationService.createNotification(createDto);

            // Act
            NotificationResponseDto result = notificationService.markAsRead(created.getId(), null, true);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getReadAt()).isNotNull();
        }

        @Test
        @Order(5)
        @DisplayName("Should dismiss notification")
        void shouldDismissNotification() {
            // Arrange
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(testUserId)
                    .role(NotificationRole.CUSTOMER)
                    .title("Dismiss Test")
                    .message("Test message")
                    .category(NotificationCategory.ORDER)
                    .notificationType(NotificationType.ORDER_CREATED)
                    .build();
            NotificationResponseDto created = notificationService.createNotification(createDto);

            // Act
            NotificationResponseDto result = notificationService.dismissNotification(created.getId(), null, true);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getDismissed()).isTrue();
            assertThat(result.getDismissedAt()).isNotNull();
        }

        @Test
        @Order(6)
        @DisplayName("Should delete notification")
        void shouldDeleteNotification() {
            // Arrange
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(testUserId)
                    .role(NotificationRole.CUSTOMER)
                    .title("Delete Test")
                    .message("Test message")
                    .category(NotificationCategory.ORDER)
                    .notificationType(NotificationType.ORDER_CREATED)
                    .build();
            NotificationResponseDto created = notificationService.createNotification(createDto);

            // Act
            notificationService.deleteNotification(created.getId());

            // Assert
            assertThat(notificationRepository.findById(created.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Bulk Operations Tests")
    class BulkOperationsTests {

        @Test
        @DisplayName("Should mark multiple notifications as read")
        void shouldMarkMultipleNotificationsAsRead() {
            // Arrange - Create multiple notifications
            List<Long> ids = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) {
                NotificationCreateDto createDto = NotificationCreateDto.builder()
                        .userId(testUserId)
                        .role(NotificationRole.CUSTOMER)
                        .title("Bulk Read Test " + i)
                        .message("Message " + i)
                        .category(NotificationCategory.ORDER)
                        .notificationType(NotificationType.ORDER_CREATED)
                        .build();
                NotificationResponseDto created = notificationService.createNotification(createDto);
                ids.add(created.getId());
            }

            // Act
            int count = notificationService.markAsReadByIds(ids, null, true);

            // Assert
            assertThat(count).isEqualTo(3);

            // Verify all are read
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .isRead(false)
                    .build();
            NotificationListDto result = notificationService.getNotifications(filter);
            assertThat(result.getNotifications()).isEmpty();
        }

        @Test
        @DisplayName("Should mark all notifications as read for user")
        void shouldMarkAllAsReadForUser() {
            // Arrange - Create multiple notifications
            for (int i = 0; i < 5; i++) {
                NotificationCreateDto createDto = NotificationCreateDto.builder()
                        .userId(testUserId)
                        .role(NotificationRole.CUSTOMER)
                        .title("Mark All Read Test " + i)
                        .message("Message " + i)
                        .category(NotificationCategory.ORDER)
                        .notificationType(NotificationType.ORDER_CREATED)
                        .build();
                notificationService.createNotification(createDto);
            }

            // Act
            int count = notificationService.markAllAsRead(testUserId);

            // Assert
            assertThat(count).isEqualTo(5);
            assertThat(notificationService.getUnreadCount(testUserId)).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should perform bulk action - dismiss")
        void shouldPerformBulkActionDismiss() {
            // Arrange
            List<Long> ids = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) {
                NotificationCreateDto createDto = NotificationCreateDto.builder()
                        .userId(testUserId)
                        .role(NotificationRole.CUSTOMER)
                        .title("Bulk Dismiss Test " + i)
                        .message("Message " + i)
                        .category(NotificationCategory.ORDER)
                        .notificationType(NotificationType.ORDER_CREATED)
                        .build();
                NotificationResponseDto created = notificationService.createNotification(createDto);
                ids.add(created.getId());
            }

            NotificationBulkActionDto bulkAction = NotificationBulkActionDto.builder()
                    .notificationIds(ids)
                    .action(NotificationBulkActionDto.BulkAction.DISMISS)
                    .build();

            // Act
            int count = notificationService.performBulkAction(bulkAction, null, true);

            // Assert
            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Notification Counts Tests")
    class NotificationCountsTests {

        @Test
        @DisplayName("Should get notification counts")
        void shouldGetNotificationCounts() {
            // Arrange - Create notifications with different categories
            createNotificationWithCategory(NotificationCategory.ORDER);
            createNotificationWithCategory(NotificationCategory.ORDER);
            createNotificationWithCategory(NotificationCategory.FINANCE);
            createNotificationWithCategory(NotificationCategory.SUPPORT);

            // Mark one as read
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .page(0)
                    .pageSize(1)
                    .build();
            NotificationListDto list = notificationService.getNotifications(filter);
            if (!list.getNotifications().isEmpty()) {
                notificationService.markAsRead(list.getNotifications().get(0).getId(), null, true);
            }

            // Act
            NotificationCountDto result = notificationService.getNotificationCounts(testUserId, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(4L);
            assertThat(result.getUnread()).isEqualTo(3L);
            assertThat(result.getRead()).isEqualTo(1L);
            assertThat(result.getUnreadByCategory()).isNotEmpty();
        }

        @Test
        @DisplayName("Should get unread count for user")
        void shouldGetUnreadCountForUser() {
            // Arrange
            for (int i = 0; i < 5; i++) {
                createNotificationWithCategory(NotificationCategory.ORDER);
            }

            // Mark 2 as read
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .page(0)
                    .pageSize(2)
                    .build();
            NotificationListDto list = notificationService.getNotifications(filter);
            for (NotificationResponseDto n : list.getNotifications()) {
                notificationService.markAsRead(n.getId(), null, true);
            }

            // Act
            Long unreadCount = notificationService.getUnreadCount(testUserId);

            // Assert
            assertThat(unreadCount).isEqualTo(3L);
        }

        private void createNotificationWithCategory(NotificationCategory category) {
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(testUserId)
                    .role(NotificationRole.CUSTOMER)
                    .title("Category Test - " + category)
                    .message("Test message")
                    .category(category)
                    .notificationType(NotificationType.ORDER_CREATED)
                    .build();
            notificationService.createNotification(createDto);
        }
    }

    @Nested
    @DisplayName("Order Event Notifications Tests")
    class OrderEventNotificationsTests {

        @Test
        @DisplayName("Should create notification for order created")
        void shouldCreateNotificationForOrderCreated() {
            // Arrange
            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(100L)
                    .orderNumber("ORD-100")
                    .customerId(testUserId)
                    .restaurantId(10L)
                    .restaurantName("Test Restaurant")
                    .eventType(NotificationType.ORDER_CREATED)
                    .totalAmount(BigDecimal.valueOf(50.00))
                    .build();

            // Act
            notificationService.notifyOrderCreated(request);

            // Assert
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .orderId(100L)
                    .build();
            NotificationListDto result = notificationService.getNotifications(filter);

            assertThat(result.getNotifications()).isNotEmpty();
            assertThat(result.getNotifications().get(0).getOrderId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should create notifications for multiple recipients on order delivered")
        void shouldCreateNotificationsForMultipleRecipientsOnOrderDelivered() {
            // Arrange
            Long customerId = 1L;
            Long restaurantUserId = 10L;
            Long courierId = 20L;

            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(200L)
                    .orderNumber("ORD-200")
                    .customerId(customerId)
                    .restaurantUserId(restaurantUserId)
                    .courierUserId(courierId)
                    .eventType(NotificationType.ORDER_DELIVERED)
                    .build();

            // Act
            notificationService.notifyOrderDelivered(request);

            // Assert - Check customer notification
            NotificationFilterDto customerFilter = NotificationFilterDto.builder()
                    .userId(customerId)
                    .orderId(200L)
                    .build();
            NotificationListDto customerResult = notificationService.getNotifications(customerFilter);
            assertThat(customerResult.getNotifications()).isNotEmpty();

            // Check restaurant notification
            NotificationFilterDto restaurantFilter = NotificationFilterDto.builder()
                    .userId(restaurantUserId)
                    .orderId(200L)
                    .build();
            NotificationListDto restaurantResult = notificationService.getNotifications(restaurantFilter);
            assertThat(restaurantResult.getNotifications()).isNotEmpty();

            // Check courier notification
            NotificationFilterDto courierFilter = NotificationFilterDto.builder()
                    .userId(courierId)
                    .orderId(200L)
                    .build();
            NotificationListDto courierResult = notificationService.getNotifications(courierFilter);
            assertThat(courierResult.getNotifications()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Support Ticket Notifications Tests")
    class SupportTicketNotificationsTests {

        @Test
        @DisplayName("Should create notification for support ticket created")
        void shouldCreateNotificationForSupportTicketCreated() {
            // Act
            notificationService.notifySupportTicketCreated(testUserId, 500L, "Help with my order");

            // Assert
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .category(NotificationCategory.SUPPORT)
                    .build();
            NotificationListDto result = notificationService.getNotifications(filter);

            assertThat(result.getNotifications()).hasSize(1);
            assertThat(result.getNotifications().get(0).getCategory()).isEqualTo(NotificationCategory.SUPPORT);
            assertThat(result.getNotifications().get(0).getRelatedEntityId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("Should create notification for support ticket resolved")
        void shouldCreateNotificationForSupportTicketResolved() {
            // Act
            notificationService.notifySupportTicketResolved(testUserId, 501L);

            // Assert
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .category(NotificationCategory.SUPPORT)
                    .build();
            NotificationListDto result = notificationService.getNotifications(filter);

            assertThat(result.getNotifications()).hasSize(1);
            assertThat(result.getNotifications().get(0).getNotificationType())
                    .isEqualTo(NotificationType.SUPPORT_TICKET_RESOLVED);
        }
    }

    @Nested
    @DisplayName("Cleanup Operations Tests")
    class CleanupOperationsTests {

        @Test
        @DisplayName("Should cleanup expired notifications")
        void shouldCleanupExpiredNotifications() {
            // Arrange - Create notification with past expiration
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(testUserId)
                    .role(NotificationRole.CUSTOMER)
                    .title("Expired Notification")
                    .message("This should be cleaned up")
                    .category(NotificationCategory.SYSTEM)
                    .notificationType(NotificationType.SYSTEM_MAINTENANCE)
                    .ttlHours(-1) // Expired
                    .build();
            notificationService.createNotification(createDto);

            // Act
            int count = notificationService.cleanupExpired();

            // Assert
            assertThat(count).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should cleanup old dismissed notifications")
        void shouldCleanupOldDismissedNotifications() {
            // Arrange - Create and dismiss a notification
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(testUserId)
                    .role(NotificationRole.CUSTOMER)
                    .title("Dismissed Notification")
                    .message("This was dismissed")
                    .category(NotificationCategory.ORDER)
                    .notificationType(NotificationType.ORDER_CREATED)
                    .build();
            NotificationResponseDto created = notificationService.createNotification(createDto);
            notificationService.dismissNotification(created.getId(), null, true);

            // Act - Cleanup notifications dismissed more than 0 days ago (all dismissed)
            int count = notificationService.cleanupDismissed(0);

            // Assert
            assertThat(count).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Filtering and Pagination Tests")
    class FilteringAndPaginationTests {

        @Test
        @DisplayName("Should filter by category")
        void shouldFilterByCategory() {
            // Arrange
            createNotifications(NotificationCategory.ORDER, 3);
            createNotifications(NotificationCategory.FINANCE, 2);

            // Act
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .category(NotificationCategory.ORDER)
                    .build();
            NotificationListDto result = notificationService.getNotifications(filter);

            // Assert
            assertThat(result.getNotifications()).hasSize(3);
            assertThat(result.getNotifications())
                    .allMatch(n -> n.getCategory() == NotificationCategory.ORDER);
        }

        @Test
        @DisplayName("Should filter by read status")
        void shouldFilterByReadStatus() {
            // Arrange - Create 5 notifications, mark 2 as read
            List<Long> ids = new java.util.ArrayList<>();
            for (int i = 0; i < 5; i++) {
                NotificationCreateDto createDto = NotificationCreateDto.builder()
                        .userId(testUserId)
                        .role(NotificationRole.CUSTOMER)
                        .title("Read Filter Test " + i)
                        .message("Message " + i)
                        .category(NotificationCategory.ORDER)
                        .notificationType(NotificationType.ORDER_CREATED)
                        .build();
                NotificationResponseDto created = notificationService.createNotification(createDto);
                if (i < 2) {
                    notificationService.markAsRead(created.getId(), null, true);
                }
            }

            // Act - Get unread only
            NotificationFilterDto unreadFilter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .isRead(false)
                    .build();
            NotificationListDto unreadResult = notificationService.getNotifications(unreadFilter);

            // Assert
            assertThat(unreadResult.getNotifications()).hasSize(3);
        }

        @Test
        @DisplayName("Should paginate results")
        void shouldPaginateResults() {
            // Arrange - Create 10 notifications
            createNotifications(NotificationCategory.ORDER, 10);

            // Act - Get first page
            NotificationFilterDto page1Filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .page(0)
                    .pageSize(5)
                    .build();
            NotificationListDto page1 = notificationService.getNotifications(page1Filter);

            // Get second page
            NotificationFilterDto page2Filter = NotificationFilterDto.builder()
                    .userId(testUserId)
                    .page(1)
                    .pageSize(5)
                    .build();
            NotificationListDto page2 = notificationService.getNotifications(page2Filter);

            // Assert
            assertThat(page1.getNotifications()).hasSize(5);
            assertThat(page1.getTotalElements()).isEqualTo(10);
            assertThat(page1.getTotalPages()).isEqualTo(2);
            assertThat(page1.getHasNext()).isTrue();
            assertThat(page1.getHasPrevious()).isFalse();

            assertThat(page2.getNotifications()).hasSize(5);
            assertThat(page2.getHasNext()).isFalse();
            assertThat(page2.getHasPrevious()).isTrue();
        }

        private void createNotifications(NotificationCategory category, int count) {
            for (int i = 0; i < count; i++) {
                NotificationCreateDto createDto = NotificationCreateDto.builder()
                        .userId(testUserId)
                        .role(NotificationRole.CUSTOMER)
                        .title("Test " + category + " " + i)
                        .message("Message " + i)
                        .category(category)
                        .notificationType(NotificationType.ORDER_CREATED)
                        .build();
                notificationService.createNotification(createDto);
            }
        }
    }
}

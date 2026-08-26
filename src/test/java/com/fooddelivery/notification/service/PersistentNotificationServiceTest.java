package com.fooddelivery.notification.service;

import com.fooddelivery.notification.dto.*;
import com.fooddelivery.notification.mapper.NotificationMapper;
import com.fooddelivery.notification.model.*;
import com.fooddelivery.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersistentNotificationService Unit Tests")
class PersistentNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private PersistentNotificationServiceImpl notificationService;

    private Notification testNotification;
    private NotificationResponseDto testResponseDto;
    private NotificationCreateDto testCreateDto;

    @BeforeEach
    void setUp() {
        testNotification = createTestNotification();
        testResponseDto = createTestResponseDto();
        testCreateDto = createTestCreateDto();
    }

    @Nested
    @DisplayName("createNotification Tests")
    class CreateNotificationTests {

        @Test
        @DisplayName("Should create notification successfully")
        void shouldCreateNotificationSuccessfully() {
            // Arrange
            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            NotificationResponseDto result = notificationService.createNotification(testCreateDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Notification");

            verify(notificationMapper).toEntity(testCreateDto);
            verify(notificationRepository).save(any(Notification.class));
            verify(notificationMapper).toResponseDto(testNotification);
        }

        @Test
        @DisplayName("Should create notification with all fields")
        void shouldCreateNotificationWithAllFields() {
            // Arrange
            NotificationCreateDto createDto = NotificationCreateDto.builder()
                    .userId(100L)
                    .role(NotificationRole.CUSTOMER)
                    .title("Order Update")
                    .message("Your order is ready")
                    .category(NotificationCategory.ORDER)
                    .notificationType(NotificationType.ORDER_READY)
                    .priority(NotificationPriority.HIGH)
                    .orderId(500L)
                    .metadata(Map.of("key", "value"))
                    .build();

            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            NotificationResponseDto result = notificationService.createNotification(createDto);

            // Assert
            assertThat(result).isNotNull();
            verify(notificationMapper).toEntity(createDto);
        }
    }

    @Nested
    @DisplayName("getNotificationById Tests")
    class GetNotificationByIdTests {

        @Test
        @DisplayName("Should get notification by ID")
        void shouldGetNotificationById() {
            // Arrange
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
            when(notificationMapper.toResponseDto(testNotification)).thenReturn(testResponseDto);

            // Act
            NotificationResponseDto result = notificationService.getNotificationById(1L, null, true);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(notificationRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when notification not found")
        void shouldThrowExceptionWhenNotificationNotFound() {
            // Arrange
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> notificationService.getNotificationById(999L, null, true))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Notification not found");

            verify(notificationRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("getNotifications Tests")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should get notifications with filter")
        void shouldGetNotificationsWithFilter() {
            // Arrange
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(1L)
                    .page(0)
                    .pageSize(10)
                    .build();

            List<Notification> notifications = List.of(testNotification);
            Page<Notification> page = new PageImpl<>(notifications);

            when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(notificationMapper.toResponseDtoList(anyList())).thenReturn(List.of(testResponseDto));
            when(notificationRepository.count(any(Specification.class))).thenReturn(1L);

            // Act
            NotificationListDto result = notificationService.getNotifications(filter);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getNotifications()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get empty list when no notifications found")
        void shouldGetEmptyListWhenNoNotificationsFound() {
            // Arrange
            NotificationFilterDto filter = NotificationFilterDto.builder()
                    .userId(999L)
                    .page(0)
                    .pageSize(10)
                    .build();

            Page<Notification> emptyPage = new PageImpl<>(Collections.emptyList());

            when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);
            when(notificationMapper.toResponseDtoList(anyList())).thenReturn(Collections.emptyList());

            // Act
            NotificationListDto result = notificationService.getNotifications(filter);

            // Assert
            assertThat(result.getNotifications()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("markAsRead Tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkNotificationAsRead() {
            // Arrange
            Notification unreadNotification = createTestNotification();
            unreadNotification.setReadAt(null);

            when(notificationRepository.findById(1L)).thenReturn(Optional.of(unreadNotification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
                Notification n = invocation.getArgument(0);
                n.setReadAt(LocalDateTime.now());
                return n;
            });
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            NotificationResponseDto result = notificationService.markAsRead(1L, null, true);

            // Assert
            assertThat(result).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should not update already read notification")
        void shouldNotUpdateAlreadyReadNotification() {
            // Arrange
            Notification readNotification = createTestNotification();
            readNotification.setReadAt(LocalDateTime.now().minusHours(1));

            when(notificationRepository.findById(1L)).thenReturn(Optional.of(readNotification));
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            NotificationResponseDto result = notificationService.markAsRead(1L, null, true);

            // Assert
            assertThat(result).isNotNull();
            verify(notificationRepository, never()).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("markAsReadByIds Tests")
    class MarkAsReadByIdsTests {

        @Test
        @DisplayName("Should mark multiple notifications as read")
        void shouldMarkMultipleNotificationsAsRead() {
            // Arrange
            List<Long> ids = List.of(1L, 2L, 3L);
            when(notificationRepository.markAsReadByIds(anyList(), any(LocalDateTime.class))).thenReturn(3);

            // Act
            int result = notificationService.markAsReadByIds(ids, null, true);

            // Assert
            assertThat(result).isEqualTo(3);
            verify(notificationRepository).markAsReadByIds(eq(ids), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("markAllAsRead Tests")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should mark all notifications as read for user")
        void shouldMarkAllAsReadForUser() {
            // Arrange
            when(notificationRepository.markAllAsReadForUser(eq(1L), any(LocalDateTime.class))).thenReturn(5);

            // Act
            int result = notificationService.markAllAsRead(1L);

            // Assert
            assertThat(result).isEqualTo(5);
            verify(notificationRepository).markAllAsReadForUser(eq(1L), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should mark all notifications as read for user and role")
        void shouldMarkAllAsReadForUserAndRole() {
            // Arrange
            when(notificationRepository.markAllAsReadForUserAndRole(eq(1L), eq(NotificationRole.CUSTOMER), any(LocalDateTime.class)))
                    .thenReturn(3);

            // Act
            int result = notificationService.markAllAsRead(1L, NotificationRole.CUSTOMER);

            // Assert
            assertThat(result).isEqualTo(3);
            verify(notificationRepository).markAllAsReadForUserAndRole(eq(1L), eq(NotificationRole.CUSTOMER), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("deleteNotification Tests")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification")
        void shouldDeleteNotification() {
            // Arrange
            when(notificationRepository.existsById(1L)).thenReturn(true);

            // Act
            notificationService.deleteNotification(1L);

            // Assert
            verify(notificationRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent notification")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            // Arrange
            when(notificationRepository.existsById(999L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> notificationService.deleteNotification(999L))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(notificationRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("dismissNotification Tests")
    class DismissNotificationTests {

        @Test
        @DisplayName("Should dismiss notification")
        void shouldDismissNotification() {
            // Arrange
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            NotificationResponseDto result = notificationService.dismissNotification(1L, null, true);

            // Assert
            assertThat(result).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("performBulkAction Tests")
    class PerformBulkActionTests {

        @Test
        @DisplayName("Should perform bulk mark read action")
        void shouldPerformBulkMarkReadAction() {
            // Arrange
            NotificationBulkActionDto bulkAction = NotificationBulkActionDto.builder()
                    .notificationIds(List.of(1L, 2L, 3L))
                    .action(NotificationBulkActionDto.BulkAction.MARK_READ)
                    .build();

            when(notificationRepository.markAsReadByIds(anyList(), any(LocalDateTime.class))).thenReturn(3);

            // Act
            int result = notificationService.performBulkAction(bulkAction, null, true);

            // Assert
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("Should perform bulk dismiss action")
        void shouldPerformBulkDismissAction() {
            // Arrange
            NotificationBulkActionDto bulkAction = NotificationBulkActionDto.builder()
                    .notificationIds(List.of(1L, 2L))
                    .action(NotificationBulkActionDto.BulkAction.DISMISS)
                    .build();

            when(notificationRepository.dismissByIds(anyList(), any(LocalDateTime.class))).thenReturn(2);

            // Act
            int result = notificationService.performBulkAction(bulkAction, null, true);

            // Assert
            assertThat(result).isEqualTo(2);
        }

        @Test
        @DisplayName("Should perform bulk delete action")
        void shouldPerformBulkDeleteAction() {
            // Arrange
            NotificationBulkActionDto bulkAction = NotificationBulkActionDto.builder()
                    .notificationIds(List.of(1L, 2L, 3L, 4L))
                    .action(NotificationBulkActionDto.BulkAction.DELETE)
                    .build();

            // Act
            int result = notificationService.performBulkAction(bulkAction, null, true);

            // Assert
            assertThat(result).isEqualTo(4);
            verify(notificationRepository).deleteAllById(bulkAction.getNotificationIds());
        }
    }

    @Nested
    @DisplayName("getNotificationCounts Tests")
    class GetNotificationCountsTests {

        @Test
        @DisplayName("Should get notification counts for user")
        void shouldGetNotificationCountsForUser() {
            // Arrange
            when(notificationRepository.countByUserId(1L)).thenReturn(10L);
            when(notificationRepository.countUnreadByUserId(1L)).thenReturn(5L);
            when(notificationRepository.countUnreadByCategoryForUser(1L)).thenReturn(
                    List.of(new Object[]{NotificationCategory.ORDER, 3L}));
            when(notificationRepository.countUnreadByPriorityForUser(1L)).thenReturn(
                    List.of(new Object[]{NotificationPriority.HIGH, 2L}));

            // Act
            NotificationCountDto result = notificationService.getNotificationCounts(1L, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(10L);
            assertThat(result.getUnread()).isEqualTo(5L);
            assertThat(result.getRead()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should get notification counts for user with role filter")
        void shouldGetNotificationCountsForUserWithRoleFilter() {
            // Arrange
            when(notificationRepository.countByUserId(1L)).thenReturn(10L);
            when(notificationRepository.countUnreadByUserIdAndRole(1L, NotificationRole.CUSTOMER)).thenReturn(3L);
            when(notificationRepository.countUnreadByCategoryForUser(1L)).thenReturn(Collections.emptyList());
            when(notificationRepository.countUnreadByPriorityForUser(1L)).thenReturn(Collections.emptyList());

            // Act
            NotificationCountDto result = notificationService.getNotificationCounts(1L, NotificationRole.CUSTOMER);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUnread()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("getUnreadCount Tests")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should get unread count for user")
        void shouldGetUnreadCountForUser() {
            // Arrange
            when(notificationRepository.countUnreadByUserId(1L)).thenReturn(7L);

            // Act
            Long result = notificationService.getUnreadCount(1L);

            // Assert
            assertThat(result).isEqualTo(7L);
        }

        @Test
        @DisplayName("Should get unread count for user and role")
        void shouldGetUnreadCountForUserAndRole() {
            // Arrange
            when(notificationRepository.countUnreadByUserIdAndRole(1L, NotificationRole.COURIER)).thenReturn(4L);

            // Act
            Long result = notificationService.getUnreadCount(1L, NotificationRole.COURIER);

            // Assert
            assertThat(result).isEqualTo(4L);
        }
    }

    @Nested
    @DisplayName("Order Event Notification Tests")
    class OrderEventNotificationTests {

        @Test
        @DisplayName("Should create notification for order created event")
        void shouldCreateNotificationForOrderCreatedEvent() {
            // Arrange
            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(100L)
                    .orderNumber("ORD-100")
                    .customerId(1L)
                    .restaurantId(10L)
                    .eventType(NotificationType.ORDER_CREATED)
                    .restaurantName("Test Restaurant")
                    .totalAmount(BigDecimal.valueOf(25.99))
                    .build();

            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            notificationService.notifyOrderCreated(request);

            // Assert
            verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should create notification for order delivered event")
        void shouldCreateNotificationForOrderDeliveredEvent() {
            // Arrange
            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(100L)
                    .orderNumber("ORD-100")
                    .customerId(1L)
                    .restaurantUserId(10L)
                    .courierUserId(20L)
                    .eventType(NotificationType.ORDER_DELIVERED)
                    .build();

            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            notificationService.notifyOrderDelivered(request);

            // Assert - customer, restaurant, and courier should all be notified
            verify(notificationRepository, times(3)).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should create notification for order cancelled event")
        void shouldCreateNotificationForOrderCancelledEvent() {
            // Arrange
            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(100L)
                    .orderNumber("ORD-100")
                    .customerId(1L)
                    .restaurantUserId(10L)
                    .cancellationReason("Customer requested")
                    .eventType(NotificationType.ORDER_CANCELLED)
                    .build();

            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            notificationService.notifyOrderCancelled(request);

            // Assert - customer, restaurant, and admin should be notified
            verify(notificationRepository, atLeast(3)).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should create notification for courier assigned event")
        void shouldCreateNotificationForCourierAssignedEvent() {
            // Arrange
            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(100L)
                    .orderNumber("ORD-100")
                    .customerId(1L)
                    .restaurantUserId(10L)
                    .courierUserId(20L)
                    .courierName("John Doe")
                    .eventType(NotificationType.COURIER_ASSIGNED)
                    .build();

            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            notificationService.notifyCourierAssigned(request);

            // Assert - customer, courier, and restaurant should be notified
            verify(notificationRepository, times(3)).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("Support Ticket Notification Tests")
    class SupportTicketNotificationTests {

        @Test
        @DisplayName("Should create notification for support ticket created")
        void shouldCreateNotificationForSupportTicketCreated() {
            // Arrange
            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            notificationService.notifySupportTicketCreated(1L, 100L, "Help with order");

            // Assert
            ArgumentCaptor<NotificationCreateDto> captor = ArgumentCaptor.forClass(NotificationCreateDto.class);
            verify(notificationMapper).toEntity(captor.capture());

            NotificationCreateDto capturedDto = captor.getValue();
            assertThat(capturedDto.getCategory()).isEqualTo(NotificationCategory.SUPPORT);
            assertThat(capturedDto.getNotificationType()).isEqualTo(NotificationType.SUPPORT_TICKET_CREATED);
        }

        @Test
        @DisplayName("Should create notification for support ticket updated")
        void shouldCreateNotificationForSupportTicketUpdated() {
            // Arrange
            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            notificationService.notifySupportTicketUpdated(1L, 100L, "Your ticket has been assigned");

            // Assert
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should create notification for support ticket resolved")
        void shouldCreateNotificationForSupportTicketResolved() {
            // Arrange
            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            notificationService.notifySupportTicketResolved(1L, 100L);

            // Assert
            ArgumentCaptor<NotificationCreateDto> captor = ArgumentCaptor.forClass(NotificationCreateDto.class);
            verify(notificationMapper).toEntity(captor.capture());

            NotificationCreateDto capturedDto = captor.getValue();
            assertThat(capturedDto.getNotificationType()).isEqualTo(NotificationType.SUPPORT_TICKET_RESOLVED);
        }
    }

    @Nested
    @DisplayName("Payout Notification Tests")
    class PayoutNotificationTests {

        @Test
        @DisplayName("Should create notification for payout issued")
        void shouldCreateNotificationForPayoutIssued() {
            // Arrange
            Map<String, Object> metadata = Map.of("payoutId", 500L, "periodStart", "2024-01-01");

            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            notificationService.notifyPayoutIssued(1L, NotificationRole.RESTAURANT, "$500.00", metadata);

            // Assert
            ArgumentCaptor<NotificationCreateDto> captor = ArgumentCaptor.forClass(NotificationCreateDto.class);
            verify(notificationMapper).toEntity(captor.capture());

            NotificationCreateDto capturedDto = captor.getValue();
            assertThat(capturedDto.getCategory()).isEqualTo(NotificationCategory.FINANCE);
            assertThat(capturedDto.getMessage()).contains("$500.00");
        }
    }

    @Nested
    @DisplayName("Cleanup Operations Tests")
    class CleanupOperationsTests {

        @Test
        @DisplayName("Should cleanup expired notifications")
        void shouldCleanupExpiredNotifications() {
            // Arrange
            when(notificationRepository.deleteExpired(any(LocalDateTime.class))).thenReturn(10);

            // Act
            int result = notificationService.cleanupExpired();

            // Assert
            assertThat(result).isEqualTo(10);
            verify(notificationRepository).deleteExpired(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should cleanup old dismissed notifications")
        void shouldCleanupOldDismissedNotifications() {
            // Arrange
            when(notificationRepository.deleteDismissedBefore(any(LocalDateTime.class))).thenReturn(5);

            // Act
            int result = notificationService.cleanupDismissed(7);

            // Assert
            assertThat(result).isEqualTo(5);
            verify(notificationRepository).deleteDismissedBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should cleanup old read notifications")
        void shouldCleanupOldReadNotifications() {
            // Arrange
            when(notificationRepository.deleteReadBefore(any(LocalDateTime.class))).thenReturn(20);

            // Act
            int result = notificationService.cleanupOldRead(90);

            // Assert
            assertThat(result).isEqualTo(20);
            verify(notificationRepository).deleteReadBefore(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("createForUser Tests")
    class CreateForUserTests {

        @Test
        @DisplayName("Should create notification for specific user")
        void shouldCreateNotificationForSpecificUser() {
            // Arrange
            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            NotificationResponseDto result = notificationService.createForUser(
                    1L, NotificationRole.CUSTOMER, "Test Title", "Test Message",
                    NotificationType.ORDER_CREATED, Map.of("key", "value"));

            // Assert
            assertThat(result).isNotNull();
            ArgumentCaptor<NotificationCreateDto> captor = ArgumentCaptor.forClass(NotificationCreateDto.class);
            verify(notificationMapper).toEntity(captor.capture());

            NotificationCreateDto capturedDto = captor.getValue();
            assertThat(capturedDto.getUserId()).isEqualTo(1L);
            assertThat(capturedDto.getRole()).isEqualTo(NotificationRole.CUSTOMER);
        }
    }

    @Nested
    @DisplayName("createForRole Tests")
    class CreateForRoleTests {

        @Test
        @DisplayName("Should create broadcast notification for role")
        void shouldCreateBroadcastNotificationForRole() {
            // Arrange
            when(notificationMapper.toEntity(any(NotificationCreateDto.class))).thenReturn(testNotification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            when(notificationMapper.toResponseDto(any(Notification.class))).thenReturn(testResponseDto);

            // Act
            NotificationResponseDto result = notificationService.createForRole(
                    NotificationRole.ALL, "System Update", "Platform maintenance scheduled",
                    NotificationType.SYSTEM_MAINTENANCE, null);

            // Assert
            assertThat(result).isNotNull();
            ArgumentCaptor<NotificationCreateDto> captor = ArgumentCaptor.forClass(NotificationCreateDto.class);
            verify(notificationMapper).toEntity(captor.capture());

            NotificationCreateDto capturedDto = captor.getValue();
            assertThat(capturedDto.getUserId()).isNull(); // Broadcast
            assertThat(capturedDto.getRole()).isEqualTo(NotificationRole.ALL);
        }
    }

    // Helper methods

    private Notification createTestNotification() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(1L);
        notification.setRole(NotificationRole.CUSTOMER);
        notification.setTitle("Test Notification");
        notification.setMessage("Test message");
        notification.setCategory(NotificationCategory.ORDER);
        notification.setNotificationType(NotificationType.ORDER_CREATED);
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }

    private NotificationResponseDto createTestResponseDto() {
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

    private NotificationCreateDto createTestCreateDto() {
        return NotificationCreateDto.builder()
                .userId(1L)
                .role(NotificationRole.CUSTOMER)
                .title("Test Notification")
                .message("Test message")
                .category(NotificationCategory.ORDER)
                .notificationType(NotificationType.ORDER_CREATED)
                .priority(NotificationPriority.NORMAL)
                .build();
    }
}

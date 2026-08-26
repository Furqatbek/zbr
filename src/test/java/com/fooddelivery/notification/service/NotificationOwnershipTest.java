package com.fooddelivery.notification.service;

import com.fooddelivery.notification.dto.NotificationBulkActionDto;
import com.fooddelivery.notification.mapper.NotificationMapper;
import com.fooddelivery.notification.model.Notification;
import com.fooddelivery.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * These endpoints identify a notification by id alone and are reachable by any
 * authenticated user, so the owner check cannot live in @PreAuthorize the way it
 * does on the /user/{userId}/* routes. Notification ids are sequential, so
 * without these checks a customer can walk the whole table.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationOwnershipTest {

    private static final Long OWNER = 1L;
    private static final Long INTRUDER = 2L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private PersistentNotificationServiceImpl service;

    private Notification ownedByOwner() {
        Notification n = new Notification();
        n.setId(99L);
        n.setUserId(OWNER);
        when(notificationRepository.findById(99L)).thenReturn(Optional.of(n));
        return n;
    }

    @Test
    @DisplayName("cannot read another user's notification")
    void cannotReadForeign() {
        ownedByOwner();

        assertThatThrownBy(() -> service.getNotificationById(99L, INTRUDER, false))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("cannot dismiss another user's notification")
    void cannotDismissForeign() {
        ownedByOwner();

        assertThatThrownBy(() -> service.dismissNotification(99L, INTRUDER, false))
                .isInstanceOf(EntityNotFoundException.class);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("cannot mark another user's notification read")
    void cannotMarkForeignRead() {
        ownedByOwner();

        assertThatThrownBy(() -> service.markAsRead(99L, INTRUDER, false))
                .isInstanceOf(EntityNotFoundException.class);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("bulk delete only touches ids the caller owns")
    void bulkDeleteFiltersToOwned() {
        // The caller submits four ids; only one is theirs.
        when(notificationRepository.findOwnedIds(anyList(), any()))
                .thenReturn(List.of(7L));

        NotificationBulkActionDto action = new NotificationBulkActionDto();
        action.setNotificationIds(List.of(5L, 6L, 7L, 8L));
        action.setAction(NotificationBulkActionDto.BulkAction.DELETE);

        int affected = service.performBulkAction(action, INTRUDER, false);

        assertThat(affected).isEqualTo(1);
        verify(notificationRepository).deleteAllById(List.of(7L));
    }

    @Test
    @DisplayName("a caller owning none of the submitted ids changes nothing")
    void bulkWithNoOwnedIdsIsANoOp() {
        when(notificationRepository.findOwnedIds(anyList(), any())).thenReturn(List.of());

        NotificationBulkActionDto action = new NotificationBulkActionDto();
        action.setNotificationIds(List.of(5L, 6L));
        action.setAction(NotificationBulkActionDto.BulkAction.DELETE);

        assertThat(service.performBulkAction(action, INTRUDER, false)).isZero();
        verify(notificationRepository, never()).deleteAllById(anyList());
    }

    @Test
    @DisplayName("the owner still reaches their own notification")
    void ownerUnaffected() {
        ownedByOwner();

        assertThatCode(() -> service.getNotificationById(99L, OWNER, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("an admin reaches any notification")
    void adminUnaffected() {
        ownedByOwner();

        assertThatCode(() -> service.getNotificationById(99L, INTRUDER, true))
                .doesNotThrowAnyException();
    }
}

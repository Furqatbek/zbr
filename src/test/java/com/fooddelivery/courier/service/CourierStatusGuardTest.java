package com.fooddelivery.courier.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.courier.repository.CourierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A courier reaches updateStatus only through their own /me/status endpoint, so
 * it must not be a way out of an administrative state. Clearing SUSPENDED or
 * PENDING_APPROVAL belongs to activateCourier, behind an ADMIN/PLATFORM
 * endpoint.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourierStatusGuardTest {

    @Mock
    private CourierRepository courierRepository;

    @InjectMocks
    private CourierService courierService;

    private Courier courierWith(CourierStatus status) {
        Courier courier = new Courier();
        courier.setId(7L);
        courier.setStatus(status);
        // Verified on purpose: this is a courier who was approved and then
        // suspended, so the "must be verified to go online" check passes and
        // only the administrative-state guard stands in the way.
        courier.setVerified(true);
        // toDto dereferences the user on the success path.
        User user = new User();
        user.setId(42L);
        courier.setUser(user);
        when(courierRepository.findByIdWithUser(7L)).thenReturn(Optional.of(courier));
        return courier;
    }

    @Test
    @DisplayName("a suspended courier cannot lift their own suspension")
    void suspendedCourierCannotSelfClear() {
        courierWith(CourierStatus.SUSPENDED);

        assertThatThrownBy(() -> courierService.updateStatus(7L, CourierStatus.AVAILABLE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("suspended");

        verify(courierRepository, never()).save(any());
    }

    @Test
    @DisplayName("a courier awaiting approval cannot approve themselves")
    void pendingCourierCannotSelfApprove() {
        courierWith(CourierStatus.PENDING_APPROVAL);

        assertThatThrownBy(() -> courierService.updateStatus(7L, CourierStatus.OFFLINE))
                .isInstanceOf(BusinessException.class);

        verify(courierRepository, never()).save(any());
    }

    @Test
    @DisplayName("an ordinary courier can still go online")
    void activeCourierMayGoOnline() {
        Courier courier = courierWith(CourierStatus.OFFLINE);
        when(courierRepository.save(courier)).thenReturn(courier);

        courierService.updateStatus(7L, CourierStatus.AVAILABLE);

        assertThat(courier.getStatus()).isEqualTo(CourierStatus.AVAILABLE);
        verify(courierRepository).save(courier);
    }
}

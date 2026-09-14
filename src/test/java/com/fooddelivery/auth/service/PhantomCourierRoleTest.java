package com.fooddelivery.auth.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.ConsumerAddressRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A role is not a profile.
 *
 * <p>Granting COURIER to an account with no {@code couriers} row produced a
 * phantom: it passed every authorization check, was listed as a courier by
 * {@code GET /users}, and returned 404 from every {@code /couriers/me} endpoint
 * because it owned nothing. Two of those reached production and cost two
 * separate rounds of bug reports — once as "cannot change status", once as
 * "/users shows couriers that are not in /couriers".
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService.addRole — no phantom couriers")
class PhantomCourierRoleTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserDeviceTokenRepository deviceTokenRepository;
    @Mock private ConsumerAddressRepository consumerAddressRepository;
    @Mock private CourierRepository courierRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private UserService userService;

    private User user() {
        return User.builder().id(11L).email("u11@example.com")
                .role(Role.CONSUMER).status(UserStatus.ACTIVE).build();
    }

    @Test
    @DisplayName("refuses COURIER when the user has no courier profile")
    void refusesPhantomCourier() {
        when(userRepository.findById(11L)).thenReturn(Optional.of(user()));
        when(courierRepository.existsByUserId(11L)).thenReturn(false);

        assertThatThrownBy(() -> userService.addRole(11L, Role.COURIER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no courier profile")
                .hasMessageContaining("/api/v1/couriers/register");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("allows COURIER when the profile already exists — a repair, not a phantom")
    void allowsWhenProfileExists() {
        User user = user();
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(courierRepository.existsByUserId(11L)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.addRole(11L, Role.COURIER);

        assertThat(user.hasRole(Role.COURIER)).isTrue();
    }

    @Test
    @DisplayName("other roles are unaffected")
    void otherRolesStillGrantable() {
        User user = user();
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(courierRepository.existsByUserId(11L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // RESTAURANT_OWNER has its own legitimate admin path (ownership
        // transfer), and the staff roles have no profile concept at all.
        userService.addRole(11L, Role.RESTAURANT_OWNER);

        assertThat(user.hasRole(Role.RESTAURANT_OWNER)).isTrue();
    }
}

package com.fooddelivery.auth.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.ConsumerAddressRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Account deletion must be a real erasure, not a deactivation — app stores
 * reject "delete" flows that only flip a status flag.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService.deleteAccount — real erasure")
class UserServiceDeleteAccountTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserDeviceTokenRepository deviceTokenRepository;
    @Mock private ConsumerAddressRepository consumerAddressRepository;

    @InjectMocks private UserService userService;

    private User existingUser() {
        return User.builder()
                .id(42L)
                .email("real.person@example.com")
                .phone("+998901234567")
                .firstName("Real")
                .lastName("Person")
                .profileImageUrl("https://cdn/avatar.png")
                .status(UserStatus.ACTIVE)
                .passwordHash("$2a$12$somebcrypthash")
                .build();
    }

    @Test
    @DisplayName("erases personal data, releases email/phone, and revokes access")
    void erasesPersonalData() {
        User user = existingUser();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByUserId(42L)).thenReturn(List.of());

        userService.deleteAccount(42L);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User erased = saved.getValue();

        // No personally identifying data may survive.
        assertThat(erased.getEmail()).doesNotContain("real.person");
        assertThat(erased.getPhone()).doesNotContain("901234567");
        assertThat(erased.getFirstName()).isEqualTo("Deleted");
        assertThat(erased.getLastName()).isEqualTo("User");
        assertThat(erased.getProfileImageUrl()).isNull();
        assertThat(erased.getStatus()).isEqualTo(UserStatus.DELETED);

        // Identifiers released so the person can sign up again.
        assertThat(erased.getEmail()).isEqualTo("deleted-42@deleted.invalid");
        assertThat(erased.getPhone()).isEqualTo("deleted-42");

        // Credential must be unusable.
        assertThat(erased.getPasswordHash()).isEqualTo("ACCOUNT_DELETED");

        // Sessions, push tokens and saved addresses are gone.
        verify(refreshTokenRepository).revokeAllUserTokens(anyLong(), any(), anyString());
        verify(consumerAddressRepository).deleteByUserId(42L);
    }

    @Test
    @DisplayName("deletes every registered device token")
    void deletesDeviceTokens() {
        User user = existingUser();
        var token = new com.fooddelivery.notification.entity.UserDeviceToken();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByUserId(42L)).thenReturn(List.of(token));

        userService.deleteAccount(42L);

        verify(deviceTokenRepository).delete(token);
    }
}

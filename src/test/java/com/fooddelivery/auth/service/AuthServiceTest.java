package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.LoginRequest;
import com.fooddelivery.auth.dto.RefreshTokenRequest;
import com.fooddelivery.auth.dto.RegisterRequest;
import com.fooddelivery.auth.entity.RefreshToken;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.PasswordResetTokenRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.JwtService;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.notification.service.NotificationService;
import com.fooddelivery.platform.service.ReferralService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guard-path tests for {@link AuthService}: the security checks that must reject
 * bad input before any token is minted — duplicate accounts, locked/inactive
 * logins, and invalid/revoked/expired refresh tokens. The happy paths depend on
 * heavy JWT/principal plumbing and are exercised by the integration suite.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService guard-path tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private NotificationService notificationService;
    @Mock private ReferralService referralService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("register")
    class Register {

        @ParameterizedTest
        @EnumSource(value = Role.class, names = {"ADMIN", "PLATFORM", "SYSTEM",
                "OPERATIONS_MANAGER", "FINANCE_MANAGER", "SECURITY_ANALYST", "RESTAURANT_STAFF"})
        @DisplayName("refuses to self-register a privileged role (public endpoint)")
        void rejectsPrivilegedSelfRegistration(Role privileged) {
            RegisterRequest request = RegisterRequest.builder()
                    .email("attacker@example.com")
                    .phone("+998900000001")
                    .role(privileged)
                    .build();

            assertThatThrownBy(() -> authService.register(request, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cannot be self-registered");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a duplicate email")
        void rejectsDuplicateEmail() {
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            RegisterRequest request = RegisterRequest.builder()
                    .email("taken@example.com")
                    .phone("+998900000000")
                    .build();

            assertThatThrownBy(() -> authService.register(request, null))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("rejects a duplicate phone")
        void rejectsDuplicatePhone() {
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByPhone("+998900000000")).thenReturn(true);

            RegisterRequest request = RegisterRequest.builder()
                    .email("new@example.com")
                    .phone("+998900000000")
                    .build();

            assertThatThrownBy(() -> authService.register(request, null))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("rejects an unknown user with bad credentials")
        void rejectsUnknownUser() {
            when(userRepository.findByEmailOrPhone(anyString(), anyString())).thenReturn(Optional.empty());

            LoginRequest request = LoginRequest.builder()
                    .emailOrPhone("ghost@example.com")
                    .password("whatever")
                    .build();

            assertThatThrownBy(() -> authService.login(request, null))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("rejects a locked account")
        void rejectsLockedAccount() {
            User user = mock(User.class);
            when(user.isAccountLocked()).thenReturn(true);
            when(userRepository.findByEmailOrPhone(anyString(), anyString())).thenReturn(Optional.of(user));

            LoginRequest request = LoginRequest.builder()
                    .emailOrPhone("locked@example.com")
                    .password("secret")
                    .build();

            assertThatThrownBy(() -> authService.login(request, null))
                    .isInstanceOf(LockedException.class);
        }

        @Test
        @DisplayName("rejects a non-active account")
        void rejectsInactiveAccount() {
            User user = mock(User.class);
            when(user.isAccountLocked()).thenReturn(false);
            when(user.getStatus()).thenReturn(UserStatus.SUSPENDED);
            when(userRepository.findByEmailOrPhone(anyString(), anyString())).thenReturn(Optional.of(user));

            LoginRequest request = LoginRequest.builder()
                    .emailOrPhone("suspended@example.com")
                    .password("secret")
                    .build();

            assertThatThrownBy(() -> authService.login(request, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active");
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class Refresh {

        @Test
        @DisplayName("rejects a token that does not exist")
        void rejectsUnknownToken() {
            when(refreshTokenRepository.findByTokenAndRevokedFalse("nope")).thenReturn(Optional.empty());
            when(refreshTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("nope").build();

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("rejects a token that has been revoked")
        void rejectsRevokedToken() {
            when(refreshTokenRepository.findByTokenAndRevokedFalse("revoked")).thenReturn(Optional.empty());
            when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(mock(RefreshToken.class)));

            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("revoked").build();

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("revoked");
        }

        @Test
        @DisplayName("rejects and revokes an expired token")
        void rejectsExpiredToken() {
            RefreshToken token = mock(RefreshToken.class);
            when(token.isExpired()).thenReturn(true);
            when(token.getUser()).thenReturn(mock(User.class));
            when(refreshTokenRepository.findByTokenAndRevokedFalse("expired")).thenReturn(Optional.of(token));

            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("expired").build();

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expired");
            verify(token).revoke("Expired");
            verify(refreshTokenRepository).save(token);
        }
    }
}

package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.AuthResponse;
import com.fooddelivery.auth.dto.LoginRequest;
import com.fooddelivery.auth.dto.RefreshTokenRequest;
import com.fooddelivery.auth.dto.RegisterRequest;
import com.fooddelivery.auth.entity.RefreshToken;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.PasswordResetTokenRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.JwtService;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.notification.service.NotificationService;
import com.fooddelivery.platform.service.ReferralService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReferralService referralService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.CONSUMER);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .phone("+1234567890")
                .roles(roles)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build();
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUser() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("new@example.com")
                    .password("Password123!")
                    .firstName("New")
                    .lastName("User")
                    .phone("+1234567890")
                    .role(Role.CONSUMER)
                    .build();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("refreshToken");
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            AuthResponse response = authService.register(request, httpServletRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
            assertThat(response.getEmail()).isEqualTo("new@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("existing@example.com")
                    .password("Password123!")
                    .firstName("Existing")
                    .lastName("User")
                    .build();

            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request, httpServletRequest))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() {
            LoginRequest request = LoginRequest.builder()
                    .emailOrPhone("test@example.com")
                    .password("correctPassword")
                    .build();

            UserPrincipal userPrincipal = UserPrincipal.create(testUser);
            Authentication authentication = mock(Authentication.class);

            when(userRepository.findByEmailOrPhone(anyString(), anyString())).thenReturn(Optional.of(testUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userPrincipal);
            when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("refreshToken");
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            AuthResponse response = authService.login(request, httpServletRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void shouldThrowExceptionForInvalidCredentials() {
            LoginRequest request = LoginRequest.builder()
                    .emailOrPhone("test@example.com")
                    .password("wrongPassword")
                    .build();

            when(userRepository.findByEmailOrPhone(anyString(), anyString())).thenReturn(Optional.of(testUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception for inactive user")
        void shouldThrowExceptionForInactiveUser() {
            testUser.setStatus(UserStatus.SUSPENDED);
            LoginRequest request = LoginRequest.builder()
                    .emailOrPhone("test@example.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByEmailOrPhone(anyString(), anyString())).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("Should throw exception for locked user")
        void shouldThrowExceptionForLockedUser() {
            testUser.setLockedUntil(LocalDateTime.now().plusHours(1));
            LoginRequest request = LoginRequest.builder()
                    .emailOrPhone("test@example.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByEmailOrPhone(anyString(), anyString())).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                    .isInstanceOf(LockedException.class)
                    .hasMessageContaining("locked");
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            LoginRequest request = LoginRequest.builder()
                    .emailOrPhone("nonexistent@example.com")
                    .password("password")
                    .build();

            when(userRepository.findByEmailOrPhone(anyString(), anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("validRefreshToken")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("validRefreshToken")
                    .build();

            when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                    .thenReturn(Optional.of(refreshToken));
            when(jwtService.isTokenValid(anyString(), any(UserPrincipal.class))).thenReturn(true);
            when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("newAccessToken");
            when(jwtService.getAccessTokenExpiration()).thenReturn(3600000L);

            AuthResponse response = authService.refreshToken(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalidToken")
                    .build();

            when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        @DisplayName("Should throw exception for expired refresh token")
        void shouldThrowExceptionForExpiredRefreshToken() {
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("expiredToken")
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .revoked(false)
                    .build();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("expiredToken")
                    .build();

            when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                    .thenReturn(Optional.of(refreshToken));

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expired");
        }
    }
}

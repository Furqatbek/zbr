package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.AuthResponse;
import com.fooddelivery.auth.dto.LoginRequest;
import com.fooddelivery.auth.dto.RegisterRequest;
import com.fooddelivery.auth.entity.RefreshToken;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.JwtService;
import com.fooddelivery.common.exception.BadRequestException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

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
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .fullName("Test User")
                .phone("+1234567890")
                .role(Role.CONSUMER)
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
                    .fullName("New User")
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
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            AuthResponse response = authService.register(request);

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
                    .fullName("Existing User")
                    .build();

            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email already registered");

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
                    .email("test@example.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void shouldThrowExceptionForInvalidCredentials() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("wrongPassword")
                    .build();

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("Should throw exception for inactive user")
        void shouldThrowExceptionForInactiveUser() {
            testUser.setStatus(UserStatus.SUSPENDED);
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Account is not active");
        }

        @Test
        @DisplayName("Should throw exception for locked user")
        void shouldThrowExceptionForLockedUser() {
            testUser.setLockedUntil(LocalDateTime.now().plusHours(1));
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("correctPassword")
                    .build();

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Account is temporarily locked");
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            LoginRequest request = LoginRequest.builder()
                    .email("nonexistent@example.com")
                    .password("password")
                    .build();

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
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

            when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                    .thenReturn(Optional.of(refreshToken));
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("newAccessToken");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("newRefreshToken");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            AuthResponse response = authService.refreshToken("validRefreshToken");

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            verify(refreshTokenRepository).save(argThat(token -> token.isRevoked()));
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken("invalidToken"))
                    .isInstanceOf(UnauthorizedException.class)
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

            when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                    .thenReturn(Optional.of(refreshToken));

            assertThatThrownBy(() -> authService.refreshToken("expiredToken"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Refresh token expired");
        }
    }
}

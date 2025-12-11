package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.entity.*;
import com.fooddelivery.auth.repository.PasswordResetTokenRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.JwtService;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.notification.service.NotificationService;
import com.fooddelivery.platform.service.ReferralService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final NotificationService notificationService;
    private final ReferralService referralService;

    /**
     * Register a new user.
     */
    @Transactional
    @Auditable(action = "REGISTER", entityType = "User")
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check for existing user
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("User", "phone", request.getPhone());
        }

        // Create user
        Set<Role> roles = new HashSet<>();
        roles.add(request.getRole() != null ? request.getRole() : Role.CONSUMER);

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(roles)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with id: {}", user.getId());

        // Process referral code if provided
        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            try {
                referralService.processReferral(request.getReferralCode(), user.getId());
            } catch (Exception e) {
                log.warn("Failed to process referral code: {}", e.getMessage());
            }
        }

        // Generate tokens
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Save refresh token
        saveRefreshToken(user, refreshToken, httpRequest);

        // Send welcome notification
        try {
            notificationService.sendWelcomeEmail(user);
        } catch (Exception e) {
            log.warn("Failed to send welcome email: {}", e.getMessage());
        }

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Authenticate user and return tokens.
     */
    @Transactional
    @Auditable(action = "LOGIN", entityType = "User")
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for: {}", request.getEmailOrPhone());

        // Find user by email or phone
        User user = userRepository.findByEmailOrPhone(request.getEmailOrPhone(), request.getEmailOrPhone())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new LockedException("Account is locked. Please try again later or reset your password.");
        }

        // Check account status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is not active. Status: " + user.getStatus());
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // Reset failed attempts and update last login
            user.recordLogin();
            userRepository.save(user);

            // Generate tokens
            String accessToken = jwtService.generateAccessToken(userPrincipal);
            String refreshToken = jwtService.generateRefreshToken(userPrincipal);

            // Save refresh token
            saveRefreshToken(user, refreshToken, httpRequest);

            log.info("User logged in successfully: {}", user.getEmail());

            return buildAuthResponse(user, accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenStr = request.getRefreshToken();

        // Validate refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenStr)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshToken.revoke("Expired");
            refreshTokenRepository.save(refreshToken);
            throw new BusinessException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        UserPrincipal userPrincipal = UserPrincipal.create(user);

        // Verify JWT token
        if (!jwtService.isTokenValid(refreshTokenStr, userPrincipal)) {
            throw new BusinessException("Invalid refresh token");
        }

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(userPrincipal);

        log.info("Token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles())
                .build();
    }

    /**
     * Logout user by revoking refresh token.
     */
    @Transactional
    @Auditable(action = "LOGOUT", entityType = "User")
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElse(null);

        if (token != null && !token.getRevoked()) {
            token.revoke("User logout");
            refreshTokenRepository.save(token);
            log.info("User logged out, refresh token revoked");
        }
    }

    /**
     * Logout user from all devices.
     */
    @Transactional
    @Auditable(action = "LOGOUT_ALL", entityType = "User")
    public void logoutAll(Long userId) {
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(
                userId,
                LocalDateTime.now(),
                "Logout from all devices"
        );
        log.info("Revoked {} refresh tokens for user {}", revokedCount, userId);
    }

    /**
     * Initiate password reset.
     */
    @Transactional
    @Auditable(action = "PASSWORD_RESET_REQUEST", entityType = "User")
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Always return success to prevent email enumeration
        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            return;
        }

        // Invalidate existing tokens
        passwordResetTokenRepository.invalidateUserTokens(user.getId(), LocalDateTime.now());

        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send email
        try {
            notificationService.sendPasswordResetEmail(user, token);
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
        }

        log.info("Password reset token generated for user: {}", user.getEmail());
    }

    /**
     * Confirm password reset with token.
     */
    @Transactional
    @Auditable(action = "PASSWORD_RESET_CONFIRM", entityType = "User")
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            throw new BusinessException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user.getId(), LocalDateTime.now(), "Password reset");

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    private void saveRefreshToken(User user, String token, HttpServletRequest request) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .deviceInfo(request != null ? request.getHeader("User-Agent") : null)
                .ipAddress(request != null ? getClientIp(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .expiresAt(LocalDateTime.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles())
                .build();
    }
}

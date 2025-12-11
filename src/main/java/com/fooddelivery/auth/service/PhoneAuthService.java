package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.entity.OtpCode;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.JwtService;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for phone-based authentication (OTP login/signup).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneAuthService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Initiate phone login/signup by sending OTP.
     *
     * @param request the phone login request
     * @return response with OTP send status
     */
    @Transactional
    public OtpSendResponse initiatePhoneAuth(PhoneLoginRequest request) {
        String phone = normalizePhone(request.getPhone());

        // Check if user exists
        Optional<User> existingUser = userRepository.findByPhone(phone);
        boolean isNewUser = existingUser.isEmpty();

        // Determine OTP purpose
        OtpCode.OtpPurpose purpose = isNewUser ? OtpCode.OtpPurpose.SIGNUP : OtpCode.OtpPurpose.LOGIN;

        // Generate and send OTP
        OtpCode otp = otpService.generateAndSendOtp(phone, purpose);

        log.info("OTP sent for phone auth: phone={}, isNewUser={}", maskPhone(phone), isNewUser);

        return OtpSendResponse.builder()
                .phone(maskPhone(phone))
                .message("Verification code sent to your phone")
                .expiresInSeconds(otpService.getSecondsUntilExpiry(phone))
                .isNewUser(isNewUser)
                .remainingAttempts(otp.getMaxAttempts())
                .build();
    }

    /**
     * Verify OTP and complete login/signup.
     *
     * @param request the OTP verification request
     * @return authentication response with tokens
     */
    @Transactional
    public AuthResponse verifyOtpAndAuthenticate(OtpVerifyRequest request) {
        String phone = normalizePhone(request.getPhone());

        // Verify OTP
        boolean isValid = otpService.verifyOtp(phone, request.getCode());

        if (!isValid) {
            int remaining = otpService.getRemainingAttempts(phone);
            if (remaining > 0) {
                throw new BusinessException("Invalid verification code. " + remaining + " attempts remaining.");
            } else {
                throw new BusinessException("Too many invalid attempts. Please request a new code.");
            }
        }

        // Find or create user
        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createNewConsumer(phone));

        // Ensure user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new BusinessException("Your account has been suspended. Please contact support.");
            }
            // Activate pending users on successful OTP verification
            user.setStatus(UserStatus.ACTIVE);
            user = userRepository.save(user);
        }

        // Mark phone as verified
        if (!Boolean.TRUE.equals(user.getPhoneVerified())) {
            user.setPhoneVerified(true);
            user = userRepository.save(user);
        }

        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Phone auth successful: userId={}, isNewUser={}",
                user.getId(), user.getCreatedAt().equals(user.getUpdatedAt()));

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Resend OTP code.
     *
     * @param request the phone login request
     * @return response with OTP send status
     */
    @Transactional
    public OtpSendResponse resendOtp(PhoneLoginRequest request) {
        return initiatePhoneAuth(request);
    }

    /**
     * Update consumer profile.
     *
     * @param userId  the user ID
     * @param request the profile update request
     * @return updated user DTO
     */
    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Update fields if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email is already taken
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Email is already in use");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false); // Require re-verification
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getLatitude() != null) {
            user.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            user.setLongitude(request.getLongitude());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);

        return mapToUserDto(user);
    }

    /**
     * Get consumer profile.
     */
    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToUserDto(user);
    }

    /**
     * Create new consumer user.
     */
    private User createNewConsumer(String phone) {
        User user = User.builder()
                .phone(phone)
                .phoneVerified(true)
                .role(Role.CONSUMER)
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("New consumer created via phone auth: userId={}, phone={}",
                user.getId(), maskPhone(phone));

        return user;
    }

    /**
     * Map User entity to UserDto.
     */
    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .address(user.getAddress())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Normalize phone number.
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        String digits = phone.replaceAll("[^0-9+]", "");

        if (digits.startsWith("+998")) {
            return digits.substring(1);
        } else if (digits.startsWith("998")) {
            return digits;
        } else if (digits.startsWith("8") && digits.length() == 10) {
            return "998" + digits.substring(1);
        } else if (digits.length() == 9 && !digits.startsWith("+")) {
            return "998" + digits;
        }

        return digits.startsWith("+") ? digits.substring(1) : digits;
    }

    /**
     * Mask phone for logging.
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}

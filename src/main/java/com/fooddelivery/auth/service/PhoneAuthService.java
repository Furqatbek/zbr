package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.entity.OtpCode;
import com.fooddelivery.auth.entity.RefreshToken;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.JwtService;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final ConsumerAddressService addressService;
    private final OrderRepository orderRepository;

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

        // Ensure user is active.
        //
        // Only PENDING_VERIFICATION may be promoted here — that is what "verify
        // your phone" means. This previously blocked SUSPENDED and promoted
        // EVERYTHING else to ACTIVE, so a BANNED or INACTIVE account un-banned
        // itself simply by logging in with an OTP.
        if (user.getStatus() != UserStatus.ACTIVE) {
            switch (user.getStatus()) {
                case PENDING_VERIFICATION -> {
                    user.setStatus(UserStatus.ACTIVE);
                    user = userRepository.save(user);
                }
                case SUSPENDED -> throw new BusinessException(
                        "Your account has been suspended. Please contact support.");
                default -> {
                    log.warn("SECURITY: OTP login attempted on {} account (user {})",
                            user.getStatus(), user.getId());
                    throw new BusinessException(
                            "This account cannot be used. Please contact support.");
                }
            }
        }

        // Mark phone as verified
        if (!Boolean.TRUE.equals(user.getPhoneVerified())) {
            user.setPhoneVerified(true);
            user = userRepository.save(user);
        }

        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Save refresh token to database
        saveRefreshToken(user, refreshToken);

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
     * Complete phone registration for new users with additional details.
     *
     * @param request the complete registration request
     * @return authentication response with tokens
     */
    @Transactional
    public AuthResponse completeRegistration(CompleteRegistrationRequest request) {
        String phone = normalizePhone(request.getPhone());

        // Verify OTP
        boolean isValid = otpService.verifyOtp(phone, request.getOtp());

        if (!isValid) {
            int remaining = otpService.getRemainingAttempts(phone);
            if (remaining > 0) {
                throw new BusinessException("Invalid verification code. " + remaining + " attempts remaining.");
            } else {
                throw new BusinessException("Too many invalid attempts. Please request a new code.");
            }
        }

        // Check if user already exists
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new BusinessException("User already exists. Please use login instead.");
        }

        // Check if email is already taken
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email is already in use");
        }

        // Parse full name into first and last name
        String[] nameParts = request.getFullName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Create new user with provided details
        User user = User.builder()
                .phone(phone)
                .phoneVerified(true)
                .email(request.getEmail())
                .emailVerified(false)
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.CONSUMER)
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("New consumer registration completed: userId={}, phone={}", user.getId(), maskPhone(phone));

        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Save refresh token to database
        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(mapToUserDto(user))
                .build();
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

        // fullName first, so an explicit firstName/lastName in the same request
        // still wins. Jackson ignores unknown properties, so a client sending
        // only fullName previously updated NOTHING and still got 200 — the
        // update looked successful and silently discarded the name.
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            String[] parts = request.getFullName().trim().split("\\s+", 2);
            user.setFirstName(parts[0]);
            user.setLastName(parts.length > 1 ? parts[1] : "");
        }

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

        // Return the SAME shape as GET /consumers/profile. Returning a thinner
        // object made a successful update look like a partial one — clients that
        // replaced their cached profile with the response lost the address and
        // order count until the next full fetch.
        return mapToConsumerProfile(user);
    }

    /**
     * Get consumer profile.
     */
    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToConsumerProfile(user);
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiration() / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
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
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .address(user.getAddress())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .lastSeenAt(user.getLastSeenAt())
                .build();
    }

    /**
     * The full consumer profile: everything in {@link #mapToUserDto} plus the
     * default address and lifetime order count.
     *
     * <p>Separate from mapToUserDto because it costs two extra queries. Login
     * and OTP verification return the lean shape — they are on the hot path and
     * the client fetches the profile straight afterwards anyway.
     */
    private UserDto mapToConsumerProfile(User user) {
        UserDto dto = mapToUserDto(user);
        dto.setAvatarUrl(user.getProfileImageUrl());
        dto.setMemberSince(user.getCreatedAt());
        dto.setDefaultAddress(addressService.getDefaultAddress(user.getId()));
        dto.setTotalOrders(orderRepository.countByConsumerId(user.getId()));
        return dto;
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

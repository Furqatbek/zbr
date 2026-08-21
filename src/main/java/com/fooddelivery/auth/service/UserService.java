package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.ChangePasswordRequest;
import com.fooddelivery.auth.dto.UpdateUserRequest;
import com.fooddelivery.auth.dto.UserDto;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import java.time.LocalDateTime;

import com.fooddelivery.auth.repository.ConsumerAddressRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDeviceTokenRepository deviceTokenRepository;
    private final ConsumerAddressRepository consumerAddressRepository;

    /**
     * Get user by ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToDto(user);
    }

    /**
     * Get user by email.
     */
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToDto(user);
    }

    /**
     * Get user entity by ID (internal use).
     */
    @Transactional(readOnly = true)
    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    /**
     * Update user profile.
     */
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @Auditable(action = "UPDATE_PROFILE", entityType = "User")
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            // Check if phone is already taken
            if (!request.getPhone().equals(user.getPhone()) &&
                    userRepository.existsByPhone(request.getPhone())) {
                throw new DuplicateResourceException("User", "phone", request.getPhone());
            }
            user.setPhone(request.getPhone());
            user.setPhoneVerified(false);
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        user = userRepository.save(user);
        log.info("User profile updated: {}", user.getEmail());

        return mapToDto(user);
    }

    /**
     * Change user password.
     */
    @Transactional
    @Auditable(action = "CHANGE_PASSWORD", entityType = "User")
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Check if new password is different
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getEmail());
    }

    /**
     * Update user status (admin only).
     */
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @Auditable(action = "UPDATE_STATUS", entityType = "User")
    public UserDto updateUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setStatus(status);
        user = userRepository.save(user);

        log.info("User status updated: {} -> {}", user.getEmail(), status);

        return mapToDto(user);
    }

    /**
     * Add role to user (admin only).
     */
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @Auditable(action = "ADD_ROLE", entityType = "User")
    public UserDto addRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.addRole(role);
        user = userRepository.save(user);

        log.info("Role {} added to user: {}", role, user.getEmail());

        return mapToDto(user);
    }

    /**
     * Remove role from user (admin only).
     */
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @Auditable(action = "REMOVE_ROLE", entityType = "User")
    public UserDto removeRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (user.getRoles().size() <= 1) {
            throw new BusinessException("User must have at least one role");
        }

        user.removeRole(role);
        user = userRepository.save(user);

        log.info("Role {} removed from user: {}", role, user.getEmail());

        return mapToDto(user);
    }

    /**
     * Get all users with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return PagedResponse.from(users, users.getContent().stream()
                .map(this::mapToDto)
                .toList());
    }

    /**
     * Get users by role.
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getUsersByRole(Role role, Pageable pageable) {
        Page<User> users = userRepository.findByRole(role, pageable);
        return PagedResponse.from(users, users.getContent().stream()
                .map(this::mapToDto)
                .toList());
    }

    /**
     * Get users by status.
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getUsersByStatus(UserStatus status, Pageable pageable) {
        Page<User> users = userRepository.findByStatus(status, pageable);
        return PagedResponse.from(users, users.getContent().stream()
                .map(this::mapToDto)
                .toList());
    }

    /**
     * Search users.
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserDto> searchUsers(String query, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(query, pageable);
        return PagedResponse.from(users, users.getContent().stream()
                .map(this::mapToDto)
                .toList());
    }

    /**
     * Delete user (soft delete).
     */
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @Auditable(action = "DELETE_USER", entityType = "User")
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        log.info("User soft deleted: {}", user.getEmail());
    }

    /**
     * Permanently erase a user's account and personal data (self-service deletion).
     *
     * This is a real deletion, not a deactivation: every personal field is
     * destroyed and the unique identifiers (email, phone) are released so the
     * person can sign up again. The row itself is retained ONLY as an anonymous
     * stub because orders/payments reference it by foreign key and must survive
     * for financial and tax records — but nothing personally identifying remains.
     *
     * Effect is immediate and irreversible; there is no grace period.
     */
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @Auditable(action = "DELETE_ACCOUNT", entityType = "User")
    public void deleteAccount(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Release the unique identifiers so the address/number can be reused,
        // using per-user placeholders so repeated deletions cannot collide.
        user.setEmail("deleted-" + id + "@deleted.invalid");
        user.setPhone("deleted-" + id);

        // Destroy the remaining personal data.
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setProfileImageUrl(null);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        // Unusable credential — no bcrypt hash matches this, so it cannot be logged into.
        user.setPasswordHash("ACCOUNT_DELETED");
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        // Kill every active session and stop all push to the device.
        refreshTokenRepository.revokeAllUserTokens(id, LocalDateTime.now(), "Account deleted");
        deviceTokenRepository.findByUserId(id).forEach(deviceTokenRepository::delete);

        // Saved delivery addresses are personal data too.
        consumerAddressRepository.deleteByUserId(id);

        log.info("Account permanently deleted and personal data erased for user id {}", id);
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(new HashSet<>(user.getRoles()))
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

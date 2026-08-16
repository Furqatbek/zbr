package com.fooddelivery.notification.service;

import com.fooddelivery.notification.dto.DeviceTokenRequest;
import com.fooddelivery.notification.entity.UserDeviceToken;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing user device tokens for push notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeviceTokenService {

    private final UserDeviceTokenRepository deviceTokenRepository;

    /**
     * Register or update a device token for a user.
     * If the token already exists for another user, it will be reassigned.
     */
    public UserDeviceToken registerToken(Long userId, DeviceTokenRequest request) {
        log.info("Registering device token for user {}", userId);

        // Prefer upserting on (user, deviceId): the OS rotates push tokens, and
        // keying on the token alone would leave the old row behind so the device
        // receives duplicate pushes. One row per physical device.
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
            Optional<UserDeviceToken> byDevice =
                    deviceTokenRepository.findByUserIdAndDeviceId(userId, request.getDeviceId());
            if (byDevice.isPresent()) {
                UserDeviceToken token = byDevice.get();
                token.setDeviceToken(request.getDeviceToken());
                token.setActive(true);
                token.setDeviceType(request.getDeviceType() != null ? request.getDeviceType() : token.getDeviceType());
                token.setDeviceName(request.getDeviceName() != null ? request.getDeviceName() : token.getDeviceName());
                token.setAppVersion(request.getAppVersion() != null ? request.getAppVersion() : token.getAppVersion());
                token.setLastUsedAt(LocalDateTime.now());
                log.debug("Updated token for user {} device {}", userId, request.getDeviceId());
                return deviceTokenRepository.save(token);
            }
        }

        // Check if token already exists
        Optional<UserDeviceToken> existingToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());

        if (existingToken.isPresent()) {
            UserDeviceToken token = existingToken.get();

            // If same user, just update
            if (token.getUserId().equals(userId)) {
                token.setActive(true);
                token.setDeviceType(request.getDeviceType() != null ? request.getDeviceType() : token.getDeviceType());
                token.setDeviceName(request.getDeviceName() != null ? request.getDeviceName() : token.getDeviceName());
                token.setAppVersion(request.getAppVersion() != null ? request.getAppVersion() : token.getAppVersion());
                if (request.getDeviceId() != null) token.setDeviceId(request.getDeviceId());
                token.setLastUsedAt(LocalDateTime.now());
                log.debug("Updated existing token for user {}", userId);
                return deviceTokenRepository.save(token);
            } else {
                // Token belongs to different user - reassign (user logged into different account)
                token.setUserId(userId);
                token.setActive(true);
                token.setDeviceType(request.getDeviceType() != null ? request.getDeviceType() : token.getDeviceType());
                token.setDeviceName(request.getDeviceName() != null ? request.getDeviceName() : token.getDeviceName());
                token.setAppVersion(request.getAppVersion() != null ? request.getAppVersion() : token.getAppVersion());
                if (request.getDeviceId() != null) token.setDeviceId(request.getDeviceId());
                token.setLastUsedAt(LocalDateTime.now());
                log.debug("Reassigned token from another user to user {}", userId);
                return deviceTokenRepository.save(token);
            }
        }

        // Create new token
        UserDeviceToken newToken = UserDeviceToken.builder()
                .userId(userId)
                .deviceToken(request.getDeviceToken())
                .deviceId(request.getDeviceId())
                .deviceType(request.getDeviceType() != null ? request.getDeviceType() : UserDeviceToken.DeviceType.UNKNOWN)
                .deviceName(request.getDeviceName())
                .appVersion(request.getAppVersion())
                .active(true)
                .lastUsedAt(LocalDateTime.now())
                .build();

        log.debug("Created new device token for user {}", userId);
        return deviceTokenRepository.save(newToken);
    }

    /**
     * Deactivate a device token (e.g., on logout).
     */
    public void deactivateToken(String deviceToken) {
        log.info("Deactivating device token");
        deviceTokenRepository.deactivateToken(deviceToken);
    }

    /**
     * Deactivate all tokens for a user (e.g., logout from all devices).
     */
    public void deactivateAllTokens(Long userId) {
        log.info("Deactivating all device tokens for user {}", userId);
        deviceTokenRepository.deactivateAllForUser(userId);
    }

    /**
     * Get all active tokens for a user.
     */
    @Transactional(readOnly = true)
    public List<UserDeviceToken> getActiveTokens(Long userId) {
        return deviceTokenRepository.findByUserIdAndActiveTrue(userId);
    }

    /**
     * Clean up old inactive tokens (should be run periodically).
     */
    public int cleanupInactiveTokens(int daysOld) {
        LocalDateTime before = LocalDateTime.now().minusDays(daysOld);
        int deleted = deviceTokenRepository.deleteInactiveTokensOlderThan(before);
        log.info("Cleaned up {} inactive device tokens older than {} days", deleted, daysOld);
        return deleted;
    }
}

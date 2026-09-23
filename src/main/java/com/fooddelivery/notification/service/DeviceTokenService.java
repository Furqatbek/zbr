package com.fooddelivery.notification.service;

import com.fooddelivery.notification.dto.DeviceTokenRequest;
import com.fooddelivery.notification.entity.UserDeviceToken;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

        // Prefer upserting on (user, deviceId, appId): the OS rotates push
        // tokens, and keying on the token alone would leave the old row behind
        // so the device receives duplicate pushes. One row per APP per physical
        // device.
        //
        // The appId is not optional in that key, though it looks it. Our apps
        // report the SAME deviceId on one phone — Android's ANDROID_ID is per
        // signing key, iOS's identifierForVendor is per vendor — so keying on
        // (user, deviceId) meant the courier app's registration overwrote the
        // customer app's row. One person running both apps ended up with a
        // single token, and every push for them went to whichever app had
        // registered last: courier alerts on the customer app, order updates on
        // the courier app.
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
            Optional<UserDeviceToken> byDevice = findForApp(userId, request);
            if (byDevice.isPresent()) {
                UserDeviceToken token = byDevice.get();
                token.setDeviceToken(request.getDeviceToken());
                token.setActive(true);
                token.setDeviceType(request.getDeviceType() != null ? request.getDeviceType() : token.getDeviceType());
                token.setDeviceName(request.getDeviceName() != null ? request.getDeviceName() : token.getDeviceName());
                token.setAppId(request.getAppId() != null ? request.getAppId() : token.getAppId());
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
                token.setAppId(request.getAppId() != null ? request.getAppId() : token.getAppId());
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
                token.setAppId(request.getAppId() != null ? request.getAppId() : token.getAppId());
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
                .appId(request.getAppId())
                .appVersion(request.getAppVersion())
                .active(true)
                .lastUsedAt(LocalDateTime.now())
                .build();

        log.debug("Created new device token for user {}", userId);
        return deviceTokenRepository.save(newToken);
    }

    /**
     * The row this app already owns on this device, if any.
     *
     * <p>An app that now sends an {@code appId} adopts its own legacy row — the
     * one registered before the apps sent one — rather than leaving it behind.
     * An abandoned row keeps receiving every push for that user, because
     * targeting deliberately fails open for a token whose app is unknown, so
     * "leave it alone" would mean the wrong app keeps buzzing indefinitely.
     *
     * <p>Whichever app registers first claims it; the second finds it stamped
     * and creates its own.
     */
    private Optional<UserDeviceToken> findForApp(Long userId, DeviceTokenRequest request) {
        String deviceId = request.getDeviceId();
        String appId = request.getAppId();

        if (appId == null || appId.isBlank()) {
            return deviceTokenRepository.findByUserIdAndDeviceIdAndAppIdIsNull(userId, deviceId);
        }

        Optional<UserDeviceToken> exact =
                deviceTokenRepository.findByUserIdAndDeviceIdAndAppId(userId, deviceId, appId);
        if (exact.isPresent()) {
            return exact;
        }
        return deviceTokenRepository.findByUserIdAndDeviceIdAndAppIdIsNull(userId, deviceId);
    }

    /**
     * Deactivate a device token (e.g., on logout).
     */
    public void deactivateToken(Long userId, String deviceToken) {
        int updated = deviceTokenRepository.deactivateToken(userId, deviceToken);
        if (updated == 0) {
            // Either the token was already inactive or it is not this user's.
            // Same response either way — do not confirm existence to a caller
            // probing for someone else's token.
            log.info("No active device token deactivated for user {}", userId);
        } else {
            log.info("Deactivated device token for user {}", userId);
        }
    }

    /**
     * Retire a token the push provider has rejected as dead.
     *
     * <p>REQUIRES_NEW because every caller is a push sender running OUTSIDE any
     * transaction. The repository call is @Modifying, so it threw
     * "Executing an update/delete query" every single time and was swallowed by
     * the senders' catch-all — meaning rejected tokens were never actually
     * retired, and every later push retried the same dead token and failed
     * again. A new transaction also keeps this independent: pruning is cleanup,
     * and it must not roll back or be rolled back by whatever the caller is
     * doing.
     *
     * <p>Unscoped by design — the provider identifies the token, and there is no
     * user in context. For a user-initiated removal use
     * {@link #deactivateToken(Long, String)}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deactivateRejectedToken(String deviceToken, String reason) {
        try {
            int updated = deviceTokenRepository.deactivateRejectedToken(deviceToken);
            if (updated > 0) {
                log.info("Retired device token rejected by the provider: {}", reason);
            }
        } catch (Exception e) {
            // Never let cleanup break the send that discovered the dead token.
            log.warn("Could not retire rejected device token ({}): {}", reason, e.getMessage());
        }
    }

    /**
     * Retire the token for one physical device, identified the way registration
     * identifies it.
     *
     * <p>Scoped to the user on purpose: deviceId is client-supplied, so an
     * unscoped delete would let any caller retire a stranger's device.
     */
    public void deactivateByDeviceId(Long userId, String deviceId) {
        int updated = deviceTokenRepository.deactivateByDeviceId(userId, deviceId);
        if (updated == 0) {
            // Already inactive, or not this user's. Same answer either way —
            // do not confirm existence to a caller probing device ids.
            log.info("No active device token deactivated for user {}", userId);
        } else {
            log.info("Deactivated device token for user {} device {}", userId, deviceId);
        }
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

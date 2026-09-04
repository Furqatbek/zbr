package com.fooddelivery.notification.service;

import com.fooddelivery.notification.entity.UserDeviceToken;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Decides which of a user's devices a notification belongs on.
 *
 * <p>One person is one {@code users} row across all three apps — a courier IS a
 * consumer with a courier profile attached. Push targeting is by user id, so
 * without this a "new order available" alert aimed at a courier also lands on
 * that same person's customer app, and an order-status update for a customer
 * buzzes their courier app mid-delivery.
 *
 * <p><b>This fails open, deliberately.</b> A device is excluded only when its
 * {@code appId} is known AND belongs to a different app. Two cases both keep
 * delivery working:
 *
 * <ul>
 *   <li>A token with no {@code appId} always receives. The column arrived in
 *       V37 and clients only populate it if they were told to, so most existing
 *       rows are null. Filtering those out would silently stop push for every
 *       already-registered device — a far worse failure than the duplicate
 *       delivery this fixes.</li>
 *   <li>A role with no configured app id disables filtering for that role.
 *       Nothing is configured by default, so this change is inert until an
 *       operator supplies the real bundle ids. Guessing them here would break
 *       push the moment a guess was wrong.</li>
 * </ul>
 *
 * <p>Configure in {@code app.push.app-ids}, keyed by {@code NotificationRole}
 * (case-insensitive):
 *
 * <pre>
 * app:
 *   push:
 *     app-ids:
 *       consumer: app.zbr.customer
 *       courier:  app.zbr.courier
 *       restaurant: app.zbr.owner
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.push")
@Getter
@Setter
@Slf4j
public class PushAppIdResolver {

    /** Role name (lower-case) to the app id that serves it. Empty = no filtering. */
    private Map<String, String> appIds = new HashMap<>();

    /**
     * The app id notifications for this role belong to, or null when the role
     * is unconfigured, unknown, or has no app of its own (ADMIN, FINANCE, and
     * ALL — a system-wide broadcast belongs on every app).
     */
    public String appIdFor(String role) {
        if (role == null || appIds.isEmpty()) {
            return null;
        }
        String key = role.toLowerCase(Locale.ROOT);
        if ("all".equals(key)) {
            return null;
        }
        // CUSTOMER is the deprecated spelling of CONSUMER and both are still
        // emitted; treat them as the same audience rather than making an
        // operator configure the same bundle id twice.
        String appId = appIds.get(key);
        if (appId == null && ("customer".equals(key) || "consumer".equals(key))) {
            appId = appIds.get("customer".equals(key) ? "consumer" : "customer");
        }
        return (appId == null || appId.isBlank()) ? null : appId;
    }

    /**
     * Narrow a user's devices to those the given role's app runs on.
     *
     * @return the devices to send to — never fewer than the caller can afford
     *         to lose; see the class note on failing open
     */
    public List<UserDeviceToken> filter(List<UserDeviceToken> devices, String role) {
        String target = appIdFor(role);
        if (target == null) {
            return devices;
        }

        List<UserDeviceToken> kept = devices.stream()
                .filter(d -> d.getAppId() == null || d.getAppId().isBlank()
                        || target.equals(d.getAppId()))
                .toList();

        if (kept.size() < devices.size()) {
            log.debug("Push for role {} narrowed from {} to {} device(s) targeting {}",
                    role, devices.size(), kept.size(), target);
        }
        return kept;
    }
}

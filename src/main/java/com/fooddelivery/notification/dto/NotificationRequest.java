package com.fooddelivery.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for notification requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String email;
    private String phone;
    private String deviceToken;
    private String subject;
    private String body;
    private String channel; // email | push  (SMS is auth-codes only, not a notification channel)
    private String templateId;
    private Map<String, Object> templateData;
    private Integer priority; // 1-10
    private String referenceId;
    private String referenceType;

    /**
     * The order this push is about, surfaced to clients as data.orderId so the
     * app can fetch and deep-link. referenceId/referenceType cannot serve this:
     * they carry the NOTIFICATION id, so the order id never reached a device.
     */
    private Long orderId;

    /** NotificationCategory name — clients switch screens on ORDER*. */
    private String category;

    /**
     * NotificationRole this was aimed at, used to pick which of the recipient's
     * apps to push to. One person is one user row across all three apps, so
     * without this a courier alert also lands on their customer app. Null means
     * "no preference" and every device receives, which is what messages already
     * in the queue at deploy time will carry.
     */
    private String targetRole;

    /**
     * Where tapping the push should land, e.g. {@code /orders/4417}.
     *
     * <p>Stored on the notification row since it was introduced, and never sent
     * to a device — so the apps parsed an {@code actionUrl} the push had never
     * contained and fell back to {@code orderId} every time.
     */
    private String actionUrl;
}

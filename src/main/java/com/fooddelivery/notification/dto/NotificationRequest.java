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
     * NotificationRole this was aimed at, used to pick which of the recipient's
     * apps to push to. One person is one user row across all three apps, so
     * without this a courier alert also lands on their customer app. Null means
     * "no preference" and every device receives, which is what messages already
     * in the queue at deploy time will carry.
     */
    private String targetRole;
}

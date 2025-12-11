package com.fooddelivery.notification.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/**
 * DTO for bulk notification actions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationBulkActionDto {

    /**
     * List of notification IDs to act upon.
     */
    @NotEmpty(message = "At least one notification ID is required")
    private List<Long> notificationIds;

    /**
     * Action to perform.
     */
    private BulkAction action;

    public enum BulkAction {
        MARK_READ,
        MARK_UNREAD,
        DISMISS,
        DELETE
    }
}

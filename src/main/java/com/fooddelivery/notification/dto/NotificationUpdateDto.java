package com.fooddelivery.notification.dto;

import lombok.*;

/**
 * DTO for updating notification status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationUpdateDto {

    /**
     * Mark notification as read.
     */
    private Boolean markAsRead;

    /**
     * Mark notification as dismissed.
     */
    private Boolean dismiss;
}

package com.fooddelivery.notification.mapper;

import com.fooddelivery.notification.dto.NotificationCreateDto;
import com.fooddelivery.notification.dto.NotificationResponseDto;
import com.fooddelivery.notification.model.Notification;
import com.fooddelivery.notification.model.NotificationPriority;
import com.fooddelivery.notification.util.NotificationTimeUtils;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MapStruct mapper for Notification entity and DTOs.
 */
@Mapper(componentModel = "spring", imports = {LocalDateTime.class, NotificationTimeUtils.class})
public interface NotificationMapper {

    /**
     * Map create DTO to entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "dismissed", constant = "false")
    @Mapping(target = "dismissedAt", ignore = true)
    @Mapping(target = "expiresAt", expression = "java(calculateExpiresAt(dto.getTtlHours()))")
    @Mapping(target = "priority", expression = "java(dto.getPriority() != null ? dto.getPriority() : dto.getNotificationType().getDefaultPriority())")
    Notification toEntity(NotificationCreateDto dto);

    /**
     * Map entity to response DTO.
     */
    @Mapping(target = "roleDisplayName", expression = "java(notification.getRole().getDisplayName())")
    @Mapping(target = "categoryDisplayName", expression = "java(notification.getCategory().getDisplayName())")
    @Mapping(target = "notificationTypeDisplayName", expression = "java(notification.getNotificationType().getDisplayName())")
    @Mapping(target = "priorityDisplayName", expression = "java(notification.getPriority().getDisplayName())")
    @Mapping(target = "isRead", expression = "java(notification.isRead())")
    @Mapping(target = "isExpired", expression = "java(notification.isExpired())")
    @Mapping(target = "timeAgo", expression = "java(NotificationTimeUtils.getTimeAgo(notification.getCreatedAt()))")
    NotificationResponseDto toResponseDto(Notification notification);

    /**
     * Map list of entities to response DTOs.
     */
    List<NotificationResponseDto> toResponseDtoList(List<Notification> notifications);

    /**
     * Update entity from create DTO.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "dismissed", ignore = true)
    @Mapping(target = "dismissedAt", ignore = true)
    @Mapping(target = "expiresAt", expression = "java(calculateExpiresAt(dto.getTtlHours()))")
    void updateEntityFromDto(NotificationCreateDto dto, @MappingTarget Notification entity);

    /**
     * Calculate expiration time from TTL hours.
     */
    default LocalDateTime calculateExpiresAt(Integer ttlHours) {
        if (ttlHours == null || ttlHours <= 0) {
            return null;
        }
        return LocalDateTime.now().plusHours(ttlHours);
    }

    /**
     * Get default priority if not specified.
     */
    default NotificationPriority getDefaultPriority(NotificationCreateDto dto) {
        if (dto.getPriority() != null) {
            return dto.getPriority();
        }
        return dto.getNotificationType() != null
                ? dto.getNotificationType().getDefaultPriority()
                : NotificationPriority.NORMAL;
    }
}

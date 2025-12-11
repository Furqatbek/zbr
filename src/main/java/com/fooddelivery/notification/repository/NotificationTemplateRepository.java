package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.model.NotificationTemplate;
import com.fooddelivery.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for notification templates.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * Find template by notification type and role.
     */
    Optional<NotificationTemplate> findByNotificationTypeAndRoleAndActiveTrue(
            NotificationType notificationType, NotificationRole role);

    /**
     * Find all templates for a notification type.
     */
    List<NotificationTemplate> findByNotificationTypeAndActiveTrue(NotificationType notificationType);

    /**
     * Find all templates for a role.
     */
    List<NotificationTemplate> findByRoleAndActiveTrue(NotificationRole role);

    /**
     * Find all active templates.
     */
    List<NotificationTemplate> findByActiveTrue();

    /**
     * Check if template exists.
     */
    boolean existsByNotificationTypeAndRole(NotificationType notificationType, NotificationRole role);

    /**
     * Find templates with TTL configured.
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.active = true AND t.defaultTtlHours IS NOT NULL")
    List<NotificationTemplate> findTemplatesWithTtl();
}

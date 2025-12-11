package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.dto.NotificationFilterDto;
import com.fooddelivery.notification.model.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic notification queries.
 */
public class NotificationSpecifications {

    private NotificationSpecifications() {
        // Utility class
    }

    /**
     * Build specification from filter DTO.
     */
    public static Specification<Notification> fromFilter(NotificationFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User ID filter
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
            }

            // Role filter
            if (filter.getRole() != null) {
                predicates.add(cb.equal(root.get("role"), filter.getRole()));
            }

            // Multiple roles filter
            if (filter.getRoles() != null && !filter.getRoles().isEmpty()) {
                predicates.add(root.get("role").in(filter.getRoles()));
            }

            // Read status filter
            if (filter.getIsRead() != null) {
                if (filter.getIsRead()) {
                    predicates.add(cb.isNotNull(root.get("readAt")));
                } else {
                    predicates.add(cb.isNull(root.get("readAt")));
                }
            }

            // Category filter
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }

            // Multiple categories filter
            if (filter.getCategories() != null && !filter.getCategories().isEmpty()) {
                predicates.add(root.get("category").in(filter.getCategories()));
            }

            // Notification type filter
            if (filter.getNotificationType() != null) {
                predicates.add(cb.equal(root.get("notificationType"), filter.getNotificationType()));
            }

            // Priority filter
            if (filter.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
            }

            // Minimum priority filter
            if (filter.getMinPriority() != null) {
                // Priority is ordered LOW < NORMAL < HIGH < URGENT
                List<NotificationPriority> validPriorities = getMinPriorityList(filter.getMinPriority());
                predicates.add(root.get("priority").in(validPriorities));
            }

            // Order ID filter
            if (filter.getOrderId() != null) {
                predicates.add(cb.equal(root.get("orderId"), filter.getOrderId()));
            }

            // Related entity filter
            if (filter.getRelatedEntityId() != null) {
                predicates.add(cb.equal(root.get("relatedEntityId"), filter.getRelatedEntityId()));
            }

            if (filter.getRelatedEntityType() != null) {
                predicates.add(cb.equal(root.get("relatedEntityType"), filter.getRelatedEntityType()));
            }

            // Date range filter
            if (filter.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
            }

            if (filter.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));
            }

            // Dismissed filter
            if (!Boolean.TRUE.equals(filter.getIncludeDismissed())) {
                predicates.add(cb.equal(root.get("dismissed"), false));
            }

            // Expired filter
            if (!Boolean.TRUE.equals(filter.getIncludeExpired())) {
                predicates.add(cb.or(
                        cb.isNull(root.get("expiresAt")),
                        cb.greaterThan(root.get("expiresAt"), LocalDateTime.now())
                ));
            }

            // Search term filter
            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isBlank()) {
                String searchPattern = "%" + filter.getSearchTerm().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), searchPattern),
                        cb.like(cb.lower(root.get("message")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification for user's notifications including broadcasts.
     */
    public static Specification<Notification> forUserWithBroadcasts(Long userId, NotificationRole role) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("userId"), userId),
                cb.and(
                        cb.isNull(root.get("userId")),
                        cb.equal(root.get("role"), role)
                )
        );
    }

    /**
     * Specification for unread notifications.
     */
    public static Specification<Notification> isUnread() {
        return (root, query, cb) -> cb.isNull(root.get("readAt"));
    }

    /**
     * Specification for read notifications.
     */
    public static Specification<Notification> isRead() {
        return (root, query, cb) -> cb.isNotNull(root.get("readAt"));
    }

    /**
     * Specification for not dismissed notifications.
     */
    public static Specification<Notification> notDismissed() {
        return (root, query, cb) -> cb.equal(root.get("dismissed"), false);
    }

    /**
     * Specification for not expired notifications.
     */
    public static Specification<Notification> notExpired() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("expiresAt")),
                cb.greaterThan(root.get("expiresAt"), LocalDateTime.now())
        );
    }

    /**
     * Specification for high priority notifications.
     */
    public static Specification<Notification> highPriority() {
        return (root, query, cb) -> root.get("priority").in(
                List.of(NotificationPriority.HIGH, NotificationPriority.URGENT)
        );
    }

    /**
     * Get list of priorities >= minimum priority.
     */
    private static List<NotificationPriority> getMinPriorityList(NotificationPriority minPriority) {
        List<NotificationPriority> result = new ArrayList<>();
        for (NotificationPriority p : NotificationPriority.values()) {
            if (p.getWeight() >= minPriority.getWeight()) {
                result.add(p);
            }
        }
        return result;
    }
}

package com.fooddelivery.notification.controller;

import com.fooddelivery.notification.model.*;
import com.fooddelivery.notification.repository.NotificationTemplateRepository;
import com.fooddelivery.notification.util.NotificationConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for notification reference data and templates.
 * Provides endpoints for retrieving enum values and managing notification templates.
 */
@Slf4j
@RestController
@RequestMapping(NotificationConstants.API_BASE_PATH)
@RequiredArgsConstructor
@Tag(name = "Notification Reference Data", description = "Reference data and template management endpoints")
public class NotificationReferenceController {

    private final NotificationTemplateRepository templateRepository;

    // ===== Reference Data Endpoints =====

    /**
     * Get all notification types.
     */
    @GetMapping("/types")
    @Operation(summary = "Get notification types",
            description = "Get all available notification types with their display names and default settings")
    public ResponseEntity<List<Map<String, Object>>> getNotificationTypes() {
        log.debug("Getting all notification types");

        List<Map<String, Object>> types = Arrays.stream(NotificationType.values())
                .map(type -> Map.<String, Object>of(
                        "value", type.name(),
                        "displayName", type.getDisplayName(),
                        "defaultCategory", type.getDefaultCategory().name(),
                        "defaultPriority", type.getDefaultPriority().name()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(types);
    }

    /**
     * Get all notification roles.
     */
    @GetMapping("/roles")
    @Operation(summary = "Get notification roles",
            description = "Get all available notification roles with their display names")
    public ResponseEntity<List<Map<String, String>>> getNotificationRoles() {
        log.debug("Getting all notification roles");

        List<Map<String, String>> roles = Arrays.stream(NotificationRole.values())
                .map(role -> Map.of(
                        "value", role.name(),
                        "displayName", role.getDisplayName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(roles);
    }

    /**
     * Get all notification categories.
     */
    @GetMapping("/categories")
    @Operation(summary = "Get notification categories",
            description = "Get all available notification categories with their display names and codes")
    public ResponseEntity<List<Map<String, String>>> getNotificationCategories() {
        log.debug("Getting all notification categories");

        List<Map<String, String>> categories = Arrays.stream(NotificationCategory.values())
                .map(category -> Map.of(
                        "value", category.name(),
                        "displayName", category.getDisplayName(),
                        "code", category.getCode()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(categories);
    }

    /**
     * Get all notification priorities.
     */
    @GetMapping("/priorities")
    @Operation(summary = "Get notification priorities",
            description = "Get all available notification priorities with their display names and weights")
    public ResponseEntity<List<Map<String, Object>>> getNotificationPriorities() {
        log.debug("Getting all notification priorities");

        List<Map<String, Object>> priorities = Arrays.stream(NotificationPriority.values())
                .map(priority -> Map.<String, Object>of(
                        "value", priority.name(),
                        "displayName", priority.getDisplayName(),
                        "weight", priority.getWeight(),
                        "requiresImmediateDisplay", priority.requiresImmediateDisplay()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(priorities);
    }

    /**
     * Get all reference data in one call.
     */
    @GetMapping("/reference-data")
    @Operation(summary = "Get all reference data",
            description = "Get all notification reference data (types, roles, categories, priorities) in a single call")
    public ResponseEntity<Map<String, Object>> getAllReferenceData() {
        log.debug("Getting all notification reference data");

        Map<String, Object> referenceData = Map.of(
                "types", Arrays.stream(NotificationType.values())
                        .map(type -> Map.<String, Object>of(
                                "value", type.name(),
                                "displayName", type.getDisplayName(),
                                "defaultCategory", type.getDefaultCategory().name(),
                                "defaultPriority", type.getDefaultPriority().name()
                        ))
                        .collect(Collectors.toList()),
                "roles", Arrays.stream(NotificationRole.values())
                        .map(role -> Map.of(
                                "value", role.name(),
                                "displayName", role.getDisplayName()
                        ))
                        .collect(Collectors.toList()),
                "categories", Arrays.stream(NotificationCategory.values())
                        .map(category -> Map.of(
                                "value", category.name(),
                                "displayName", category.getDisplayName(),
                                "code", category.getCode()
                        ))
                        .collect(Collectors.toList()),
                "priorities", Arrays.stream(NotificationPriority.values())
                        .map(priority -> Map.<String, Object>of(
                                "value", priority.name(),
                                "displayName", priority.getDisplayName(),
                                "weight", priority.getWeight(),
                                "requiresImmediateDisplay", priority.requiresImmediateDisplay()
                        ))
                        .collect(Collectors.toList())
        );

        return ResponseEntity.ok(referenceData);
    }

    // ===== Template CRUD Endpoints =====

    /**
     * Get all notification templates.
     */
    @GetMapping("/templates")
    @Operation(summary = "Get all templates",
            description = "Get all notification templates")
    public ResponseEntity<List<NotificationTemplate>> getAllTemplates(
            @RequestParam(required = false) Boolean activeOnly) {
        log.debug("Getting all notification templates, activeOnly={}", activeOnly);

        List<NotificationTemplate> templates = (activeOnly != null && activeOnly)
                ? templateRepository.findByActiveTrue()
                : templateRepository.findAll();

        return ResponseEntity.ok(templates);
    }

    /**
     * Get template by ID.
     */
    @GetMapping("/templates/{id}")
    @Operation(summary = "Get template by ID",
            description = "Get a specific notification template by its ID")
    public ResponseEntity<NotificationTemplate> getTemplateById(@PathVariable Long id) {
        log.debug("Getting notification template by ID: {}", id);

        return templateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get templates by notification type.
     */
    @GetMapping("/templates/by-type/{type}")
    @Operation(summary = "Get templates by type",
            description = "Get all templates for a specific notification type")
    public ResponseEntity<List<NotificationTemplate>> getTemplatesByType(
            @PathVariable NotificationType type) {
        log.debug("Getting templates for notification type: {}", type);

        List<NotificationTemplate> templates = templateRepository.findByNotificationTypeAndActiveTrue(type);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get templates by role.
     */
    @GetMapping("/templates/by-role/{role}")
    @Operation(summary = "Get templates by role",
            description = "Get all templates for a specific role")
    public ResponseEntity<List<NotificationTemplate>> getTemplatesByRole(
            @PathVariable NotificationRole role) {
        log.debug("Getting templates for role: {}", role);

        List<NotificationTemplate> templates = templateRepository.findByRoleAndActiveTrue(role);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get template by type and role.
     */
    @GetMapping("/templates/by-type-and-role")
    @Operation(summary = "Get template by type and role",
            description = "Get a specific template for a notification type and role combination")
    public ResponseEntity<NotificationTemplate> getTemplateByTypeAndRole(
            @RequestParam NotificationType type,
            @RequestParam NotificationRole role) {
        log.debug("Getting template for type: {} and role: {}", type, role);

        return templateRepository.findByNotificationTypeAndRoleAndActiveTrue(type, role)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new notification template.
     */
    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create template",
            description = "Create a new notification template. Admin only.")
    public ResponseEntity<NotificationTemplate> createTemplate(
            @Valid @RequestBody NotificationTemplate template) {
        log.info("Creating notification template for type: {} and role: {}",
                template.getNotificationType(), template.getRole());

        if (templateRepository.existsByNotificationTypeAndRole(
                template.getNotificationType(), template.getRole())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        NotificationTemplate saved = templateRepository.save(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Update an existing notification template.
     */
    @PutMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update template",
            description = "Update an existing notification template. Admin only.")
    public ResponseEntity<NotificationTemplate> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplate template) {
        log.info("Updating notification template ID: {}", id);

        return templateRepository.findById(id)
                .map(existing -> {
                    existing.setTitleTemplate(template.getTitleTemplate());
                    existing.setMessageTemplate(template.getMessageTemplate());
                    existing.setIcon(template.getIcon());
                    existing.setActionUrlTemplate(template.getActionUrlTemplate());
                    existing.setActive(template.getActive());
                    existing.setDefaultTtlHours(template.getDefaultTtlHours());
                    return ResponseEntity.ok(templateRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a notification template.
     */
    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete template",
            description = "Delete a notification template. Admin only.")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        log.info("Deleting notification template ID: {}", id);

        if (!templateRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle template active status.
     */
    @PatchMapping("/templates/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle template active status",
            description = "Toggle the active status of a notification template. Admin only.")
    public ResponseEntity<NotificationTemplate> toggleTemplateActive(@PathVariable Long id) {
        log.info("Toggling active status for notification template ID: {}", id);

        return templateRepository.findById(id)
                .map(template -> {
                    template.setActive(!template.getActive());
                    return ResponseEntity.ok(templateRepository.save(template));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

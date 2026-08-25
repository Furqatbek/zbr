package com.fooddelivery.sms.controller;

import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import com.fooddelivery.sms.dto.SmsTemplateRequest;
import com.fooddelivery.sms.dto.SmsTemplateResponse;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateType;
import com.fooddelivery.sms.service.SmsTemplateService;
import com.fooddelivery.sms.service.SmsTemplateService.TemplateStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for SMS template management.
 * Allows creating, updating, and syncing templates with SMS providers.
 */
@RestController
@RequestMapping("/api/v1/sms/templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SMS Templates", description = "Manage SMS templates for Eskiz and DevSMS providers")
public class SmsTemplateController {

    private final SmsTemplateService templateService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new SMS template",
            description = "Creates a new SMS template and optionally syncs it with the provider")
    public ResponseEntity<SmsTemplateResponse> createTemplate(
            @Valid @RequestBody SmsTemplateRequest request) {
        log.info("Creating SMS template: code={}, provider={}", request.getTemplateCode(), request.getProvider());
        SmsTemplateResponse response = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing SMS template",
            description = "Updates template content and optionally re-syncs with provider")
    public ResponseEntity<SmsTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody SmsTemplateRequest request) {
        log.info("Updating SMS template: id={}", id);
        SmsTemplateResponse response = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER')")
    @Operation(summary = "Get SMS template by ID")
    public ResponseEntity<SmsTemplateResponse> getTemplate(@PathVariable Long id) {
        SmsTemplateResponse response = templateService.getTemplate(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-code")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER')")
    @Operation(summary = "Get SMS template by code and provider")
    public ResponseEntity<SmsTemplateResponse> getTemplateByCode(
            @RequestParam String code,
            @RequestParam SmsProviderType provider) {
        SmsTemplateResponse response = templateService.getTemplateByCode(code, provider);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER')")
    @Operation(summary = "List all SMS templates")
    public ResponseEntity<List<SmsTemplateResponse>> listAllTemplates() {
        List<SmsTemplateResponse> templates = templateService.listAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/by-provider/{provider}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER')")
    @Operation(summary = "List SMS templates by provider")
    public ResponseEntity<List<SmsTemplateResponse>> listByProvider(
            @PathVariable SmsProviderType provider) {
        List<SmsTemplateResponse> templates = templateService.listTemplatesByProvider(provider);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/by-type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER')")
    @Operation(summary = "List SMS templates by type")
    public ResponseEntity<List<SmsTemplateResponse>> listByType(
            @PathVariable SmsTemplateType type) {
        List<SmsTemplateResponse> templates = templateService.listTemplatesByType(type);
        return ResponseEntity.ok(templates);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete (deactivate) an SMS template",
            description = "Soft deletes the template and removes it from the provider")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        log.info("Deleting SMS template: id={}", id);
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync template with SMS provider",
            description = "Registers or updates the template on the SMS provider")
    public ResponseEntity<SmsTemplateResponse> syncTemplate(@PathVariable Long id) {
        log.info("Syncing SMS template with provider: id={}", id);
        SmsTemplateResponse response = templateService.syncTemplate(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import/{provider}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Import templates that already exist on the provider",
            description = "Pulls the templates registered with the provider into this system, "
                    + "matching on the provider's template id. /sync and /sync-all only push "
                    + "local drafts OUT, so templates registered directly with Eskiz were "
                    + "previously invisible here. Imported templates come in as type GENERAL "
                    + "and must be retyped to OTP before they are used for sending.")
    public ResponseEntity<List<SmsTemplateResponse>> importFromProvider(
            @PathVariable SmsProviderType provider) {
        log.info("Importing SMS templates from provider: {}", provider);
        return ResponseEntity.ok(templateService.importFromProvider(provider));
    }

    @PostMapping("/{id}/mark-approved")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record a provider approval granted outside this system",
            description = "Eskiz templates are usually approved in their web cabinet rather than "
                    + "over the API. Use this to record that, optionally supplying the provider's "
                    + "template id. Only APPROVED + active templates are used for sending, so "
                    + "without this a cabinet-approved template would never take effect and OTPs "
                    + "would keep using the built-in fallback text.")
    public ResponseEntity<SmsTemplateResponse> markApproved(
            @PathVariable Long id,
            @RequestBody(required = false) MarkApprovedRequest request) {
        String providerTemplateId = request != null ? request.getProviderTemplateId() : null;
        log.info("Marking SMS template approved manually: id={}", id);
        return ResponseEntity.ok(templateService.markApproved(id, providerTemplateId));
    }

    /** Body for {@link #markApproved}; the provider template id is optional. */
    @lombok.Data
    public static class MarkApprovedRequest {
        private String providerTemplateId;
    }

    @PostMapping("/sync-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync all pending templates",
            description = "Syncs all templates in DRAFT status with their providers")
    public ResponseEntity<List<SmsTemplateResponse>> syncAllPending() {
        log.info("Syncing all pending SMS templates");
        List<SmsTemplateResponse> templates = templateService.syncAllPendingTemplates();
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/{id}/refresh-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER')")
    @Operation(summary = "Refresh template status from provider",
            description = "Fetches the current approval status from the SMS provider")
    public ResponseEntity<SmsTemplateResponse> refreshStatus(@PathVariable Long id) {
        log.info("Refreshing SMS template status: id={}", id);
        SmsTemplateResponse response = templateService.refreshTemplateStatus(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER')")
    @Operation(summary = "Get template statistics",
            description = "Returns counts of templates by status")
    public ResponseEntity<TemplateStats> getStats() {
        TemplateStats stats = templateService.getTemplateStats();
        return ResponseEntity.ok(stats);
    }
}

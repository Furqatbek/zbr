package com.fooddelivery.sms.service;

import com.fooddelivery.sms.config.SmsProperties;
import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import com.fooddelivery.sms.dto.SmsTemplateRequest;
import com.fooddelivery.sms.dto.SmsTemplateResponse;
import com.fooddelivery.sms.dto.SmsTemplateSyncResponse;
import com.fooddelivery.sms.entity.SmsTemplate;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateStatus;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateType;
import com.fooddelivery.sms.repository.SmsTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing SMS templates across multiple providers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsTemplateService {

    private final SmsTemplateRepository templateRepository;
    private final SmsProviderFactory providerFactory;
    private final SmsProperties smsProperties;

    /**
     * Create a new SMS template.
     */
    @Transactional
    public SmsTemplateResponse createTemplate(SmsTemplateRequest request) {
        // Check for duplicate
        if (templateRepository.existsByTemplateCodeAndProvider(request.getTemplateCode(), request.getProvider())) {
            throw new IllegalArgumentException("Template with code '" + request.getTemplateCode() +
                    "' already exists for provider " + request.getProvider());
        }

        SmsTemplate template = SmsTemplate.builder()
                .templateCode(request.getTemplateCode())
                .name(request.getName())
                .content(request.getContent())
                .templateType(request.getTemplateType())
                .provider(request.getProvider())
                .variables(request.getVariables() != null ? String.join(",", request.getVariables()) : null)
                .language(request.getLanguage())
                .description(request.getDescription())
                .status(SmsTemplateStatus.DRAFT)
                .active(true)
                .build();

        template = templateRepository.save(template);
        log.info("Created SMS template: id={}, code={}, provider={}",
                template.getId(), template.getTemplateCode(), template.getProvider());

        // Sync with provider if requested
        if (request.isSyncImmediately()) {
            syncTemplateWithProvider(template);
            template = templateRepository.save(template);
        }

        return SmsTemplateResponse.fromEntity(template);
    }

    /**
     * Update an existing SMS template.
     */
    @Transactional
    public SmsTemplateResponse updateTemplate(Long id, SmsTemplateRequest request) {
        SmsTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + id));

        template.setName(request.getName());
        template.setContent(request.getContent());
        template.setTemplateType(request.getTemplateType());
        template.setVariables(request.getVariables() != null ? String.join(",", request.getVariables()) : null);
        template.setLanguage(request.getLanguage());
        template.setDescription(request.getDescription());

        // If content changed, mark as needing re-sync
        template.setStatus(SmsTemplateStatus.DRAFT);

        template = templateRepository.save(template);
        log.info("Updated SMS template: id={}, code={}", template.getId(), template.getTemplateCode());

        // Sync with provider if requested
        if (request.isSyncImmediately() && template.getProviderTemplateId() != null) {
            syncTemplateWithProvider(template);
            template = templateRepository.save(template);
        }

        return SmsTemplateResponse.fromEntity(template);
    }

    /**
     * Get template by ID.
     */
    @Transactional(readOnly = true)
    public SmsTemplateResponse getTemplate(Long id) {
        SmsTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + id));
        return SmsTemplateResponse.fromEntity(template);
    }

    /**
     * Get template by code and provider.
     */
    @Transactional(readOnly = true)
    public SmsTemplateResponse getTemplateByCode(String templateCode, SmsProviderType provider) {
        SmsTemplate template = templateRepository.findByTemplateCodeAndProvider(templateCode, provider)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Template not found: " + templateCode + " for provider " + provider));
        return SmsTemplateResponse.fromEntity(template);
    }

    /**
     * List all templates.
     */
    @Transactional(readOnly = true)
    public List<SmsTemplateResponse> listAllTemplates() {
        return templateRepository.findByActiveTrue().stream()
                .map(SmsTemplateResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * List templates by provider.
     */
    @Transactional(readOnly = true)
    public List<SmsTemplateResponse> listTemplatesByProvider(SmsProviderType provider) {
        return templateRepository.findByProviderAndActiveTrue(provider).stream()
                .map(SmsTemplateResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * List templates by type.
     */
    @Transactional(readOnly = true)
    public List<SmsTemplateResponse> listTemplatesByType(SmsTemplateType type) {
        return templateRepository.findByTemplateTypeAndActiveTrue(type).stream()
                .map(SmsTemplateResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Delete (deactivate) a template.
     */
    @Transactional
    public void deleteTemplate(Long id) {
        SmsTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + id));

        // Try to delete from provider first
        if (template.getProviderTemplateId() != null) {
            SmsProvider provider = providerFactory.getProvider(template.getProvider());
            if (provider != null && provider.isAvailable()) {
                SmsTemplateSyncResponse response = provider.deleteTemplate(template);
                if (!response.isSuccess()) {
                    log.warn("Failed to delete template from provider: {}", response.getMessage());
                }
            }
        }

        // Soft delete
        template.setActive(false);
        templateRepository.save(template);
        log.info("Deleted SMS template: id={}, code={}", id, template.getTemplateCode());
    }

    /**
     * Sync template with SMS provider.
     */
    @Transactional
    public SmsTemplateResponse syncTemplate(Long id) {
        SmsTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + id));

        syncTemplateWithProvider(template);
        template = templateRepository.save(template);

        return SmsTemplateResponse.fromEntity(template);
    }

    /**
     * Sync all draft templates with their providers.
     */
    @Transactional
    public List<SmsTemplateResponse> syncAllPendingTemplates() {
        List<SmsTemplate> pendingTemplates = templateRepository.findTemplatesPendingSync();
        log.info("Syncing {} pending templates", pendingTemplates.size());

        for (SmsTemplate template : pendingTemplates) {
            try {
                syncTemplateWithProvider(template);
            } catch (Exception e) {
                log.error("Failed to sync template {}: {}", template.getId(), e.getMessage());
            }
        }

        templateRepository.saveAll(pendingTemplates);

        return pendingTemplates.stream()
                .map(SmsTemplateResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Refresh template status from provider.
     */
    @Transactional
    public SmsTemplateResponse refreshTemplateStatus(Long id) {
        SmsTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + id));

        if (template.getProviderTemplateId() == null) {
            throw new IllegalStateException("Template has not been synced with provider yet");
        }

        SmsProvider provider = providerFactory.getProvider(template.getProvider());
        if (provider == null || !provider.isAvailable()) {
            throw new IllegalStateException("Provider " + template.getProvider() + " is not available");
        }

        SmsTemplateSyncResponse response = provider.getTemplateStatus(template.getProviderTemplateId());
        if (response.isSuccess()) {
            template.setStatus(response.getStatus());
            template.setLastSyncedAt(LocalDateTime.now());
            template = templateRepository.save(template);
            log.info("Refreshed template status: id={}, status={}", id, response.getStatus());
        } else {
            log.warn("Failed to refresh template status: {}", response.getMessage());
        }

        return SmsTemplateResponse.fromEntity(template);
    }

    /**
     * Get approved template for sending.
     */
    @Transactional(readOnly = true)
    public SmsTemplate getApprovedTemplateForSending(SmsTemplateType type) {
        SmsProviderType provider = smsProperties.getProvider();
        return templateRepository.findApprovedTemplate(type, provider)
                .orElse(null);
    }

    /**
     * Get template statistics.
     */
    @Transactional(readOnly = true)
    public TemplateStats getTemplateStats() {
        return TemplateStats.builder()
                .totalActive(templateRepository.findByActiveTrue().size())
                .approved(templateRepository.countByStatusAndActiveTrue(SmsTemplateStatus.APPROVED))
                .pending(templateRepository.countByStatusAndActiveTrue(SmsTemplateStatus.PENDING))
                .draft(templateRepository.countByStatusAndActiveTrue(SmsTemplateStatus.DRAFT))
                .rejected(templateRepository.countByStatusAndActiveTrue(SmsTemplateStatus.REJECTED))
                .build();
    }

    /**
     * Internal method to sync template with provider.
     */
    private void syncTemplateWithProvider(SmsTemplate template) {
        SmsProvider provider = providerFactory.getProvider(template.getProvider());
        if (provider == null || !provider.isAvailable()) {
            log.warn("Provider {} is not available for syncing template", template.getProvider());
            return;
        }

        SmsTemplateSyncResponse response;
        if (template.getProviderTemplateId() == null) {
            // Register new template
            response = provider.registerTemplate(template);
        } else {
            // Update existing template
            response = provider.updateTemplate(template);
        }

        if (response.isSuccess()) {
            template.setProviderTemplateId(response.getProviderTemplateId());
            template.setStatus(response.getStatus());
            template.setRejectionReason(null);
        } else {
            template.setRejectionReason(response.getMessage());
        }
        template.setLastSyncedAt(LocalDateTime.now());

        log.info("Template sync result: id={}, success={}, status={}",
                template.getId(), response.isSuccess(), response.getStatus());
    }

    /**
     * Template statistics.
     */
    @lombok.Data
    @lombok.Builder
    public static class TemplateStats {
        private long totalActive;
        private long approved;
        private long pending;
        private long draft;
        private long rejected;
    }
}

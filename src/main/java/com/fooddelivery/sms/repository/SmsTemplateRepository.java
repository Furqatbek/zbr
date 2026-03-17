package com.fooddelivery.sms.repository;

import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import com.fooddelivery.sms.entity.SmsTemplate;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateStatus;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SMS templates.
 */
@Repository
public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, Long> {

    /**
     * Find template by code and provider.
     */
    Optional<SmsTemplate> findByTemplateCodeAndProvider(String templateCode, SmsProviderType provider);

    /**
     * Find all templates for a provider.
     */
    List<SmsTemplate> findByProviderAndActiveTrue(SmsProviderType provider);

    /**
     * Find all templates by type.
     */
    List<SmsTemplate> findByTemplateTypeAndActiveTrue(SmsTemplateType templateType);

    /**
     * Find approved templates by type and provider.
     */
    List<SmsTemplate> findByTemplateTypeAndProviderAndStatusAndActiveTrue(
            SmsTemplateType templateType, SmsProviderType provider, SmsTemplateStatus status);

    /**
     * Find template by provider's internal ID.
     */
    Optional<SmsTemplate> findByProviderTemplateIdAndProvider(String providerTemplateId, SmsProviderType provider);

    /**
     * Find all active templates.
     */
    List<SmsTemplate> findByActiveTrue();

    /**
     * Find templates by status.
     */
    List<SmsTemplate> findByStatusAndActiveTrue(SmsTemplateStatus status);

    /**
     * Find templates pending sync (DRAFT status).
     */
    @Query("SELECT t FROM SmsTemplate t WHERE t.status = 'DRAFT' AND t.active = true")
    List<SmsTemplate> findTemplatesPendingSync();

    /**
     * Check if template code exists for provider.
     */
    boolean existsByTemplateCodeAndProvider(String templateCode, SmsProviderType provider);

    /**
     * Find approved template for a type and provider (returns the most recently updated one).
     */
    Optional<SmsTemplate> findFirstByTemplateTypeAndProviderAndStatusAndActiveTrueOrderByUpdatedAtDesc(
            SmsTemplateType templateType, SmsProviderType provider, SmsTemplateStatus status);

    /**
     * Count templates by status.
     */
    long countByStatusAndActiveTrue(SmsTemplateStatus status);
}

package com.fooddelivery.sms.service;

import com.fooddelivery.sms.dto.SmsMessage;
import com.fooddelivery.sms.dto.SmsSendResponse;
import com.fooddelivery.sms.dto.SmsTemplateSyncResponse;
import com.fooddelivery.sms.entity.SmsTemplate;

import java.util.List;

/**
 * Common interface for SMS providers.
 * Implementations include Eskiz and DevSMS.
 */
public interface SmsProvider {

    /**
     * Send an SMS message.
     *
     * @param smsMessage the SMS message to send
     * @return the response from the SMS provider
     */
    SmsSendResponse sendSms(SmsMessage smsMessage);

    /**
     * Send SMS directly with phone number and message.
     *
     * @param phoneNumber recipient phone number
     * @param message SMS content
     * @return the response from the SMS provider
     */
    SmsSendResponse sendSms(String phoneNumber, String message);

    /**
     * Check if the provider is available and properly configured.
     *
     * @return true if the provider is ready to send messages
     */
    boolean isAvailable();

    /**
     * Get the provider name.
     *
     * @return provider identifier
     */
    String getProviderName();

    /**
     * Get SMS delivery status.
     *
     * @param smsId the SMS ID to check
     * @return status string
     */
    String getSmsStatus(String smsId);

    // ==================== Template Management ====================

    /**
     * Register a template with the SMS provider.
     *
     * @param template the template to register
     * @return sync response with provider template ID and status
     */
    SmsTemplateSyncResponse registerTemplate(SmsTemplate template);

    /**
     * Update an existing template on the SMS provider.
     *
     * @param template the template to update
     * @return sync response with updated status
     */
    SmsTemplateSyncResponse updateTemplate(SmsTemplate template);

    /**
     * Delete a template from the SMS provider.
     *
     * @param template the template to delete
     * @return sync response indicating success/failure
     */
    SmsTemplateSyncResponse deleteTemplate(SmsTemplate template);

    /**
     * Get template status from the SMS provider.
     *
     * @param providerTemplateId the provider's template ID
     * @return sync response with current status
     */
    SmsTemplateSyncResponse getTemplateStatus(String providerTemplateId);

    /**
     * List all templates registered with the provider, WITH their text.
     *
     * <p>Returns ProviderTemplateDto rather than a sync response: importing
     * needs the message content, and a sync response carries only an id and a
     * status.
     *
     * @return templates as the provider holds them; empty if unsupported
     */
    List<com.fooddelivery.sms.dto.ProviderTemplateDto> listProviderTemplates();

    /**
     * Send SMS using a registered template.
     *
     * @param phoneNumber recipient phone number
     * @param providerTemplateId provider's template ID
     * @param variables template variables as key-value pairs
     * @return the response from the SMS provider
     */
    SmsSendResponse sendTemplatedSms(String phoneNumber, String providerTemplateId, java.util.Map<String, String> variables);
}

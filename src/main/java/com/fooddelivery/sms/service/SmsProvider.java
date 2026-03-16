package com.fooddelivery.sms.service;

import com.fooddelivery.sms.dto.SmsMessage;
import com.fooddelivery.sms.dto.SmsSendResponse;

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
}

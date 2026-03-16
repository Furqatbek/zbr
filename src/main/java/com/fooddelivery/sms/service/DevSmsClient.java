package com.fooddelivery.sms.service;

import com.fooddelivery.sms.config.DevSmsProperties;
import com.fooddelivery.sms.dto.DevSmsSendResponse;
import com.fooddelivery.sms.dto.SmsMessage;
import com.fooddelivery.sms.dto.SmsSendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client service for DevSMS gateway.
 * API Documentation: https://devsms.uz/api/docs.php
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DevSmsClient implements SmsProvider {

    private final DevSmsProperties properties;
    private final RestTemplate restTemplate;

    private static final String SEND_SMS_ENDPOINT = "/send_sms.php";
    private static final String GET_STATUS_ENDPOINT = "/get_status.php";
    private static final String GET_BALANCE_ENDPOINT = "/get_balance.php";

    @Override
    public SmsSendResponse sendSms(SmsMessage smsMessage) {
        if (!isAvailable()) {
            log.warn("DevSMS client is not available");
            return SmsSendResponse.failure("DevSMS is not configured", "NOT_CONFIGURED", getProviderName());
        }

        String url = properties.getBaseUrl() + SEND_SMS_ENDPOINT;

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("phone", normalizePhoneNumber(smsMessage.getPhoneNumber()));
        body.put("message", smsMessage.getMessage());
        body.put("from", properties.getFrom());

        if (isValidCallbackUrl(properties.getCallbackUrl())) {
            body.put("callback_url", properties.getCallbackUrl());
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<DevSmsSendResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    DevSmsSendResponse.class
            );

            DevSmsSendResponse devSmsResponse = response.getBody();

            if (devSmsResponse != null && devSmsResponse.isSuccess()) {
                log.info("SMS sent successfully via DevSMS to {}: smsId={}, status={}",
                        maskPhoneNumber(smsMessage.getPhoneNumber()),
                        devSmsResponse.getData() != null ? devSmsResponse.getData().getSmsId() : "unknown",
                        devSmsResponse.getData() != null ? devSmsResponse.getData().getStatus() : "unknown");
                return devSmsResponse.toSmsSendResponse();
            } else {
                String error = devSmsResponse != null ? devSmsResponse.getError() : "Unknown error";
                log.error("DevSMS send failed to {}: {}", maskPhoneNumber(smsMessage.getPhoneNumber()), error);
                return SmsSendResponse.failure(error, "SEND_FAILED", getProviderName());
            }

        } catch (Exception e) {
            log.error("Failed to send SMS via DevSMS to {}: {}",
                    maskPhoneNumber(smsMessage.getPhoneNumber()), e.getMessage());
            return SmsSendResponse.failure(e.getMessage(), "EXCEPTION", getProviderName());
        }
    }

    @Override
    public SmsSendResponse sendSms(String phoneNumber, String message) {
        SmsMessage smsMessage = SmsMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .phoneNumber(phoneNumber)
                .message(message)
                .type(SmsMessage.SmsType.GENERAL)
                .build();

        return sendSms(smsMessage);
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled() && properties.getToken() != null && !properties.getToken().isBlank();
    }

    @Override
    public String getProviderName() {
        return "DEVSMS";
    }

    @Override
    public String getSmsStatus(String smsId) {
        if (!isAvailable()) {
            return "NOT_AVAILABLE";
        }

        String url = properties.getBaseUrl() + GET_STATUS_ENDPOINT + "?sms_id=" + smsId;

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map body = response.getBody();
            if (body != null && body.containsKey("data")) {
                Map data = (Map) body.get("data");
                return data.get("status") != null ? data.get("status").toString() : "UNKNOWN";
            }
            return "UNKNOWN";

        } catch (Exception e) {
            log.error("Failed to get SMS status from DevSMS for {}: {}", smsId, e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Get account balance.
     */
    public Double getBalance() {
        if (!isAvailable()) {
            return null;
        }

        String url = properties.getBaseUrl() + GET_BALANCE_ENDPOINT;

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map body = response.getBody();
            if (body != null && body.containsKey("data")) {
                Map data = (Map) body.get("data");
                Object balance = data.get("balance");
                if (balance instanceof Number) {
                    return ((Number) balance).doubleValue();
                }
            }
            return null;

        } catch (Exception e) {
            log.error("Failed to get balance from DevSMS: {}", e.getMessage());
            return null;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getToken());
        return headers;
    }

    /**
     * Normalize phone number to Uzbekistan format (998XXXXXXXXX).
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        // Remove all non-digit characters
        String digits = phoneNumber.replaceAll("[^0-9]", "");

        // Handle different formats
        if (digits.startsWith("998") && digits.length() == 12) {
            return digits;
        } else if (digits.startsWith("8") && digits.length() == 10) {
            return "998" + digits.substring(1);
        } else if (digits.length() == 9) {
            return "998" + digits;
        } else if (digits.startsWith("+998")) {
            return digits.substring(1);
        }

        return digits;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "***";
        }
        return phoneNumber.substring(0, 6) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }

    /**
     * Validate callback URL - must start with http:// or https://.
     */
    private boolean isValidCallbackUrl(String callbackUrl) {
        return callbackUrl != null
                && !callbackUrl.isBlank()
                && (callbackUrl.startsWith("http://") || callbackUrl.startsWith("https://"));
    }
}

package com.fooddelivery.sms.service;

import com.fooddelivery.sms.config.EskizSmsProperties;
import com.fooddelivery.sms.dto.EskizAuthResponse;
import com.fooddelivery.sms.dto.EskizSendSmsRequest;
import com.fooddelivery.sms.dto.EskizSendSmsResponse;
import com.fooddelivery.sms.dto.SmsMessage;
import com.fooddelivery.sms.dto.SmsSendResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client service for Eskiz SMS gateway.
 * Handles authentication, token refresh, and SMS sending.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.sms.eskiz", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EskizSmsClient implements SmsProvider {

    private final EskizSmsProperties properties;
    private final RestTemplate restTemplate;

    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> tokenExpiresAt = new AtomicReference<>();

    private static final String AUTH_ENDPOINT = "/auth/login";
    private static final String REFRESH_TOKEN_ENDPOINT = "/auth/refresh";
    private static final String SEND_SMS_ENDPOINT = "/message/sms/send";
    private static final String SEND_BATCH_SMS_ENDPOINT = "/message/sms/send-batch";
    private static final String GET_STATUS_ENDPOINT = "/message/sms/status";

    @PostConstruct
    public void init() {
        if (properties.isEnabled() && properties.getEmail() != null && properties.getPassword() != null) {
            try {
                authenticate();
                log.info("Eskiz SMS client initialized successfully");
            } catch (Exception e) {
                log.warn("Failed to initialize Eskiz SMS client: {}. Will retry on first SMS send.", e.getMessage());
            }
        }
    }

    /**
     * Authenticate with Eskiz API and obtain access token.
     */
    public void authenticate() {
        String url = properties.getBaseUrl() + AUTH_ENDPOINT;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("email", properties.getEmail());
        body.add("password", properties.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<EskizAuthResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    EskizAuthResponse.class
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                String token = response.getBody().getData().getToken();
                accessToken.set(token);
                tokenExpiresAt.set(LocalDateTime.now().plusSeconds(properties.getTokenTtlSeconds()));
                log.info("Eskiz authentication successful, token expires at: {}", tokenExpiresAt.get());
            } else {
                throw new RuntimeException("Invalid authentication response from Eskiz");
            }
        } catch (Exception e) {
            log.error("Eskiz authentication failed: {}", e.getMessage());
            throw new RuntimeException("Failed to authenticate with Eskiz SMS gateway", e);
        }
    }

    /**
     * Refresh the authentication token.
     */
    @Scheduled(cron = "0 0 0 */25 * *") // Refresh every 25 days
    public void refreshToken() {
        if (!properties.isEnabled() || accessToken.get() == null) {
            return;
        }

        String url = properties.getBaseUrl() + REFRESH_TOKEN_ENDPOINT;

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<EskizAuthResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    request,
                    EskizAuthResponse.class
            );

            if (response.getBody() != null && response.getBody().getData() != null) {
                String token = response.getBody().getData().getToken();
                accessToken.set(token);
                tokenExpiresAt.set(LocalDateTime.now().plusSeconds(properties.getTokenTtlSeconds()));
                log.info("Eskiz token refreshed successfully");
            }
        } catch (Exception e) {
            log.error("Failed to refresh Eskiz token: {}. Re-authenticating...", e.getMessage());
            authenticate();
        }
    }

    /**
     * Send SMS message.
     *
     * @param smsMessage the SMS message to send
     * @return the response from Eskiz API
     */
    @Override
    public SmsSendResponse sendSms(SmsMessage smsMessage) {
        if (!isAvailable()) {
            log.warn("Eskiz SMS client is not available");
            return SmsSendResponse.failure("Eskiz is not configured", "NOT_CONFIGURED", getProviderName());
        }

        ensureAuthenticated();

        String url = properties.getBaseUrl() + SEND_SMS_ENDPOINT;

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("mobile_phone", normalizePhoneNumber(smsMessage.getPhoneNumber()));
        body.add("message", smsMessage.getMessage());
        body.add("from", properties.getFrom());

        if (smsMessage.getMessageId() != null) {
            body.add("user_sms_id", smsMessage.getMessageId());
        }

        if (isValidCallbackUrl(properties.getCallbackUrl())) {
            body.add("callback_url", properties.getCallbackUrl());
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<EskizSendSmsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    EskizSendSmsResponse.class
            );

            EskizSendSmsResponse smsResponse = response.getBody();
            log.info("SMS sent successfully to {}: messageId={}, status={}",
                    maskPhoneNumber(smsMessage.getPhoneNumber()),
                    smsResponse != null ? smsResponse.getId() : "unknown",
                    smsResponse != null ? smsResponse.getStatus() : "unknown");

            if (smsResponse != null) {
                return SmsSendResponse.builder()
                        .success(true)
                        .smsId(smsResponse.getId())
                        .status(smsResponse.getStatus())
                        .message(smsResponse.getMessage())
                        .provider(getProviderName())
                        .build();
            }
            return SmsSendResponse.success(null, "sent", getProviderName());

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Token expired, re-authenticating and retrying...");
            authenticate();
            return sendSms(smsMessage);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}",
                    maskPhoneNumber(smsMessage.getPhoneNumber()), e.getMessage());
            return SmsSendResponse.failure(e.getMessage(), "EXCEPTION", getProviderName());
        }
    }

    /**
     * Send SMS directly with phone and message.
     */
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
    public String getProviderName() {
        return "ESKIZ";
    }

    /**
     * Get SMS delivery status.
     */
    public String getSmsStatus(String smsId) {
        ensureAuthenticated();

        String url = properties.getBaseUrl() + GET_STATUS_ENDPOINT + "/" + smsId;

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get SMS status for {}: {}", smsId, e.getMessage());
            throw new RuntimeException("Failed to get SMS status", e);
        }
    }

    /**
     * Check if the service is properly configured and authenticated.
     */
    public boolean isAvailable() {
        return properties.isEnabled() && accessToken.get() != null;
    }

    private void ensureAuthenticated() {
        if (accessToken.get() == null || isTokenExpired()) {
            authenticate();
        }
    }

    private boolean isTokenExpired() {
        LocalDateTime expiresAt = tokenExpiresAt.get();
        return expiresAt == null || LocalDateTime.now().isAfter(expiresAt.minusHours(24));
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.get());
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
            // Local format starting with 8
            return "998" + digits.substring(1);
        } else if (digits.length() == 9) {
            // Just the local number
            return "998" + digits;
        } else if (digits.startsWith("+998")) {
            return digits.substring(1);
        }

        return digits;
    }

    /**
     * Mask phone number for logging.
     */
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

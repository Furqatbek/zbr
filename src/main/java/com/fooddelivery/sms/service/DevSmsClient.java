package com.fooddelivery.sms.service;

import com.fooddelivery.sms.config.DevSmsProperties;
import com.fooddelivery.sms.dto.DevSmsSendResponse;
import com.fooddelivery.sms.dto.SmsMessage;
import com.fooddelivery.sms.dto.SmsSendResponse;
import com.fooddelivery.sms.dto.SmsTemplateSyncResponse;
import com.fooddelivery.sms.entity.SmsTemplate;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final String TEMPLATE_ENDPOINT = "/template.php";
    private static final String SEND_TEMPLATE_SMS_ENDPOINT = "/send_template_sms.php";

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

    // ==================== Template Management ====================

    @Override
    public SmsTemplateSyncResponse registerTemplate(SmsTemplate template) {
        if (!isAvailable()) {
            return SmsTemplateSyncResponse.failure("DevSMS is not configured", "NOT_CONFIGURED", getProviderName());
        }

        String url = properties.getBaseUrl() + TEMPLATE_ENDPOINT;

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("action", "create");
        body.put("name", template.getName());
        body.put("text", template.getContent());
        body.put("type", mapTemplateType(template.getTemplateType()));
        if (template.getVariables() != null) {
            body.put("variables", template.getVariables());
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                Map data = (Map) responseBody.get("data");
                String templateId = data != null && data.get("template_id") != null
                        ? data.get("template_id").toString() : null;
                String status = data != null && data.get("status") != null
                        ? data.get("status").toString() : "pending";

                log.info("Template registered with DevSMS: id={}, status={}", templateId, status);
                return SmsTemplateSyncResponse.success(templateId, mapProviderStatus(status), getProviderName());
            }

            String error = responseBody != null ? (String) responseBody.get("error") : "Unknown error";
            return SmsTemplateSyncResponse.failure(error, "API_ERROR", getProviderName());

        } catch (Exception e) {
            log.error("Failed to register template with DevSMS: {}", e.getMessage());
            return SmsTemplateSyncResponse.failure(e.getMessage(), "EXCEPTION", getProviderName());
        }
    }

    @Override
    public SmsTemplateSyncResponse updateTemplate(SmsTemplate template) {
        if (!isAvailable() || template.getProviderTemplateId() == null) {
            return SmsTemplateSyncResponse.failure("Cannot update template", "INVALID_STATE", getProviderName());
        }

        String url = properties.getBaseUrl() + TEMPLATE_ENDPOINT;

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("action", "update");
        body.put("template_id", template.getProviderTemplateId());
        body.put("name", template.getName());
        body.put("text", template.getContent());
        body.put("type", mapTemplateType(template.getTemplateType()));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                log.info("Template updated on DevSMS: id={}", template.getProviderTemplateId());
                return SmsTemplateSyncResponse.success(template.getProviderTemplateId(),
                        SmsTemplateStatus.PENDING, getProviderName());
            }

            String error = responseBody != null ? (String) responseBody.get("error") : "Unknown error";
            return SmsTemplateSyncResponse.failure(error, "API_ERROR", getProviderName());

        } catch (Exception e) {
            log.error("Failed to update template on DevSMS: {}", e.getMessage());
            return SmsTemplateSyncResponse.failure(e.getMessage(), "EXCEPTION", getProviderName());
        }
    }

    @Override
    public SmsTemplateSyncResponse deleteTemplate(SmsTemplate template) {
        if (!isAvailable() || template.getProviderTemplateId() == null) {
            return SmsTemplateSyncResponse.failure("Cannot delete template", "INVALID_STATE", getProviderName());
        }

        String url = properties.getBaseUrl() + TEMPLATE_ENDPOINT;

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("action", "delete");
        body.put("template_id", template.getProviderTemplateId());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                log.info("Template deleted from DevSMS: id={}", template.getProviderTemplateId());
                return SmsTemplateSyncResponse.success(template.getProviderTemplateId(),
                        SmsTemplateStatus.DRAFT, getProviderName());
            }

            String error = responseBody != null ? (String) responseBody.get("error") : "Unknown error";
            return SmsTemplateSyncResponse.failure(error, "API_ERROR", getProviderName());

        } catch (Exception e) {
            log.error("Failed to delete template from DevSMS: {}", e.getMessage());
            return SmsTemplateSyncResponse.failure(e.getMessage(), "EXCEPTION", getProviderName());
        }
    }

    @Override
    public SmsTemplateSyncResponse getTemplateStatus(String providerTemplateId) {
        if (!isAvailable() || providerTemplateId == null) {
            return SmsTemplateSyncResponse.failure("Cannot get template status", "INVALID_STATE", getProviderName());
        }

        String url = properties.getBaseUrl() + TEMPLATE_ENDPOINT + "?action=get&template_id=" + providerTemplateId;

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                Map data = (Map) responseBody.get("data");
                String status = data != null && data.get("status") != null
                        ? data.get("status").toString() : "unknown";
                return SmsTemplateSyncResponse.success(providerTemplateId, mapProviderStatus(status), getProviderName());
            }

            return SmsTemplateSyncResponse.failure("Template not found", "NOT_FOUND", getProviderName());

        } catch (Exception e) {
            log.error("Failed to get template status from DevSMS: {}", e.getMessage());
            return SmsTemplateSyncResponse.failure(e.getMessage(), "EXCEPTION", getProviderName());
        }
    }

    @Override
    public List<SmsTemplateSyncResponse> listProviderTemplates() {
        List<SmsTemplateSyncResponse> templates = new ArrayList<>();

        if (!isAvailable()) {
            return templates;
        }

        String url = properties.getBaseUrl() + TEMPLATE_ENDPOINT + "?action=list";

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                Object data = responseBody.get("data");
                if (data instanceof List) {
                    for (Object item : (List) data) {
                        if (item instanceof Map) {
                            Map templateData = (Map) item;
                            String templateId = templateData.get("template_id") != null
                                    ? templateData.get("template_id").toString() : null;
                            String status = templateData.get("status") != null
                                    ? templateData.get("status").toString() : "unknown";
                            templates.add(SmsTemplateSyncResponse.success(templateId, mapProviderStatus(status), getProviderName()));
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to list templates from DevSMS: {}", e.getMessage());
        }

        return templates;
    }

    @Override
    public SmsSendResponse sendTemplatedSms(String phoneNumber, String providerTemplateId, Map<String, String> variables) {
        if (!isAvailable()) {
            return SmsSendResponse.failure("DevSMS is not configured", "NOT_CONFIGURED", getProviderName());
        }

        String url = properties.getBaseUrl() + SEND_TEMPLATE_SMS_ENDPOINT;

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("phone", normalizePhoneNumber(phoneNumber));
        body.put("template_id", providerTemplateId);
        body.put("params", variables != null ? variables : Map.of());

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
                log.info("Templated SMS sent via DevSMS to {}: smsId={}",
                        maskPhoneNumber(phoneNumber),
                        devSmsResponse.getData() != null ? devSmsResponse.getData().getSmsId() : "unknown");
                return devSmsResponse.toSmsSendResponse();
            } else {
                String error = devSmsResponse != null ? devSmsResponse.getError() : "Unknown error";
                log.error("DevSMS templated send failed to {}: {}", maskPhoneNumber(phoneNumber), error);
                return SmsSendResponse.failure(error, "SEND_FAILED", getProviderName());
            }

        } catch (Exception e) {
            log.error("Failed to send templated SMS via DevSMS to {}: {}",
                    maskPhoneNumber(phoneNumber), e.getMessage());
            return SmsSendResponse.failure(e.getMessage(), "EXCEPTION", getProviderName());
        }
    }

    /**
     * Map our template type to DevSMS template type.
     */
    private String mapTemplateType(SmsTemplate.SmsTemplateType type) {
        return switch (type) {
            case OTP -> "otp";
            case ORDER_CONFIRMATION, ORDER_STATUS -> "transactional";
            case PROMOTIONAL -> "promotional";
            default -> "general";
        };
    }

    /**
     * Map DevSMS status to our status enum.
     */
    private SmsTemplateStatus mapProviderStatus(String status) {
        if (status == null) {
            return SmsTemplateStatus.PENDING;
        }
        return switch (status.toLowerCase()) {
            case "approved", "active" -> SmsTemplateStatus.APPROVED;
            case "rejected" -> SmsTemplateStatus.REJECTED;
            case "suspended", "blocked" -> SmsTemplateStatus.SUSPENDED;
            case "draft" -> SmsTemplateStatus.DRAFT;
            default -> SmsTemplateStatus.PENDING;
        };
    }
}

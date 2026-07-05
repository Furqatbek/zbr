package com.fooddelivery.notification.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivers push notifications to Expo push tokens via the Expo Push API
 * ({@code https://exp.host/--/api/v2/push/send}). The mobile apps register Expo
 * tokens ({@code ExponentPushToken[...]}), which Firebase Cloud Messaging cannot
 * target directly; Expo fans out to FCM (Android) and APNs (iOS) itself.
 *
 * FCM tokens continue to go through {@code PushNotificationConsumer}'s Firebase
 * path; routing is by token format, so both transports coexist.
 */
@Service
@Slf4j
public class ExpoPushService {

    private static final int BATCH_SIZE = 100; // Expo accepts up to 100 messages per request

    private final UserDeviceTokenRepository deviceTokenRepository;
    private final RestTemplate restTemplate;
    private final boolean enabled;
    private final String expoUrl;
    private final String accessToken;

    public ExpoPushService(
            UserDeviceTokenRepository deviceTokenRepository,
            @Value("${app.expo.push.enabled:true}") boolean enabled,
            @Value("${app.expo.push.url:https://exp.host/--/api/v2/push/send}") String expoUrl,
            @Value("${app.expo.push.access-token:}") String accessToken) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.enabled = enabled;
        this.expoUrl = expoUrl;
        this.accessToken = accessToken;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /** @return true for Expo push tokens (ExponentPushToken[...] / ExpoPushToken[...]). */
    public static boolean isExpoToken(String token) {
        return token != null
                && (token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken["));
    }

    /**
     * Send a notification to the given Expo tokens (batched). Never throws — a
     * push transport failure must not break the notification pipeline.
     */
    public void send(String title, String body, Map<String, String> data, List<String> expoTokens) {
        if (!enabled) {
            log.warn("Expo push disabled; skipping {} token(s)", expoTokens.size());
            return;
        }
        for (int i = 0; i < expoTokens.size(); i += BATCH_SIZE) {
            sendBatch(title, body, data, expoTokens.subList(i, Math.min(i + BATCH_SIZE, expoTokens.size())));
        }
    }

    private void sendBatch(String title, String body, Map<String, String> data, List<String> tokens) {
        List<Map<String, Object>> messages = new ArrayList<>(tokens.size());
        for (String to : tokens) {
            Map<String, Object> message = new HashMap<>();
            message.put("to", to);
            message.put("title", title);
            message.put("body", body);
            message.put("sound", "default");
            message.put("priority", "high");
            message.put("channelId", "default");
            if (data != null && !data.isEmpty()) {
                message.put("data", data);
            }
            messages.add(message);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }

        try {
            ResponseEntity<ExpoResponse> response = restTemplate.postForEntity(
                    expoUrl, new HttpEntity<>(messages, headers), ExpoResponse.class);
            handleTickets(tokens, response.getBody());
            log.debug("Expo push sent to {} token(s)", tokens.size());
        } catch (Exception e) {
            log.error("Expo push send failed for {} token(s): {}", tokens.size(), e.getMessage());
        }
    }

    /** Deactivate tokens Expo reports as no longer registered. */
    private void handleTickets(List<String> tokens, ExpoResponse body) {
        if (body == null || body.getData() == null) {
            return;
        }
        List<ExpoTicket> tickets = body.getData();
        for (int i = 0; i < tickets.size() && i < tokens.size(); i++) {
            ExpoTicket ticket = tickets.get(i);
            if (!"ok".equalsIgnoreCase(ticket.getStatus())) {
                String error = ticket.getDetails() != null ? ticket.getDetails().getError() : null;
                if ("DeviceNotRegistered".equals(error)) {
                    deviceTokenRepository.deactivateToken(tokens.get(i));
                    log.info("Deactivated unregistered Expo token");
                } else {
                    log.warn("Expo push error: status={} message={} error={}",
                            ticket.getStatus(), ticket.getMessage(), error);
                }
            }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExpoResponse {
        private List<ExpoTicket> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExpoTicket {
        private String status;
        private String id;
        private String message;
        private ExpoTicketDetails details;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExpoTicketDetails {
        private String error;
    }
}

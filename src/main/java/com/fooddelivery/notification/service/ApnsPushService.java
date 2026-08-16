package com.fooddelivery.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivers push notifications to iOS devices via APNs over HTTP/2, authenticated
 * with a provider (.p8) token — no Firebase involvement on iOS.
 *
 * Android tokens continue through FCM ({@code PushNotificationConsumer}); routing
 * is by the device's registered platform, so both transports coexist.
 *
 * Config:
 * <pre>
 *   app.apns.enabled=true
 *   app.apns.key-file=/run/secrets/apns_auth_key.p8   (secret)
 *   app.apns.key-id=ABCDE12345
 *   app.apns.team-id=VQ56W9S7S9
 *   app.apns.topic=com.zbr.owner
 *   app.apns.production=false   (false -> api.sandbox.push.apple.com)
 * </pre>
 */
@Service
@Slf4j
public class ApnsPushService {

    private static final String PROD_HOST = "https://api.push.apple.com";
    private static final String SANDBOX_HOST = "https://api.sandbox.push.apple.com";
    /** Apple accepts a provider token for 1h; refresh well before that. */
    private static final Duration TOKEN_TTL = Duration.ofMinutes(45);

    private final UserDeviceTokenRepository deviceTokenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    private final boolean enabled;
    private final String keyFile;
    private final String keyId;
    private final String teamId;
    private final String topic;
    private final String host;

    private volatile String cachedJwt;
    private volatile Instant cachedJwtIssuedAt;

    public ApnsPushService(
            UserDeviceTokenRepository deviceTokenRepository,
            @Value("${app.apns.enabled:false}") boolean enabled,
            @Value("${app.apns.key-file:}") String keyFile,
            @Value("${app.apns.key-id:}") String keyId,
            @Value("${app.apns.team-id:}") String teamId,
            @Value("${app.apns.topic:}") String topic,
            @Value("${app.apns.production:false}") boolean production) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.enabled = enabled;
        this.keyFile = keyFile;
        this.keyId = keyId;
        this.teamId = teamId;
        this.topic = topic;
        this.host = production ? PROD_HOST : SANDBOX_HOST;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Send an alert push to the given APNs device tokens. Never throws — a push
     * transport failure must not break the notification pipeline.
     *
     * @param data extra top-level keys (e.g. {@code type}, {@code orderId})
     */
    public void send(String title, String body, Map<String, String> data, List<String> deviceTokens) {
        if (!enabled) {
            log.warn("APNs disabled; skipping {} iOS token(s). Set app.apns.* to enable.", deviceTokens.size());
            return;
        }
        String payload;
        try {
            payload = buildPayload(title, body, data);
        } catch (Exception e) {
            log.error("APNs payload build failed: {}", e.getMessage());
            return;
        }
        for (String deviceToken : deviceTokens) {
            sendOne(deviceToken, payload);
        }
    }

    private String buildPayload(String title, String body, Map<String, String> data) throws IOException {
        Map<String, Object> alert = new HashMap<>();
        alert.put("title", title);
        alert.put("body", body);

        Map<String, Object> aps = new HashMap<>();
        aps.put("alert", alert);
        aps.put("sound", "new_order.wav");
        aps.put("badge", 1);
        // Breaks through Focus / Do Not Disturb for time-critical order alerts.
        aps.put("interruption-level", "time-sensitive");

        Map<String, Object> root = new HashMap<>();
        root.put("aps", aps);
        // Custom keys sit alongside "aps" at the top level (type, orderId, ...).
        if (data != null) {
            data.forEach((k, v) -> {
                if (v != null) {
                    root.put(k, v);
                }
            });
        }
        return objectMapper.writeValueAsString(root);
    }

    private void sendOne(String deviceToken, String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(host + "/3/device/" + deviceToken))
                    .header("authorization", "bearer " + providerToken())
                    .header("apns-push-type", "alert")
                    .header("apns-priority", "10")
                    .header("apns-topic", topic)
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 200) {
                log.debug("APNs push delivered");
            } else if (status == 410) {
                // Device no longer registered — prune it.
                deviceTokenRepository.deactivateToken(deviceToken);
                log.info("Deactivated unregistered APNs token (410)");
            } else if (status == 400 && response.body() != null && response.body().contains("BadDeviceToken")) {
                // Wrong environment (sandbox token sent to prod, or vice versa) or malformed.
                deviceTokenRepository.deactivateToken(deviceToken);
                log.warn("Deactivated APNs token: BadDeviceToken (check sandbox vs production). body={}",
                        response.body());
            } else {
                log.warn("APNs push failed: status={} body={}", status, response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("APNs push interrupted");
        } catch (Exception e) {
            log.error("APNs push error: {}", e.getMessage());
        }
    }

    /** Cached ES256 provider token (Apple rate-limits token generation). */
    private synchronized String providerToken() throws Exception {
        Instant now = Instant.now();
        if (cachedJwt != null && cachedJwtIssuedAt != null
                && Duration.between(cachedJwtIssuedAt, now).compareTo(TOKEN_TTL) < 0) {
            return cachedJwt;
        }
        PrivateKey privateKey = loadPrivateKey();
        cachedJwt = Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .issuedAt(Date.from(now))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
        cachedJwtIssuedAt = now;
        log.debug("Minted new APNs provider token");
        return cachedJwt;
    }

    /** Load the PKCS#8 EC private key from the .p8 file. */
    private PrivateKey loadPrivateKey() throws Exception {
        String pem = Files.readString(Path.of(keyFile), StandardCharsets.UTF_8);
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}

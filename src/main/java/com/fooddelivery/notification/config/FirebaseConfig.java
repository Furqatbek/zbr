package com.fooddelivery.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Firebase configuration for push notifications via FCM (Android).
 *
 * Enable by setting {@code app.firebase.enabled=true} plus the service-account
 * credentials, supplied EITHER as base64 in the environment (nothing on disk,
 * secret-manager friendly) OR as a file:
 *
 *   app.firebase.credentials-base64=<base64 of the service-account JSON>   (wins)
 *   app.firebase.credentials-file=/run/secrets/firebase-service-account.json
 *
 * NOTE: this is the service-account private key (a SECRET), not the client-side
 * google-services.json that ships inside the Android app.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.credentials-file:firebase-service-account.json}")
    private String credentialsFile;

    @Value("${app.firebase.credentials-base64:}")
    private String credentialsBase64;

    /**
     * Initialise Firebase, or carry on without it.
     *
     * <p>Deliberately does NOT rethrow. Push is one feature; refusing to start
     * takes down ordering, payments and dispatch as well. A malformed credential
     * should cost notifications, not the platform.
     *
     * <p>Catches Exception rather than IOException because the likeliest failure
     * is a truncated or mis-pasted base64 blob, and
     * {@code Base64.getDecoder().decode} throws IllegalArgumentException — which
     * an IOException-only catch let through, straight out of @PostConstruct and
     * into a failed application context.
     */
    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try {
            InputStream serviceAccount = getCredentialsStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully");
        } catch (Exception e) {
            log.error("PUSH DEGRADED: Firebase failed to initialise, Android push is DISABLED "
                    + "for this run. The application continues without it. Cause: {}: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            log.error("Check FIREBASE_CREDENTIALS_BASE64 — it must be the base64 of the "
                    + "service-account JSON (starts {\"type\":\"service_account\"), NOT "
                    + "google-services.json. Verify with: "
                    + "grep FIREBASE_CREDENTIALS_BASE64 .env | cut -d= -f2- | base64 -d | head -c 80");
        }
    }

    private InputStream getCredentialsStream() throws IOException {
        // Preferred: credentials straight from configuration, so the key never
        // has to land on the server's filesystem. Accepts base64 or raw JSON.
        if (credentialsBase64 != null && !credentialsBase64.isBlank()) {
            String raw = credentialsBase64.trim();
            byte[] json = raw.startsWith("{")
                    ? raw.getBytes(StandardCharsets.UTF_8)
                    : Base64.getDecoder().decode(raw.replaceAll("\\s", ""));
            log.info("Loading Firebase credentials from configuration (base64)");
            return new ByteArrayInputStream(json);
        }

        // Try classpath first
        Resource resource = new ClassPathResource(credentialsFile);
        if (resource.exists()) {
            log.info("Loading Firebase credentials from classpath: {}", credentialsFile);
            return resource.getInputStream();
        }

        // Try file system
        log.info("Loading Firebase credentials from file system: {}", credentialsFile);
        return new FileInputStream(credentialsFile);
    }

    /**
     * The messaging client, or null when Firebase did not initialise.
     *
     * <p>{@code FirebaseMessaging.getInstance()} throws IllegalStateException
     * with no FirebaseApp, so without this guard a failed initialise would still
     * break the context here — one step later than before, but just as fatal.
     *
     * <p>Returning null is deliberate and is handled: PushNotificationConsumer
     * injects this with {@code @Autowired(required = false)} and logs instead of
     * sending when it is absent. iOS is unaffected either way — APNs has its own
     * transport and never touches Firebase.
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("PUSH DEGRADED: no FirebaseMessaging bean — Android push will be logged, not sent.");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}

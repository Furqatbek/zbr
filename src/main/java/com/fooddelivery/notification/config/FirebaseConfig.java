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

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount = getCredentialsStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            } catch (IOException e) {
                log.error("Failed to initialize Firebase: {}", e.getMessage());
                throw new RuntimeException("Failed to initialize Firebase", e);
            }
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

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}

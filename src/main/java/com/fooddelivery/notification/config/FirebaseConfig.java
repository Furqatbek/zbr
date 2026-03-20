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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase configuration for push notifications via FCM.
 *
 * Enable by setting:
 *   app.firebase.enabled=true
 *   app.firebase.credentials-file=path/to/firebase-service-account.json
 */
@Configuration
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.credentials-file:firebase-service-account.json}")
    private String credentialsFile;

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

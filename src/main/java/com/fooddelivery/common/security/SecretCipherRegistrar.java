package com.fooddelivery.common.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Hands the Spring-managed cipher to the JPA converter, which Hibernate builds
 * for itself and cannot be injected into.
 *
 * <p>Its own class rather than a line in a config: the coupling is easy to miss
 * and worth naming, since without it secrets silently fail to encrypt rather
 * than silently encrypting wrongly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecretCipherRegistrar {

    private final SecretCipher cipher;

    @PostConstruct
    void install() {
        EncryptedStringConverter.install(cipher);
        log.info("Secret encryption {}", cipher.isAvailable() ? "enabled" : "NOT configured");
    }
}

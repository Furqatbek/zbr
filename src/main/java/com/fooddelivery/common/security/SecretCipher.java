package com.fooddelivery.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Reversible encryption for secrets we have to be able to read back.
 *
 * <p>Most credentials here are hashed, which is right when the job is to verify
 * one someone presents. This is for the other kind: a key another company
 * issued US, which we have to replay on every call to them. A digest cannot be
 * replayed, so it is encrypted instead — and that is a genuine step down in
 * blast radius, which is why it is confined to this one class rather than
 * spread through the services that use it.
 *
 * <p>AES-GCM, with a fresh random IV per encryption stored alongside the
 * ciphertext. GCM rather than CBC because it authenticates: a tampered value
 * fails to decrypt instead of decrypting into something else. The IV must never
 * repeat under one key, which is what makes it random per call rather than a
 * counter we would have to persist.
 */
@Component
@Slf4j
public class SecretCipher {

    /** Marks a value this class produced, so a legacy plaintext one is recognisable. */
    private static final String PREFIX = "enc:v1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public SecretCipher(@Value("${app.security.secret-key:}") String configuredKey) {
        this.key = parseKey(configuredKey);
        if (key == null) {
            // Not fatal: the platform has one user of this today, and taking
            // every other feature down over an unset key would be a worse
            // outage than the one it prevents. Writing a secret fails loudly
            // instead — see encrypt.
            log.warn("app.security.secret-key is not set. Partner outbound credentials cannot be "
                    + "stored until it is. Generate one with: openssl rand -base64 32");
        }
    }

    public boolean isAvailable() {
        return key != null;
    }

    /**
     * @throws IllegalStateException when no key is configured — deliberately,
     *         rather than storing the value in the clear and calling it done.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (key == null) {
            throw new IllegalStateException("Cannot store a secret: app.security.secret-key is not "
                    + "configured. Generate one with `openssl rand -base64 32` and set "
                    + "APP_SECRET_KEY. Storing it unencrypted is not an option this accepts.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // IV first, then ciphertext-with-tag, base64 as one blob. The IV is
            // not secret; it only has to be unique.
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // Never the secret, and never the exception's own message, which
            // some providers build from the input.
            throw new IllegalStateException("Failed to encrypt a secret: " + e.getClass().getName());
        }
    }

    /**
     * Decrypt, passing through a value this class did not produce.
     *
     * <p>The passthrough is for rows written before encryption existed. It
     * makes the change deployable without a migration window, and it is why
     * {@link #isLegacyPlaintext} exists so those rows can be found and rewritten.
     */
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        if (!stored.startsWith(PREFIX)) {
            return stored;
        }
        if (key == null) {
            throw new IllegalStateException("Cannot read a stored secret: app.security.secret-key "
                    + "is not configured, or is not the key this value was written with.");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(
                    cipher.doFinal(combined, IV_BYTES, combined.length - IV_BYTES),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            // A tampered or truncated value, or the wrong key. GCM makes these
            // indistinguishable on purpose, and all three mean the same thing
            // to a caller: this secret is not usable.
            throw new IllegalStateException("Failed to decrypt a stored secret: "
                    + e.getClass().getName());
        }
    }

    /** True for a value stored before this class existed. */
    public boolean isLegacyPlaintext(String stored) {
        return stored != null && !stored.isBlank() && !stored.startsWith(PREFIX);
    }

    private SecretKeySpec parseKey(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configured.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.security.secret-key must be base64. Generate one with: openssl rand -base64 32");
        }
        if (decoded.length != KEY_BYTES) {
            // Refused rather than padded or hashed into shape: a short key that
            // silently works is a short key nobody ever fixes.
            throw new IllegalStateException("app.security.secret-key must decode to exactly "
                    + KEY_BYTES + " bytes (got " + decoded.length + "). "
                    + "Generate one with: openssl rand -base64 32");
        }
        return new SecretKeySpec(decoded, "AES");
    }
}

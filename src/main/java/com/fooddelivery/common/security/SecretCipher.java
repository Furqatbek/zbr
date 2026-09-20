package com.fooddelivery.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reversible encryption for secrets we have to be able to read back.
 *
 * <p>Most credentials here are hashed, which is right when the job is to verify
 * one someone presents. This is for the other kind: a key another company
 * issued US, which we have to replay on every call to them. A digest cannot be
 * replayed, so it is encrypted instead — and that is a genuine step down in
 * blast radius, which is why it is confined to this one class.
 *
 * <p>AES-GCM, with a fresh random IV per encryption. GCM rather than CBC
 * because it authenticates: a tampered value fails to decrypt instead of
 * decrypting into something else.
 *
 * <h2>Rotation</h2>
 *
 * <p>One PRIMARY key encrypts; it and any number of PREVIOUS keys decrypt. A
 * stored value names the key that wrote it, so rotating is: add the old key to
 * the previous list, make a new one primary, restart, rewrap. Nothing is
 * unreadable at any point in that sequence, which is the whole difference
 * between a rotation and an outage.
 *
 * <p>The key id is derived from the key material rather than configured, so
 * there is no second thing to keep in step and no way to mislabel a key. It is
 * a truncated digest — it identifies a key without being one.
 */
@Component
@Slf4j
public class SecretCipher {

    /** Carries a key id. Values written before rotation existed use {@code enc:v1:}. */
    private static final String PREFIX_V2 = "enc:v2:";
    private static final String PREFIX_V1 = "enc:v1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;
    private static final int KEY_ID_CHARS = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** The key new values are written with. Null when none is configured. */
    private final SecretKeySpec primary;
    private final String primaryId;

    /** Every key we can read with, primary first, by id. */
    private final Map<String, SecretKeySpec> byId = new LinkedHashMap<>();

    public SecretCipher(@Value("${app.security.secret-key:}") String primaryKey,
                        @Value("${app.security.previous-keys:}") String previousKeys) {
        this.primary = parseKey(primaryKey, "app.security.secret-key");
        this.primaryId = primary == null ? null : keyId(primary);

        if (primary != null) {
            byId.put(primaryId, primary);
        }
        for (String previous : split(previousKeys)) {
            SecretKeySpec key = parseKey(previous, "app.security.previous-keys");
            if (key != null) {
                // A previous key equal to the primary is a configuration
                // mistake, not a second key: putIfAbsent keeps the primary.
                byId.putIfAbsent(keyId(key), key);
            }
        }

        if (primary == null) {
            log.warn("app.security.secret-key is not set. Secrets that must be readable back "
                    + "cannot be stored until it is. Generate one with: openssl rand -base64 32");
        } else if (byId.size() > 1) {
            log.info("Secret encryption: primary key {}, {} previous key(s) accepted for reading. "
                    + "Rewrap to retire them.", primaryId, byId.size() - 1);
        }
    }

    /** Convenience for tests and for the converter's uninstalled fallback. */
    public SecretCipher(String primaryKey) {
        this(primaryKey, "");
    }

    public boolean isAvailable() {
        return primary != null;
    }

    /**
     * @throws IllegalStateException when no key is configured — deliberately,
     *         rather than storing the value in the clear and calling it done.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (primary == null) {
            throw new IllegalStateException("Cannot store a secret: app.security.secret-key is not "
                    + "configured. Generate one with `openssl rand -base64 32` and set "
                    + "APP_SECRET_KEY. Storing it unencrypted is not an option this accepts.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, primary, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return PREFIX_V2 + primaryId + ":" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // Never the secret, and never the provider's own message, which
            // some implementations build from the input.
            throw new IllegalStateException("Failed to encrypt a secret: " + e.getClass().getName());
        }
    }

    /**
     * Decrypt with whichever configured key wrote it, passing through a value
     * this class did not produce.
     */
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        if (stored.startsWith(PREFIX_V2)) {
            int separator = stored.indexOf(':', PREFIX_V2.length());
            if (separator < 0) {
                throw new IllegalStateException("Stored secret is malformed");
            }
            String id = stored.substring(PREFIX_V2.length(), separator);
            SecretKeySpec key = byId.get(id);
            if (key == null) {
                // Named precisely, because the remedy is specific: that key
                // belongs in app.security.previous-keys. Saying only "failed to
                // decrypt" would send someone hunting corruption instead.
                throw new IllegalStateException("No configured key can read this secret. It was "
                        + "written with key " + id + "; add that key to app.security.previous-keys.");
            }
            return decryptWith(key, stored.substring(separator + 1));
        }

        if (stored.startsWith(PREFIX_V1)) {
            // Written before values carried a key id. Try each key we hold; GCM
            // makes a wrong one fail rather than return plausible rubbish, which
            // is what makes trying them safe.
            String payload = stored.substring(PREFIX_V1.length());
            for (SecretKeySpec key : byId.values()) {
                try {
                    return decryptWith(key, payload);
                } catch (IllegalStateException ignored) {
                    // Next key.
                }
            }
            throw new IllegalStateException("No configured key can read this secret, which was "
                    + "written before keys were identified. Add the original key to "
                    + "app.security.previous-keys.");
        }

        // Written before encryption existed.
        return stored;
    }

    /**
     * Whether this value should be rewritten under the primary key.
     *
     * <p>True for legacy plaintext, for anything an older key wrote, and for
     * anything without a key id. False when it is already current, so a rewrap
     * is safe to run repeatedly and cheap when there is nothing to do.
     */
    public boolean needsRewrap(String stored) {
        if (stored == null || stored.isBlank()) {
            return false;
        }
        return !stored.startsWith(PREFIX_V2 + primaryId + ":");
    }

    /** True for a value stored before this class existed. */
    public boolean isLegacyPlaintext(String stored) {
        return stored != null && !stored.isBlank()
                && !stored.startsWith(PREFIX_V1) && !stored.startsWith(PREFIX_V2);
    }

    /** The id of the key new values are written with, for logs and reports. */
    public String primaryKeyId() {
        return primaryId;
    }

    private String decryptWith(SecretKeySpec key, String payload) {
        try {
            byte[] combined = Base64.getDecoder().decode(payload);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_BYTES);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(
                    cipher.doFinal(combined, IV_BYTES, combined.length - IV_BYTES),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            // A tampered value, a truncated one, or the wrong key. GCM makes
            // these indistinguishable on purpose, and all three mean the same
            // thing to a caller: this secret is not usable.
            throw new IllegalStateException("Failed to decrypt a stored secret: "
                    + e.getClass().getName());
        }
    }

    /**
     * A short, stable, non-secret name for a key, derived from the key itself
     * so it cannot drift from what it labels.
     */
    private String keyId(SecretKeySpec key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getEncoded());
            return java.util.HexFormat.of().formatHex(digest).substring(0, KEY_ID_CHARS);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private List<String> split(String configured) {
        if (configured == null || configured.isBlank()) {
            return List.of();
        }
        return Arrays.stream(configured.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private SecretKeySpec parseKey(String configured, String property) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configured.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    property + " must be base64. Generate one with: openssl rand -base64 32");
        }
        if (decoded.length != KEY_BYTES) {
            // Refused rather than padded or hashed into shape: a short key that
            // silently works is a short key nobody ever fixes.
            throw new IllegalStateException(property + " must decode to exactly " + KEY_BYTES
                    + " bytes (got " + decoded.length + "). "
                    + "Generate one with: openssl rand -base64 32");
        }
        return new SecretKeySpec(decoded, "AES");
    }
}

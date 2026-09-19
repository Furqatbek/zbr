package com.fooddelivery.integration.partner.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * The shape of a partner API key, and the only place that shape is defined.
 *
 * <p>A key looks like {@code zbrp_<keyId>_<secret>}. Splitting it in two is what
 * lets the secret be stored as a digest while lookup stays a single indexed
 * read: the {@code keyId} finds the row, the secret proves the caller holds it.
 * A key that is only a secret would force either a table scan or storing the
 * secret in a reversible form.
 */
public final class PartnerCredentials {

    /** Marks our keys in a log or a config file, so a leaked one is recognisable. */
    public static final String PREFIX = "zbrp";

    private static final int KEY_ID_LENGTH = 16;
    private static final int SECRET_LENGTH = 40;

    // Unambiguous alphabet: no 0/O or 1/l/I, because these get read aloud,
    // retyped from a chat message and pasted into config by hand.
    private static final char[] ALPHABET =
            "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private PartnerCredentials() {
    }

    public record Generated(String keyId, String secret, String presentedKey, String secretHash) {
    }

    public static Generated generate() {
        String keyId = randomString(KEY_ID_LENGTH);
        String secret = randomString(SECRET_LENGTH);
        return new Generated(keyId, secret, PREFIX + "_" + keyId + "_" + secret, hash(secret));
    }

    /**
     * Split a presented key into its two halves, or null if it is not one of
     * ours. Returns null rather than throwing: a malformed key is an ordinary
     * failed authentication, not an exceptional condition, and it arrives from
     * the open internet.
     */
    public static Presented parse(String presented) {
        if (presented == null) {
            return null;
        }
        String trimmed = presented.trim();
        // Exactly three parts: a secret containing an underscore would otherwise
        // be silently truncated, and the key would never verify.
        String[] parts = trimmed.split("_", 3);
        if (parts.length != 3 || !PREFIX.equals(parts[0]) || parts[1].isEmpty() || parts[2].isEmpty()) {
            return null;
        }
        return new Presented(parts[1], parts[2]);
    }

    public record Presented(String keyId, String secret) {
    }

    public static String hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the platform spec; absent means a broken JVM.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Compare a presented secret against a stored digest without leaking, via
     * timing, how much of it was right.
     *
     * <p>{@code String.equals} returns at the first differing character, so the
     * time it takes reveals the length of the matching prefix. That is enough to
     * recover a secret one character at a time given enough attempts — and a
     * partner API is called by machines, so "enough attempts" is cheap.
     */
    public static boolean matches(String presentedSecret, String storedHash) {
        if (presentedSecret == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(presentedSecret).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

    private static String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}

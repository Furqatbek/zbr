package com.fooddelivery.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Encryption for the one secret here that cannot be hashed.
 *
 * <p>A partner's outbound credential has to be replayed on every call to them,
 * so a digest is no use. It was stored in the clear, which made a database dump
 * a live credential to their production system — and the asymmetry was easy to
 * miss, because their key to call us sat hashed in the same table.
 */
@DisplayName("SecretCipher")
class SecretCipherTest {

    private static final String KEY = key(7);
    private static final String OTHER_KEY = key(42);

    private static String key(int seed) {
        byte[] bytes = new byte[32];
        new java.util.Random(seed).nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private SecretCipher cipher() {
        return new SecretCipher(KEY);
    }

    @Test
    @DisplayName("a secret round-trips")
    void roundTrip() {
        SecretCipher cipher = cipher();
        String secret = "zbrp_7fKq2mXvRtYb_hJ4nPw8sVzA";

        assertThat(cipher.decrypt(cipher.encrypt(secret))).isEqualTo(secret);
    }

    @Test
    @DisplayName("the stored form does not contain the secret")
    void ciphertextHidesTheSecret() {
        String secret = "supersecretpartnerkey";

        assertThat(cipher().encrypt(secret)).doesNotContain(secret);
    }

    @Test
    @DisplayName("the same secret encrypts differently every time")
    void freshIvPerEncryption() {
        // A repeated IV under one key breaks GCM badly, so this is not
        // cosmetic. It also means identical credentials are not identifiable
        // as identical by looking at the column.
        SecretCipher cipher = cipher();
        Set<String> seen = new HashSet<>();
        IntStream.range(0, 100).forEach(i -> seen.add(cipher.encrypt("same-secret")));

        assertThat(seen).hasSize(100);
    }

    @Test
    @DisplayName("a tampered value fails rather than decrypting into something else")
    void tamperingIsDetected() {
        // What GCM buys over CBC. A flipped byte is a failure, not a different
        // credential silently presented to a partner.
        String stored = cipher().encrypt("zbrp_real_key");
        String tampered = stored.substring(0, stored.length() - 2)
                + (stored.endsWith("A") ? "B" : "A") + "=";

        assertThatThrownBy(() -> cipher().decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("another key cannot read it")
    void wrongKeyCannotDecrypt() {
        String stored = cipher().encrypt("zbrp_real_key");

        assertThatThrownBy(() -> new SecretCipher(OTHER_KEY).decrypt(stored))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a value written before encryption still reads")
    void legacyPlaintextPassesThrough() {
        // The whole reason this is deployable without a migration window: rows
        // already in the table keep working until someone re-sends the
        // credential.
        assertThat(cipher().decrypt("plain-old-key")).isEqualTo("plain-old-key");
        assertThat(cipher().isLegacyPlaintext("plain-old-key")).isTrue();
        assertThat(cipher().isLegacyPlaintext(cipher().encrypt("x"))).isFalse();
    }

    @Test
    @DisplayName("nulls pass through both ways")
    void nullsAreFine() {
        assertThat(cipher().encrypt(null)).isNull();
        assertThat(cipher().decrypt(null)).isNull();
        assertThat(cipher().isLegacyPlaintext(null)).isFalse();
    }

    @Test
    @DisplayName("with no key configured, storing a secret fails loudly")
    void noKeyRefusesToStore() {
        // The alternative is writing it in the clear and calling it done, which
        // is the bug this class exists to close.
        SecretCipher unconfigured = new SecretCipher("");

        assertThat(unconfigured.isAvailable()).isFalse();
        assertThatThrownBy(() -> unconfigured.encrypt("secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secret-key");
    }

    @Test
    @DisplayName("with no key configured, a legacy value still reads")
    void noKeyStillReadsLegacy() {
        // So an unset key does not take down a partner that was working
        // yesterday — it only stops new secrets being stored.
        assertThat(new SecretCipher(null).decrypt("plain-old-key")).isEqualTo("plain-old-key");
    }

    @Test
    @DisplayName("a key of the wrong length is refused at construction")
    void shortKeyRefused() {
        // Padded or hashed into shape, a short key silently works and nobody
        // ever fixes it.
        assertThatThrownBy(() -> new SecretCipher(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("a key that is not base64 is refused at construction")
    void malformedKeyRefused() {
        assertThatThrownBy(() -> new SecretCipher("not base64 at all !!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base64");
    }

    @Test
    @DisplayName("a failure never quotes the secret or the library's message")
    void failuresDoNotLeak() {
        // An exception message ends up in a log, a ticket and a screenshot.
        String secret = "zbrp_do_not_appear_in_any_message";
        String stored = cipher().encrypt(secret);

        assertThatThrownBy(() -> new SecretCipher(OTHER_KEY).decrypt(stored))
                .hasMessageNotContaining(secret)
                .hasMessageNotContaining(stored);
    }
}

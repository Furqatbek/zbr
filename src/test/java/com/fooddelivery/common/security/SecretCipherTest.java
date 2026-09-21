package com.fooddelivery.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

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
    @DisplayName("Spring can build the bean")
    void springCanInstantiateIt() {
        // The failure this pins took production down: two public constructors
        // and no @Autowired, so Spring looked for a no-arg one and found none.
        // Nothing caught it, because the only test that loads a context needs
        // Docker and is excluded from `mvn test` — the first thing to ever
        // instantiate this bean was the deployment.
        //
        // A whole context is not needed to ask the question. This is the same
        // constructor resolution, in a container small enough to run anywhere.
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(PropertySourcesPlaceholderConfigurer.class);
            context.register(SecretCipher.class);
            context.refresh();

            assertThat(context.getBean(SecretCipher.class)).isNotNull();
        }
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
        // What GCM buys over CBC. A flipped bit is a failure, not a different
        // credential silently presented to a partner.
        String stored = cipher().encrypt("zbrp_real_key");

        assertThatThrownBy(() -> cipher().decrypt(flipLastCiphertextBit(stored)))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Flip one bit of the authentication tag, in the bytes rather than in the
     * text.
     *
     * <p>This used to substitute a character near the end of the base64 and
     * append a padding '=' — which, depending on where the ciphertext length
     * left the base64 alignment, sometimes decoded to the very same bytes. The
     * test then asserted that an untampered value failed to decrypt, and
     * passed or failed with the random IV. A tamper test that is sometimes not
     * a tamper is worse than no tamper test, because it is read as one.
     */
    private static String flipLastCiphertextBit(String stored) {
        int lastColon = stored.lastIndexOf(':');
        byte[] bytes = Base64.getDecoder().decode(stored.substring(lastColon + 1));
        bytes[bytes.length - 1] ^= 0x01;
        return stored.substring(0, lastColon + 1) + Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    @DisplayName("another key cannot read it")
    void wrongKeyCannotDecrypt() {
        String stored = cipher().encrypt("zbrp_real_key");

        assertThatThrownBy(() -> new SecretCipher(OTHER_KEY).decrypt(stored))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- rotation ----------------------------------------------------------

    @Test
    @DisplayName("a value written by the old key still reads after the new one is primary")
    void rotatedKeyStillReadsOldValues() {
        // THE property. If this failed, rotating a key would be an outage
        // rather than a rotation.
        String stored = new SecretCipher(OTHER_KEY).encrypt("zbrp_written_before_rotation");

        SecretCipher afterRotation = new SecretCipher(KEY, OTHER_KEY);

        assertThat(afterRotation.decrypt(stored)).isEqualTo("zbrp_written_before_rotation");
    }

    @Test
    @DisplayName("new values are written with the primary, not the previous key")
    void newValuesUseThePrimary() {
        SecretCipher afterRotation = new SecretCipher(KEY, OTHER_KEY);
        String stored = afterRotation.encrypt("fresh");

        // Readable by the primary alone, which is what lets the old key be
        // deleted once everything has been rewrapped.
        assertThat(new SecretCipher(KEY).decrypt(stored)).isEqualTo("fresh");
        assertThat(afterRotation.needsRewrap(stored)).isFalse();
    }

    @Test
    @DisplayName("a value under an old key is flagged for rewrapping")
    void oldValuesNeedRewrap() {
        String stored = new SecretCipher(OTHER_KEY).encrypt("old");
        SecretCipher afterRotation = new SecretCipher(KEY, OTHER_KEY);

        // The signal that the rotation is not finished, and the old key cannot
        // yet be deleted.
        assertThat(afterRotation.needsRewrap(stored)).isTrue();
    }

    @Test
    @DisplayName("a rewrapped value no longer needs the old key")
    void rewrapRetiresTheOldKey() {
        SecretCipher afterRotation = new SecretCipher(KEY, OTHER_KEY);
        String old = new SecretCipher(OTHER_KEY).encrypt("secret");

        String rewrapped = afterRotation.encrypt(afterRotation.decrypt(old));

        // The whole point: the old key can now be removed from configuration.
        assertThat(new SecretCipher(KEY).decrypt(rewrapped)).isEqualTo("secret");
    }

    @Test
    @DisplayName("a value whose key is gone says which key it needs")
    void missingKeyIsNamed() {
        // "Failed to decrypt" would send someone hunting corruption. The key id
        // is not secret and it names the remedy exactly.
        String stored = new SecretCipher(OTHER_KEY).encrypt("secret");
        String expectedId = stored.substring("enc:v2:".length(), stored.indexOf(':', 7));

        assertThatThrownBy(() -> new SecretCipher(KEY).decrypt(stored))
                .hasMessageContaining(expectedId)
                .hasMessageContaining("previous-keys");
    }

    @Test
    @DisplayName("a value written before keys were identified still reads")
    void v1ValuesStillRead() {
        // enc:v1: carries no key id, so every configured key is tried. GCM is
        // what makes that safe: a wrong key fails rather than returning
        // plausible rubbish.
        SecretCipher single = new SecretCipher(OTHER_KEY);
        String v1 = legacyV1(single, "written-before-rotation-existed");

        assertThat(new SecretCipher(KEY, OTHER_KEY).decrypt(v1))
                .isEqualTo("written-before-rotation-existed");
    }

    @Test
    @DisplayName("legacy plaintext is rewrapped too")
    void plaintextNeedsRewrap() {
        assertThat(new SecretCipher(KEY).needsRewrap("plain-old-key")).isTrue();
    }

    @Test
    @DisplayName("listing the primary key again as previous does not confuse it")
    void duplicateKeyIsHarmless() {
        // What a careless rotation looks like: the new key left in both places.
        SecretCipher cipher = new SecretCipher(KEY, KEY + "," + OTHER_KEY);

        String stored = cipher.encrypt("secret");
        assertThat(cipher.decrypt(stored)).isEqualTo("secret");
        assertThat(cipher.needsRewrap(stored)).isFalse();
    }

    @Test
    @DisplayName("the key id identifies a key without being one")
    void keyIdIsNotTheKey() {
        String stored = cipher().encrypt("secret");
        String id = stored.substring("enc:v2:".length(), stored.indexOf(':', 7));

        assertThat(id).hasSize(8);
        assertThat(KEY).doesNotContain(id);
    }

    /** Rebuild the pre-rotation format, which nothing writes any more. */
    private String legacyV1(SecretCipher with, String plaintext) {
        String v2 = with.encrypt(plaintext);
        return "enc:v1:" + v2.substring(v2.indexOf(':', "enc:v2:".length()) + 1);
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

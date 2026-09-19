package com.fooddelivery.integration.partner.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shape and handling of a partner credential.
 *
 * <p>Small surface, large consequences: this is the only thing standing between
 * the open internet and the ability to reprice a restaurant's menu or cancel
 * its orders.
 */
@DisplayName("Partner credentials")
class PartnerCredentialsTest {

    @Test
    @DisplayName("a generated key round-trips through parse and verify")
    void roundTrip() {
        PartnerCredentials.Generated generated = PartnerCredentials.generate();

        PartnerCredentials.Presented parsed = PartnerCredentials.parse(generated.presentedKey());

        assertThat(parsed).isNotNull();
        assertThat(parsed.keyId()).isEqualTo(generated.keyId());
        assertThat(PartnerCredentials.matches(parsed.secret(), generated.secretHash())).isTrue();
    }

    @Test
    @DisplayName("the secret is not recoverable from what we store")
    void secretIsNotStored() {
        PartnerCredentials.Generated generated = PartnerCredentials.generate();

        // Anyone reaching the database gets a digest, not a working credential.
        assertThat(generated.secretHash()).doesNotContain(generated.secret());
        assertThat(generated.secretHash()).hasSize(64);
    }

    @Test
    @DisplayName("a wrong secret against a real key id does not verify")
    void wrongSecretRejected() {
        PartnerCredentials.Generated real = PartnerCredentials.generate();
        PartnerCredentials.Generated other = PartnerCredentials.generate();

        assertThat(PartnerCredentials.matches(other.secret(), real.secretHash())).isFalse();
    }

    @Test
    @DisplayName("a secret containing an underscore survives parsing")
    void underscoreInSecretIsNotTruncated() {
        // The alphabet excludes it today, but a split with no limit would
        // truncate such a secret and the key would fail to verify with no
        // indication why. Pinning the three-part split keeps that a
        // non-problem if the alphabet ever changes.
        PartnerCredentials.Presented parsed =
                PartnerCredentials.parse("zbrp_abc123_secret_with_underscores");

        assertThat(parsed).isNotNull();
        assertThat(parsed.keyId()).isEqualTo("abc123");
        assertThat(parsed.secret()).isEqualTo("secret_with_underscores");
    }

    @Test
    @DisplayName("malformed keys parse to nothing rather than throwing")
    void malformedKeysReturnNull() {
        // These arrive from the open internet. An exception per bad request is
        // a denial-of-service lever and a noisy log; a failed authentication is
        // the correct, boring outcome.
        assertThat(PartnerCredentials.parse(null)).isNull();
        assertThat(PartnerCredentials.parse("")).isNull();
        assertThat(PartnerCredentials.parse("garbage")).isNull();
        assertThat(PartnerCredentials.parse("zbrp_only-one-part")).isNull();
        assertThat(PartnerCredentials.parse("zbrp__nosecretid")).isNull();
        assertThat(PartnerCredentials.parse("other_abc_def")).isNull();
        // A JWT sent to the wrong header, which is what a confused integrator does first.
        assertThat(PartnerCredentials.parse("eyJhbGciOiJIUzI1NiJ9.e30.abc")).isNull();
    }

    @Test
    @DisplayName("a null secret or a null stored hash never matches")
    void nullsNeverMatch() {
        assertThat(PartnerCredentials.matches(null, "abc")).isFalse();
        assertThat(PartnerCredentials.matches("abc", null)).isFalse();
        assertThat(PartnerCredentials.matches(null, null)).isFalse();
    }

    @Test
    @DisplayName("keys are unique across a large batch")
    void keysAreUnique() {
        Set<String> ids = new HashSet<>();
        Set<String> secrets = new HashSet<>();

        IntStream.range(0, 2000).forEach(i -> {
            PartnerCredentials.Generated generated = PartnerCredentials.generate();
            ids.add(generated.keyId());
            secrets.add(generated.secret());
        });

        // A collision on keyId would be a unique-index failure at issue time;
        // a collision on the secret would be worse and silent.
        assertThat(ids).hasSize(2000);
        assertThat(secrets).hasSize(2000);
    }

    @Test
    @DisplayName("the alphabet has no characters that get misread")
    void alphabetIsUnambiguous() {
        // These get read down a phone, retyped from a chat message and pasted
        // into config by hand. A key that fails because someone typed O for 0
        // costs an afternoon and looks like an auth bug.
        String all = IntStream.range(0, 200)
                .mapToObj(i -> PartnerCredentials.generate().presentedKey())
                .reduce("", String::concat)
                .replace("zbrp", "")
                .replace("_", "");

        assertThat(all).doesNotContain("0").doesNotContain("O")
                .doesNotContain("1").doesNotContain("l").doesNotContain("I");
    }
}

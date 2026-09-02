package com.fooddelivery.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These cases are shared with V41__normalize_user_phones.sql, which reimplements
 * this method in PL/pgSQL to clean up existing rows. The two MUST agree: if the
 * migration canonicalises a row differently from how the application would write
 * it, the row stops matching its own owner at login and the duplicate-account
 * bug this was written to fix comes straight back in a new shape.
 *
 * <p>Every input below was run through both implementations.
 */
@DisplayName("PhoneNumbers.normalize")
class PhoneNumbersTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            // The shape the OTP path has always stored — must be a no-op.
            "998901234567,      998901234567",
            // The shape /auth/register used to store raw. This equivalence IS
            // the bug: these two were separate accounts.
            "'+998901234567',   998901234567",
            // What a person actually types or pastes.
            "'+998 90 123 45 67', 998901234567",
            "'+998-90-123-45-67', 998901234567",
            "'998 90 123 45 67',  998901234567",
            // Subscriber number alone.
            "901234567,         998901234567",
            // Local dialling format.
            "8901234567,        998901234567",
    })
    @DisplayName("every shape of one number collapses to the same string")
    void canonicalises(String input, String expected) {
        assertThat(PhoneNumbers.normalize(input)).isEqualTo(expected);
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("null stays null")
        void nullStaysNull() {
            assertThat(PhoneNumbers.normalize(null)).isNull();
        }

        @Test
        @DisplayName("input with no digits is returned unchanged, not emptied")
        void noDigitsReturnedUnchanged() {
            // Returning "" here would silently erase the field. V41 makes the
            // same choice; the two implementations must not disagree.
            assertThat(PhoneNumbers.normalize("not-a-number")).isEqualTo("not-a-number");
            assertThat(PhoneNumbers.normalize("")).isEmpty();
        }

        @Test
        @DisplayName("an email normalises to something that matches no phone")
        void emailDoesNotBecomeAPhone() {
            // AuthService.login passes the raw identifier through this for the
            // phone side of findByEmailOrPhone. It must not accidentally
            // produce a value that matches a real account.
            assertThat(PhoneNumbers.normalize("owner@example.com")).isEqualTo("owner@example.com");
            assertThat(PhoneNumbers.normalize("user1@example.com")).isNotEqualTo("9981");
        }

        @Test
        @DisplayName("a non-Uzbek number is stripped to digits, not rejected")
        void foreignNumber() {
            // A normaliser, not a validator — the DTO @Pattern decides what is
            // acceptable.
            assertThat(PhoneNumbers.normalize("+1 415 555 0100")).isEqualTo("14155550100");
        }

        @Test
        @DisplayName("normalising twice changes nothing")
        void isIdempotent() {
            for (String input : new String[]{
                    "+998901234567", "901234567", "8901234567", "998901234567", "+1 415 555 0100"}) {
                String once = PhoneNumbers.normalize(input);
                assertThat(PhoneNumbers.normalize(once))
                        .as("normalize is idempotent for \"%s\"", input)
                        .isEqualTo(once);
            }
        }
    }
}

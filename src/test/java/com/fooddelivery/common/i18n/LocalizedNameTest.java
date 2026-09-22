package com.fooddelivery.common.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which of the three stored names gets rendered.
 *
 * <p>The app prints what we send, verbatim, with no translation table of its
 * own — so a null here is a chip with no label on it.
 */
@DisplayName("Localized name")
class LocalizedNameTest {

    @Test
    @DisplayName("the requested language wins")
    void requestedLanguage() {
        assertThat(LocalizedName.pick("ru", "Burgerlar", "Бургеры", "Burgers")).isEqualTo("Бургеры");
        assertThat(LocalizedName.pick("en", "Burgerlar", "Бургеры", "Burgers")).isEqualTo("Burgers");
        assertThat(LocalizedName.pick("uz", "Burgerlar", "Бургеры", "Burgers")).isEqualTo("Burgerlar");
    }

    @Test
    @DisplayName("a missing translation falls back to Uzbek rather than to nothing")
    void fallsBackToUzbek() {
        // A category added in a hurry with one name is usable everywhere. The
        // alternative is an unlabelled chip, which is worse than the wrong
        // language.
        assertThat(LocalizedName.pick("ru", "Kabob", null, null)).isEqualTo("Kabob");
        assertThat(LocalizedName.pick("en", "Kabob", "", "  ")).isEqualTo("Kabob");
    }

    @Test
    @DisplayName("with no Uzbek either, anything beats blank")
    void fallsBackAgain() {
        assertThat(LocalizedName.pick("en", null, "Суши", null)).isEqualTo("Суши");
        assertThat(LocalizedName.pick("ru", null, null, "Sushi")).isEqualTo("Sushi");
    }

    @Test
    @DisplayName("an unknown language is treated as the default")
    void unknownLanguage() {
        assertThat(LocalizedName.pick("de", "Kofe", "Кофе", "Coffee")).isEqualTo("Kofe");
        assertThat(LocalizedName.pick(null, "Kofe", "Кофе", "Coffee")).isEqualTo("Kofe");
    }

    @Test
    @DisplayName("nothing stored at all is null, not an empty string")
    void nothingStored() {
        // Null is something a client can test for. "" renders as a chip of
        // zero width, which is a support ticket.
        assertThat(LocalizedName.pick("uz", null, null, null)).isNull();
        assertThat(LocalizedName.pick("uz", " ", "", null)).isNull();
    }
}

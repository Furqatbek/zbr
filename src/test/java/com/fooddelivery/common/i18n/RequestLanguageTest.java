package com.fooddelivery.common.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which language a request is answered in.
 *
 * <p>The customer app sends a bare {@code uz}, {@code ru} or {@code en}, but
 * the header is a standard one and everything else that speaks HTTP sends the
 * full form. Refusing {@code ru-RU} over its region tag would hand a Russian
 * speaker Uzbek and look like a backend that ignores the header.
 */
@DisplayName("Accept-Language")
class RequestLanguageTest {

    @Test
    @DisplayName("the three codes the app sends")
    void bareCodes() {
        assertThat(RequestLanguage.from("uz")).isEqualTo("uz");
        assertThat(RequestLanguage.from("ru")).isEqualTo("ru");
        assertThat(RequestLanguage.from("en")).isEqualTo("en");
    }

    @Test
    @DisplayName("a region or script tag is still that language")
    void regionTagsAreStripped() {
        assertThat(RequestLanguage.from("ru-RU")).isEqualTo("ru");
        assertThat(RequestLanguage.from("en-GB")).isEqualTo("en");
        assertThat(RequestLanguage.from("uz-Latn-UZ")).isEqualTo("uz");
    }

    @Test
    @DisplayName("a weighted list takes the first one we can answer")
    void weightedLists() {
        assertThat(RequestLanguage.from("fr-FR,fr;q=0.9,ru;q=0.8")).isEqualTo("ru");
        assertThat(RequestLanguage.from("en-US,en;q=0.9")).isEqualTo("en");
    }

    @Test
    @DisplayName("case does not matter")
    void caseInsensitive() {
        assertThat(RequestLanguage.from("RU")).isEqualTo("ru");
        assertThat(RequestLanguage.from("En-Gb")).isEqualTo("en");
    }

    @Test
    @DisplayName("nothing asked for, or nothing we speak, is Uzbek")
    void defaults() {
        // The platform serves Uzbekistan, so an unlabelled request is more
        // likely a local customer than an English-speaking one.
        assertThat(RequestLanguage.from(null)).isEqualTo("uz");
        assertThat(RequestLanguage.from("")).isEqualTo("uz");
        assertThat(RequestLanguage.from("  ")).isEqualTo("uz");
        assertThat(RequestLanguage.from("de,fr;q=0.8")).isEqualTo("uz");
    }
}

package com.fooddelivery.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "" is not a value, and storing it as one costs every client a special case.
 *
 * <p>Jackson omits a null field entirely, so "missing" can mean "nothing here".
 * An empty string defeats that: Restos sent {@code imageUrl: ""} for every
 * product on an imported menu, we stored it, and the QR landing page rendered a
 * broken image frame for each of 45 items. The customer app found the same
 * shape on a restaurant's {@code email}.
 */
@DisplayName("Blank to null")
class BlanksTest {

    @Test
    @DisplayName("an empty or whitespace string becomes null")
    void blankBecomesNull() {
        assertThat(Blanks.toNull("")).isNull();
        assertThat(Blanks.toNull("   ")).isNull();
        assertThat(Blanks.toNull("\t\n")).isNull();
    }

    @Test
    @DisplayName("null stays null")
    void nullStaysNull() {
        assertThat(Blanks.toNull(null)).isNull();
    }

    @Test
    @DisplayName("a real value survives, trimmed")
    void realValueIsTrimmed() {
        // Trimmed because " https://… " is the same URL with a bug attached,
        // and a trailing space in a stored URL is found by whoever renders it.
        assertThat(Blanks.toNull("  https://zbrr.uz/a.png  "))
                .isEqualTo("https://zbrr.uz/a.png");
        assertThat(Blanks.toNull("Lavash")).isEqualTo("Lavash");
    }

    @Test
    @DisplayName("a string that is only punctuation is left alone")
    void nonBlankIsNotJudged() {
        // It decides blank versus not, nothing else. Guessing which values are
        // "real" is how a helper starts eating data.
        assertThat(Blanks.toNull("-")).isEqualTo("-");
        assertThat(Blanks.toNull("0")).isEqualTo("0");
    }
}

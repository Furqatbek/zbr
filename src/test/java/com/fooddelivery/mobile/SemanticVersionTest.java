package com.fooddelivery.mobile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Version ordering, written down because two implementations of it exist.
 *
 * <p>The customer app compares versions on the device; this side compares them
 * only to refuse a configuration that would lock everyone out. If the two ever
 * disagree, the disagreement is silent — so the rules the client documents are
 * asserted here one by one.
 */
@DisplayName("Semantic version")
class SemanticVersionTest {

    private static int compare(String a, String b) {
        return SemanticVersion.parse(a).compareTo(SemanticVersion.parse(b));
    }

    @Test
    @DisplayName("compares segment by segment, numerically")
    void numericNotLexical() {
        // The classic bug: as text, "1.10.0" sorts BEFORE "1.9.0", and every
        // customer past version 9 is told they are up to date forever.
        assertThat(compare("1.10.0", "1.9.0")).isPositive();
        assertThat(compare("2.0.0", "1.99.99")).isPositive();
        assertThat(compare("1.4.0", "1.4.0")).isZero();
        assertThat(compare("1.0.0", "1.0.1")).isNegative();
    }

    @Test
    @DisplayName("a missing patch segment is zero")
    void shortFormsAreAccepted() {
        assertThat(compare("1.2", "1.2.0")).isZero();
        assertThat(compare("1.2", "1.2.1")).isNegative();
    }

    @Test
    @DisplayName("a leading v is ignored")
    void vPrefixIsIgnored() {
        assertThat(compare("v1.4.0", "1.4.0")).isZero();
    }

    @Test
    @DisplayName("build metadata does not change the order")
    void buildMetadataIsIgnored() {
        assertThat(compare("1.2.3+456", "1.2.3")).isZero();
        assertThat(compare("1.2.3+999", "1.2.4")).isNegative();
    }

    @Test
    @DisplayName("a prerelease ranks below the same final version")
    void prereleasesAreOlder() {
        assertThat(compare("1.2.3-beta", "1.2.3")).isNegative();
        assertThat(compare("1.2.3-beta", "1.2.2")).isPositive();
    }

    @Test
    @DisplayName("something that is not a version is refused, not guessed at")
    void garbageIsRefused() {
        // Refusing is what turns a typo into a failed deploy rather than a
        // minimum version of 0.0.0 that silently prompts nobody.
        assertThatThrownBy(() -> SemanticVersion.parse("latest"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemanticVersion.parse("1.2.3.4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemanticVersion.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemanticVersion.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

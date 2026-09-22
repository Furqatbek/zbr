package com.fooddelivery.mobile;

/**
 * A version string, comparable the way the customer app compares them.
 *
 * <p>Deliberately lenient in the same places the client is: a leading {@code v}
 * is ignored, a missing patch segment is zero, {@code +build} metadata is
 * discarded, and a {@code -prerelease} suffix ranks below the same final
 * version. Two implementations of the same rules will drift, so the rules are
 * written down here next to the one thing on this side that depends on them.
 *
 * <p>The server does not decide whether a customer must update — the app does
 * that itself from the numbers we return. This exists so a deployment cannot
 * publish a minimum that is newer than anything in the store, which is the one
 * configuration mistake that locks every customer out of the app with no way
 * forward.
 */
record SemanticVersion(int major, int minor, int patch, boolean prerelease)
        implements Comparable<SemanticVersion> {

    /**
     * @throws IllegalArgumentException when the string is not a version at all;
     *         the caller turns that into a refusal to start
     */
    static SemanticVersion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("version is empty");
        }
        String text = raw.trim();
        if (text.startsWith("v") || text.startsWith("V")) {
            text = text.substring(1);
        }

        int plus = text.indexOf('+');
        if (plus >= 0) {
            text = text.substring(0, plus);
        }

        boolean prerelease = false;
        int dash = text.indexOf('-');
        if (dash >= 0) {
            prerelease = true;
            text = text.substring(0, dash);
        }

        String[] parts = text.split("\\.");
        if (parts.length == 0 || parts.length > 3) {
            throw new IllegalArgumentException("'" + raw + "' is not a version like 1.4.0");
        }
        int[] numbers = new int[3];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + raw + "' is not a version like 1.4.0");
            }
            if (numbers[i] < 0) {
                throw new IllegalArgumentException("'" + raw + "' has a negative segment");
            }
        }
        return new SemanticVersion(numbers[0], numbers[1], numbers[2], prerelease);
    }

    @Override
    public int compareTo(SemanticVersion other) {
        if (major != other.major) return Integer.compare(major, other.major);
        if (minor != other.minor) return Integer.compare(minor, other.minor);
        if (patch != other.patch) return Integer.compare(patch, other.patch);
        // Numerically equal: a prerelease is older than the final release.
        if (prerelease == other.prerelease) return 0;
        return prerelease ? -1 : 1;
    }
}

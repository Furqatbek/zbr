package com.fooddelivery.common.util;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for generating URL-friendly slugs and unique codes.
 */
public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-+");

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String NUMERIC = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SlugUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert a string to a URL-friendly slug.
     *
     * @param input The input string
     * @return URL-friendly slug
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);

        // Remove leading and trailing dashes
        slug = slug.replaceAll("^-+|-+$", "");

        return slug;
    }

    /**
     * Generate a unique slug with a random suffix.
     *
     * @param input The base string
     * @return Unique slug with random suffix
     */
    public static String toUniqueSlug(String input) {
        String baseSlug = toSlug(input);
        String suffix = generateRandomCode(6);
        return baseSlug.isEmpty() ? suffix.toLowerCase() : baseSlug + "-" + suffix.toLowerCase();
    }

    /**
     * Generate a random alphanumeric code.
     *
     * @param length The length of the code
     * @return Random alphanumeric code
     */
    public static String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generate a random numeric code.
     *
     * @param length The length of the code
     * @return Random numeric code
     */
    public static String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMERIC.charAt(RANDOM.nextInt(NUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generate a referral code.
     *
     * @return 8-character referral code
     */
    public static String generateReferralCode() {
        return generateRandomCode(8);
    }

    /**
     * Generate an external order number.
     *
     * @return Order number in format: FD-YYYYMMDD-XXXXXX
     */
    public static String generateOrderNumber() {
        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "FD-" + date + "-" + generateRandomCode(6);
    }

    /**
     * Generate a payment reference.
     *
     * @return Payment reference in format: PAY-XXXXXXXXXXXXX
     */
    public static String generatePaymentReference() {
        return "PAY-" + generateRandomCode(13);
    }

    /**
     * Generate a kitchen ticket number.
     *
     * @param restaurantId Restaurant ID for prefix
     * @return Ticket number
     */
    public static String generateTicketNumber(Long restaurantId) {
        return String.format("R%d-%s", restaurantId, generateNumericCode(4));
    }
}

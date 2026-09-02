package com.fooddelivery.common.util;

/**
 * The single definition of what a stored phone number looks like.
 *
 * <p>Canonical form is Uzbek E.164 <b>without</b> the leading plus:
 * {@code 998901234567}. That choice is not up for debate here — it is what the
 * OTP path has always written, so it is what the existing rows contain.
 *
 * <p><b>Why this class exists.</b> The rule used to be copy-pasted into
 * PhoneAuthService and OtpService, and the two paths that write a phone WITHOUT
 * going through either of them — {@code /auth/register} and
 * {@code PUT /users/me} — stored whatever the client sent. So the same person
 * registering with "+998901234567" and later signing in by OTP got two
 * accounts: the OTP lookup normalises to "998901234567", finds nothing, and
 * creates a second user. The UNIQUE constraint on users.phone cannot catch that
 * — the two strings genuinely differ — and neither can an existsByPhone check
 * that compares raw input. Every write path must normalise, or none of them
 * works.
 *
 * <p>Deliberately NOT used by the SMS clients. Those normalise for their
 * provider's wire format, which is a separate concern that may diverge from how
 * we store numbers; conflating them would mean a provider changing its format
 * silently changed our identity keys.
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /**
     * Reduce a phone number to its canonical stored form.
     *
     * <p>Accepts the shapes users actually type — "+998 90 123 45 67",
     * "998901234567", "901234567", "8901234567" — and returns
     * "998901234567" for all of them. Input that does not look like an Uzbek
     * number is stripped to digits and returned as-is rather than rejected:
     * this is a normaliser, not a validator, and the Bean Validation
     * {@code @Pattern} on the request DTOs is what decides acceptability.
     *
     * @return the canonical form, or null if {@code phone} was null
     */
    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }

        // Keep '+' at this stage only so the branches below can read naturally;
        // every return path drops it.
        String digits = phone.replaceAll("[^0-9+]", "");

        // No digits at all: hand the input back rather than the empty string.
        // The old behaviour turned "not-a-number" into "", quietly erasing the
        // field instead of leaving something an operator could recognise. The
        // request DTOs' @Pattern rejects such input before it reaches here, so
        // this is a floor, not a code path anyone should rely on — but V41
        // makes the same choice and the two must not disagree.
        if (digits.replace("+", "").isEmpty()) {
            return phone;
        }

        if (digits.startsWith("+998")) {
            return digits.substring(1);
        } else if (digits.startsWith("998")) {
            return digits;
        } else if (digits.startsWith("8") && digits.length() == 10) {
            // Local dialling format: 8 90 123 45 67
            return "998" + digits.substring(1);
        } else if (digits.length() == 9 && !digits.startsWith("+")) {
            // Subscriber number alone: 90 123 45 67
            return "998" + digits;
        }

        return digits.startsWith("+") ? digits.substring(1) : digits;
    }
}

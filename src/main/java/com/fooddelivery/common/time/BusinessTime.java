package com.fooddelivery.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Calendar arithmetic in the business timezone, returning values in UTC.
 *
 * <p><b>Storage stays UTC and must.</b> The JVM is pinned to UTC
 * ({@code FoodDeliveryApplication}), so is the JDBC connection, and every
 * timestamp leaves the API as UTC with a trailing {@code Z}. That is correct:
 * an instant has no timezone, and moving storage to +05:00 would make every
 * existing row ambiguous and break the moment Uzbekistan ever changed offset.
 *
 * <p>What is NOT correct is deriving a CALENDAR DAY from a UTC clock. A day
 * boundary is a wall-clock question, and midnight in Tashkent is 19:00 UTC the
 * previous day. Computing "today" as {@code LocalDate.now()} on a UTC JVM makes
 * the platform's day run 05:00→05:00 local, so orders placed between midnight
 * and 5am count toward the previous day's totals.
 *
 * <p>Use this for day boundaries and wall-clock decisions. Do NOT use it for
 * timestamps — {@code LocalDateTime.now()} for a {@code createdAt} or an expiry
 * is already right, and changing those would shift stored instants by 5 hours.
 *
 * <p>Every method returning a {@code LocalDateTime} returns it as UTC, ready to
 * compare against UTC-stored columns.
 */
public final class BusinessTime {

    private static volatile ZoneId zone = ZoneId.of("Asia/Tashkent");

    private BusinessTime() {
    }

    static void configure(ZoneId configured) {
        zone = configured;
    }

    public static ZoneId zone() {
        return zone;
    }

    /** Today's date as the business sees it, not as UTC sees it. */
    public static LocalDate today() {
        return LocalDate.now(zone);
    }

    /** Current wall-clock time in the business timezone. */
    public static LocalTime nowLocalTime() {
        return LocalTime.now(zone);
    }

    /**
     * The UTC instant at which the given local date begins.
     * For Tashkent, 2026-08-26 → 2026-08-25T19:00 UTC.
     */
    public static LocalDateTime startOfDay(LocalDate localDate) {
        return localDate.atStartOfDay(zone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    /** The UTC instant at which today began locally. */
    public static LocalDateTime startOfToday() {
        return startOfDay(today());
    }

    /**
     * The UTC instant at which the given local date ends, EXCLUSIVE — i.e. the
     * start of the next day. Prefer {@code >= start && < endExclusive} over
     * anything built on 23:59:59, which drops the final second.
     */
    public static LocalDateTime endOfDayExclusive(LocalDate localDate) {
        return startOfDay(localDate.plusDays(1));
    }

    /**
     * The UTC instant of the last representable moment of the given local date
     * (local 23:59:59.999999999).
     *
     * <p>Provided for callers that already use an INCLUSIVE upper bound, so
     * correcting the timezone does not also silently change their comparison
     * semantics. New code should prefer {@link #endOfDayExclusive(LocalDate)}.
     */
    public static LocalDateTime endOfDayInclusive(LocalDate localDate) {
        return localDate.atTime(LocalTime.MAX)
                .atZone(zone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    /** Interpret a UTC-stored timestamp as a local calendar date. */
    public static LocalDate toLocalDate(LocalDateTime utc) {
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDate();
    }
}

package com.fooddelivery.common.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tashkent is UTC+5 with no DST, so local midnight is 19:00 UTC the previous
 * day. These assertions are the whole point of the class: a day boundary is a
 * wall-clock question, and computing it on a UTC-pinned JVM shifts the
 * platform's day by five hours.
 */
class BusinessTimeTest {

    @Test
    @DisplayName("local midnight is 19:00 UTC the previous day")
    void startOfDayIsFiveHoursEarlierInUtc() {
        LocalDateTime utc = BusinessTime.startOfDay(LocalDate.of(2026, 8, 26));

        assertThat(utc).isEqualTo(LocalDateTime.of(2026, 8, 25, 19, 0));
    }

    @Test
    @DisplayName("the exclusive end of a day is the next day's start")
    void endExclusiveIsNextDayStart() {
        assertThat(BusinessTime.endOfDayExclusive(LocalDate.of(2026, 8, 26)))
                .isEqualTo(BusinessTime.startOfDay(LocalDate.of(2026, 8, 27)))
                .isEqualTo(LocalDateTime.of(2026, 8, 26, 19, 0));
    }

    @Test
    @DisplayName("the inclusive end is just under 24h after the start")
    void endInclusiveStaysWithinTheDay() {
        LocalDateTime start = BusinessTime.startOfDay(LocalDate.of(2026, 8, 26));
        LocalDateTime end = BusinessTime.endOfDayInclusive(LocalDate.of(2026, 8, 26));

        assertThat(end).isAfter(start).isBefore(start.plusDays(1));
    }

    @Test
    @DisplayName("an order at 02:00 local belongs to that local day, not the previous UTC day")
    void lateNightOrderCountsTowardTheLocalDay() {
        // 02:00 on 26 Aug in Tashkent is 21:00 on 25 Aug UTC — the case that
        // made "today's revenue" reset at 5am.
        LocalDateTime storedUtc = LocalDateTime.of(2026, 8, 25, 21, 0);

        assertThat(BusinessTime.toLocalDate(storedUtc)).isEqualTo(LocalDate.of(2026, 8, 26));
        assertThat(storedUtc)
                .isAfterOrEqualTo(BusinessTime.startOfDay(LocalDate.of(2026, 8, 26)))
                .isBefore(BusinessTime.endOfDayExclusive(LocalDate.of(2026, 8, 26)));
    }
}

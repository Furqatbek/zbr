package com.fooddelivery.analytics.fraud.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Utility class for date and time operations in fraud analytics.
 */
public final class FraudDateUtils {

    private FraudDateUtils() {
        // Prevent instantiation
    }

    /**
     * Get the start of the current day.
     */
    public static LocalDateTime getStartOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    /**
     * Get the end of the current day.
     */
    public static LocalDateTime getEndOfToday() {
        return LocalDate.now().atTime(LocalTime.MAX);
    }

    /**
     * Get the start of the current week (Monday).
     */
    public static LocalDateTime getStartOfCurrentWeek() {
        return LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
    }

    /**
     * Get the end of the current week (Sunday).
     */
    public static LocalDateTime getEndOfCurrentWeek() {
        return LocalDate.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                .atTime(LocalTime.MAX);
    }

    /**
     * Get the start of the current month.
     */
    public static LocalDateTime getStartOfCurrentMonth() {
        return LocalDate.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
    }

    /**
     * Get the end of the current month.
     */
    public static LocalDateTime getEndOfCurrentMonth() {
        return LocalDate.now()
                .with(TemporalAdjusters.lastDayOfMonth())
                .atTime(LocalTime.MAX);
    }

    /**
     * Get date range for last N days.
     *
     * @param days Number of days to go back
     * @return Array with [startDate, endDate]
     */
    public static LocalDateTime[] getLastNDays(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        return new LocalDateTime[]{startDate, endDate};
    }

    /**
     * Get date range for last N hours.
     *
     * @param hours Number of hours to go back
     * @return Array with [startDate, endDate]
     */
    public static LocalDateTime[] getLastNHours(int hours) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusHours(hours);
        return new LocalDateTime[]{startDate, endDate};
    }

    /**
     * Get date range for last N minutes.
     *
     * @param minutes Number of minutes to go back
     * @return Array with [startDate, endDate]
     */
    public static LocalDateTime[] getLastNMinutes(int minutes) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMinutes(minutes);
        return new LocalDateTime[]{startDate, endDate};
    }

    /**
     * Calculate hours between two dates.
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * Calculate days between two dates.
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Calculate minutes between two dates.
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * Check if a date is within the last N hours.
     */
    public static boolean isWithinLastHours(LocalDateTime date, int hours) {
        return date.isAfter(LocalDateTime.now().minusHours(hours));
    }

    /**
     * Check if a date is within the last N days.
     */
    public static boolean isWithinLastDays(LocalDateTime date, int days) {
        return date.isAfter(LocalDateTime.now().minusDays(days));
    }

    /**
     * Get the hour of day (0-23) from a LocalDateTime.
     */
    public static int getHourOfDay(LocalDateTime dateTime) {
        return dateTime.getHour();
    }

    /**
     * Get the day of week (1-7, Monday=1) from a LocalDateTime.
     */
    public static int getDayOfWeek(LocalDateTime dateTime) {
        return dateTime.getDayOfWeek().getValue();
    }

    /**
     * Check if the time is during "high risk" hours (typically late night/early morning).
     * Hours 0-5 and 23 are considered high risk for fraud.
     */
    public static boolean isHighRiskHour(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        return hour >= 0 && hour <= 5 || hour == 23;
    }

    /**
     * Check if the date is a weekend.
     */
    public static boolean isWeekend(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Get a description of the time period between dates.
     */
    public static String getPeriodDescription(LocalDateTime start, LocalDateTime end) {
        long days = daysBetween(start, end);
        if (days == 0) {
            long hours = hoursBetween(start, end);
            return hours + " hour" + (hours != 1 ? "s" : "");
        } else if (days < 7) {
            return days + " day" + (days != 1 ? "s" : "");
        } else if (days < 30) {
            long weeks = days / 7;
            return weeks + " week" + (weeks != 1 ? "s" : "");
        } else {
            long months = days / 30;
            return months + " month" + (months != 1 ? "s" : "");
        }
    }

    /**
     * Round a datetime down to the nearest hour.
     */
    public static LocalDateTime truncateToHour(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.HOURS);
    }

    /**
     * Round a datetime down to the nearest day.
     */
    public static LocalDateTime truncateToDay(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * Get the default analysis period (last 24 hours).
     */
    public static LocalDateTime[] getDefaultAnalysisPeriod() {
        return getLastNHours(24);
    }

    /**
     * Validate that start date is before end date.
     */
    public static boolean isValidDateRange(LocalDateTime start, LocalDateTime end) {
        return start != null && end != null && start.isBefore(end);
    }

    /**
     * Ensure the date range doesn't exceed the maximum allowed period.
     *
     * @param start Start date
     * @param end End date
     * @param maxDays Maximum allowed days
     * @return Adjusted start date if period exceeds max, otherwise original
     */
    public static LocalDateTime adjustStartForMaxPeriod(LocalDateTime start, LocalDateTime end, int maxDays) {
        if (daysBetween(start, end) > maxDays) {
            return end.minusDays(maxDays);
        }
        return start;
    }
}

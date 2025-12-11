package com.fooddelivery.notification.util;

import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for notification time-related operations.
 */
@UtilityClass
public class NotificationTimeUtils {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    /**
     * Get human-readable time ago string.
     */
    public static String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown";
        }

        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();

        if (seconds < 0) {
            return "Just now";
        }

        if (seconds < 60) {
            return "Just now";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }

        long days = hours / 24;
        if (days < 7) {
            return days == 1 ? "Yesterday" : days + " days ago";
        }

        long weeks = days / 7;
        if (weeks < 4) {
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        }

        long months = days / 30;
        if (months < 12) {
            return months == 1 ? "1 month ago" : months + " months ago";
        }

        long years = days / 365;
        return years == 1 ? "1 year ago" : years + " years ago";
    }

    /**
     * Format date for display.
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMAT);
    }

    /**
     * Format time for display.
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIME_FORMAT);
    }

    /**
     * Format datetime for display.
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATETIME_FORMAT);
    }

    /**
     * Check if the datetime is today.
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return dateTime.toLocalDate().equals(now.toLocalDate());
    }

    /**
     * Check if the datetime is within the last hour.
     */
    public static boolean isWithinLastHour(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return Duration.between(dateTime, LocalDateTime.now()).toHours() < 1;
    }

    /**
     * Check if the datetime is within the last N hours.
     */
    public static boolean isWithinLastHours(LocalDateTime dateTime, int hours) {
        if (dateTime == null) {
            return false;
        }
        return Duration.between(dateTime, LocalDateTime.now()).toHours() < hours;
    }

    /**
     * Get friendly display string for notification time.
     */
    public static String getFriendlyTimeDisplay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        if (isWithinLastHour(dateTime)) {
            return getTimeAgo(dateTime);
        }

        if (isToday(dateTime)) {
            return "Today at " + formatTime(dateTime);
        }

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        if (dateTime.toLocalDate().equals(yesterday.toLocalDate())) {
            return "Yesterday at " + formatTime(dateTime);
        }

        return formatDateTime(dateTime);
    }
}

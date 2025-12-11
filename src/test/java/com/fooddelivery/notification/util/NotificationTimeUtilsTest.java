package com.fooddelivery.notification.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationTimeUtils Unit Tests")
class NotificationTimeUtilsTest {

    @Nested
    @DisplayName("getTimeAgo Tests")
    class GetTimeAgoTests {

        @Test
        @DisplayName("Should return 'Just now' for time within last minute")
        void shouldReturnJustNowForTimeWithinLastMinute() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();

            // Act
            String result = NotificationTimeUtils.getTimeAgo(now);

            // Assert
            assertThat(result).isEqualTo("Just now");
        }

        @Test
        @DisplayName("Should return 'Just now' for time 30 seconds ago")
        void shouldReturnJustNowForTime30SecondsAgo() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusSeconds(30);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("Just now");
        }

        @Test
        @DisplayName("Should return '1 minute ago' for time 1 minute ago")
        void shouldReturn1MinuteAgoForTime1MinuteAgo() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusMinutes(1);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("1 minute ago");
        }

        @Test
        @DisplayName("Should return 'X minutes ago' for time within last hour")
        void shouldReturnMinutesAgoForTimeWithinLastHour() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusMinutes(45);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("45 minutes ago");
        }

        @Test
        @DisplayName("Should return '1 hour ago' for time 1 hour ago")
        void shouldReturn1HourAgoForTime1HourAgo() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusHours(1);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("1 hour ago");
        }

        @Test
        @DisplayName("Should return 'X hours ago' for time within last day")
        void shouldReturnHoursAgoForTimeWithinLastDay() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusHours(5);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("5 hours ago");
        }

        @Test
        @DisplayName("Should return 'Yesterday' for time 1 day ago")
        void shouldReturnYesterdayForTime1DayAgo() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusDays(1);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("Yesterday");
        }

        @Test
        @DisplayName("Should return 'X days ago' for time within last week")
        void shouldReturnDaysAgoForTimeWithinLastWeek() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusDays(5);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("5 days ago");
        }

        @Test
        @DisplayName("Should return '1 week ago' for time 1 week ago")
        void shouldReturn1WeekAgoForTime1WeekAgo() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusWeeks(1);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("1 week ago");
        }

        @Test
        @DisplayName("Should return 'X weeks ago' for time within last month")
        void shouldReturnWeeksAgoForTimeWithinLastMonth() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusWeeks(3);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("3 weeks ago");
        }

        @Test
        @DisplayName("Should return '1 month ago' for time 1 month ago")
        void shouldReturn1MonthAgoForTime1MonthAgo() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusDays(35);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("1 month ago");
        }

        @Test
        @DisplayName("Should return 'X months ago' for time within last year")
        void shouldReturnMonthsAgoForTimeWithinLastYear() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusMonths(6);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("6 months ago");
        }

        @Test
        @DisplayName("Should return '1 year ago' for time 1 year ago")
        void shouldReturn1YearAgoForTime1YearAgo() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusYears(1);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("1 year ago");
        }

        @Test
        @DisplayName("Should return 'X years ago' for old time")
        void shouldReturnYearsAgoForOldTime() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusYears(3);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(time);

            // Assert
            assertThat(result).isEqualTo("3 years ago");
        }

        @Test
        @DisplayName("Should return 'Unknown' for null input")
        void shouldReturnUnknownForNullInput() {
            // Act
            String result = NotificationTimeUtils.getTimeAgo(null);

            // Assert
            assertThat(result).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("Should return 'Just now' for future time")
        void shouldReturnJustNowForFutureTime() {
            // Arrange
            LocalDateTime futureTime = LocalDateTime.now().plusMinutes(5);

            // Act
            String result = NotificationTimeUtils.getTimeAgo(futureTime);

            // Assert
            assertThat(result).isEqualTo("Just now");
        }
    }

    @Nested
    @DisplayName("formatDate Tests")
    class FormatDateTests {

        @Test
        @DisplayName("Should format date correctly")
        void shouldFormatDateCorrectly() {
            // Arrange
            LocalDateTime date = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

            // Act
            String result = NotificationTimeUtils.formatDate(date);

            // Assert
            assertThat(result).isEqualTo("Jan 15, 2024");
        }

        @Test
        @DisplayName("Should return empty string for null input")
        void shouldReturnEmptyStringForNullInput() {
            // Act
            String result = NotificationTimeUtils.formatDate(null);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("formatTime Tests")
    class FormatTimeTests {

        @Test
        @DisplayName("Should format time correctly (AM)")
        void shouldFormatTimeCorrectlyAM() {
            // Arrange
            LocalDateTime time = LocalDateTime.of(2024, 1, 15, 9, 30, 0);

            // Act
            String result = NotificationTimeUtils.formatTime(time);

            // Assert
            assertThat(result).isEqualTo("9:30 AM");
        }

        @Test
        @DisplayName("Should format time correctly (PM)")
        void shouldFormatTimeCorrectlyPM() {
            // Arrange
            LocalDateTime time = LocalDateTime.of(2024, 1, 15, 14, 45, 0);

            // Act
            String result = NotificationTimeUtils.formatTime(time);

            // Assert
            assertThat(result).isEqualTo("2:45 PM");
        }

        @Test
        @DisplayName("Should return empty string for null input")
        void shouldReturnEmptyStringForNullInput() {
            // Act
            String result = NotificationTimeUtils.formatTime(null);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("formatDateTime Tests")
    class FormatDateTimeTests {

        @Test
        @DisplayName("Should format datetime correctly")
        void shouldFormatDateTimeCorrectly() {
            // Arrange
            LocalDateTime dateTime = LocalDateTime.of(2024, 3, 20, 15, 45, 0);

            // Act
            String result = NotificationTimeUtils.formatDateTime(dateTime);

            // Assert
            assertThat(result).isEqualTo("Mar 20, 2024 3:45 PM");
        }

        @Test
        @DisplayName("Should return empty string for null input")
        void shouldReturnEmptyStringForNullInput() {
            // Act
            String result = NotificationTimeUtils.formatDateTime(null);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isToday Tests")
    class IsTodayTests {

        @Test
        @DisplayName("Should return true for today's date")
        void shouldReturnTrueForTodaysDate() {
            // Arrange
            LocalDateTime today = LocalDateTime.now();

            // Act
            boolean result = NotificationTimeUtils.isToday(today);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for yesterday's date")
        void shouldReturnFalseForYesterdaysDate() {
            // Arrange
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            // Act
            boolean result = NotificationTimeUtils.isToday(yesterday);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null input")
        void shouldReturnFalseForNullInput() {
            // Act
            boolean result = NotificationTimeUtils.isToday(null);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isWithinLastHour Tests")
    class IsWithinLastHourTests {

        @Test
        @DisplayName("Should return true for time 30 minutes ago")
        void shouldReturnTrueForTime30MinutesAgo() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusMinutes(30);

            // Act
            boolean result = NotificationTimeUtils.isWithinLastHour(time);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for time 2 hours ago")
        void shouldReturnFalseForTime2HoursAgo() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusHours(2);

            // Act
            boolean result = NotificationTimeUtils.isWithinLastHour(time);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null input")
        void shouldReturnFalseForNullInput() {
            // Act
            boolean result = NotificationTimeUtils.isWithinLastHour(null);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isWithinLastHours Tests")
    class IsWithinLastHoursTests {

        @Test
        @DisplayName("Should return true for time within specified hours")
        void shouldReturnTrueForTimeWithinSpecifiedHours() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusHours(3);

            // Act
            boolean result = NotificationTimeUtils.isWithinLastHours(time, 5);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for time outside specified hours")
        void shouldReturnFalseForTimeOutsideSpecifiedHours() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusHours(10);

            // Act
            boolean result = NotificationTimeUtils.isWithinLastHours(time, 5);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null input")
        void shouldReturnFalseForNullInput() {
            // Act
            boolean result = NotificationTimeUtils.isWithinLastHours(null, 5);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getFriendlyTimeDisplay Tests")
    class GetFriendlyTimeDisplayTests {

        @Test
        @DisplayName("Should return time ago for recent time")
        void shouldReturnTimeAgoForRecentTime() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusMinutes(30);

            // Act
            String result = NotificationTimeUtils.getFriendlyTimeDisplay(time);

            // Assert
            assertThat(result).isEqualTo("30 minutes ago");
        }

        @Test
        @DisplayName("Should return 'Today at' for time earlier today")
        void shouldReturnTodayAtForTimeEarlierToday() {
            // Arrange
            LocalDateTime time = LocalDateTime.now().minusHours(5);

            // Act
            String result = NotificationTimeUtils.getFriendlyTimeDisplay(time);

            // Assert
            assertThat(result).startsWith("Today at ");
        }

        @Test
        @DisplayName("Should return 'Yesterday at' for yesterday's time")
        void shouldReturnYesterdayAtForYesterdaysTime() {
            // Arrange
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1).withHour(14).withMinute(30);

            // Act
            String result = NotificationTimeUtils.getFriendlyTimeDisplay(yesterday);

            // Assert
            assertThat(result).startsWith("Yesterday at ");
        }

        @Test
        @DisplayName("Should return formatted datetime for older time")
        void shouldReturnFormattedDatetimeForOlderTime() {
            // Arrange
            LocalDateTime oldTime = LocalDateTime.now().minusDays(5);

            // Act
            String result = NotificationTimeUtils.getFriendlyTimeDisplay(oldTime);

            // Assert
            assertThat(result).containsPattern("\\w{3} \\d{1,2}, \\d{4}");
        }

        @Test
        @DisplayName("Should return empty string for null input")
        void shouldReturnEmptyStringForNullInput() {
            // Act
            String result = NotificationTimeUtils.getFriendlyTimeDisplay(null);

            // Assert
            assertThat(result).isEmpty();
        }
    }
}

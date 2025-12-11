package com.fooddelivery.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO containing user activation metrics.
 *
 * <p>Activation Metrics:</p>
 * <ul>
 *   <li><b>First Delivery Count</b>: Users who completed their first delivery</li>
 *   <li><b>Activation Time</b>: Time from registration to first order</li>
 *   <li><b>Referral Usage Rate</b>: Users who signed up with referral / total new users</li>
 * </ul>
 *
 * <p>Activation Formula:</p>
 * <pre>
 * Activation Time = First Order Timestamp - User Registration Timestamp
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationMetricsDto {

    /**
     * Users who completed their first delivery today.
     */
    private Long firstDeliveryCompletedToday;

    /**
     * Users who completed their first delivery this week.
     */
    private Long firstDeliveryCompletedWeek;

    /**
     * Users who completed their first delivery this month.
     */
    private Long firstDeliveryCompletedMonth;

    /**
     * New users registered today.
     */
    private Long newRegistrationsToday;

    /**
     * New users registered this week.
     */
    private Long newRegistrationsWeek;

    /**
     * New users registered this month.
     */
    private Long newRegistrationsMonth;

    /**
     * Activation rate today (first delivery / new registrations).
     */
    private Double activationRateToday;

    /**
     * Activation rate this week.
     */
    private Double activationRateWeek;

    /**
     * Activation rate this month.
     */
    private Double activationRateMonth;

    /**
     * Average time from registration to first order (in hours).
     */
    private Double averageActivationTimeHours;

    /**
     * Median time from registration to first order (in hours).
     */
    private Double medianActivationTimeHours;

    /**
     * Minimum activation time observed (in hours).
     */
    private Double minActivationTimeHours;

    /**
     * Maximum activation time observed (in hours).
     */
    private Double maxActivationTimeHours;

    /**
     * Users who signed up with a referral code today.
     */
    private Long referralSignupsToday;

    /**
     * Users who signed up with a referral code this week.
     */
    private Long referralSignupsWeek;

    /**
     * Users who signed up with a referral code this month.
     */
    private Long referralSignupsMonth;

    /**
     * Referral usage rate (referral signups / total new users) today.
     */
    private Double referralUsageRateToday;

    /**
     * Referral usage rate this week.
     */
    private Double referralUsageRateWeek;

    /**
     * Referral usage rate this month.
     */
    private Double referralUsageRateMonth;

    /**
     * Distribution of activation times (time to first order).
     */
    private List<ActivationTimeBucket> activationTimeDistribution;

    /**
     * Daily activation trend for the past 7 days.
     */
    private List<DailyActivation> dailyTrend;

    /**
     * Reference date for the metrics.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate referenceDate;

    /**
     * Timestamp when this metric was calculated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime calculatedAt;

    /**
     * Calculate activation rates.
     */
    public void calculateRates() {
        if (newRegistrationsToday != null && newRegistrationsToday > 0) {
            this.activationRateToday = firstDeliveryCompletedToday != null ?
                    Math.round((double) firstDeliveryCompletedToday / newRegistrationsToday * 10000) / 100.0 : 0.0;
            this.referralUsageRateToday = referralSignupsToday != null ?
                    Math.round((double) referralSignupsToday / newRegistrationsToday * 10000) / 100.0 : 0.0;
        }
        if (newRegistrationsWeek != null && newRegistrationsWeek > 0) {
            this.activationRateWeek = firstDeliveryCompletedWeek != null ?
                    Math.round((double) firstDeliveryCompletedWeek / newRegistrationsWeek * 10000) / 100.0 : 0.0;
            this.referralUsageRateWeek = referralSignupsWeek != null ?
                    Math.round((double) referralSignupsWeek / newRegistrationsWeek * 10000) / 100.0 : 0.0;
        }
        if (newRegistrationsMonth != null && newRegistrationsMonth > 0) {
            this.activationRateMonth = firstDeliveryCompletedMonth != null ?
                    Math.round((double) firstDeliveryCompletedMonth / newRegistrationsMonth * 10000) / 100.0 : 0.0;
            this.referralUsageRateMonth = referralSignupsMonth != null ?
                    Math.round((double) referralSignupsMonth / newRegistrationsMonth * 10000) / 100.0 : 0.0;
        }
    }

    /**
     * Inner class for activation time distribution buckets.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivationTimeBucket {
        private String bucket; // e.g., "< 1 hour", "1-24 hours", "1-7 days", "> 7 days"
        private Long userCount;
        private Double percentage;
    }

    /**
     * Inner class for daily activation trend.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivation {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Long newRegistrations;
        private Long firstDeliveries;
        private Double activationRate;
    }
}

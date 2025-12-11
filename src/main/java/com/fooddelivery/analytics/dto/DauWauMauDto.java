package com.fooddelivery.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO containing Daily, Weekly, and Monthly Active User metrics.
 *
 * <p>Definitions:</p>
 * <ul>
 *   <li><b>DAU (Daily Active Users)</b>: Distinct users who performed any action today</li>
 *   <li><b>WAU (Weekly Active Users)</b>: Distinct users in the past 7 days</li>
 *   <li><b>MAU (Monthly Active Users)</b>: Distinct users in the past 30 days</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DauWauMauDto {

    /**
     * Daily Active Users count.
     */
    private Long dailyActiveUsers;

    /**
     * Weekly Active Users count (past 7 days).
     */
    private Long weeklyActiveUsers;

    /**
     * Monthly Active Users count (past 30 days).
     */
    private Long monthlyActiveUsers;

    /**
     * DAU/MAU ratio - a measure of user stickiness.
     * Higher ratio indicates more engaged user base.
     * Typical healthy range: 0.1 to 0.3 (10-30%).
     */
    private Double dauMauRatio;

    /**
     * DAU/WAU ratio.
     */
    private Double dauWauRatio;

    /**
     * WAU/MAU ratio.
     */
    private Double wauMauRatio;

    /**
     * The date for which DAU is calculated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate referenceDate;

    /**
     * New users registered today.
     */
    private Long newUsersToday;

    /**
     * New users registered this week.
     */
    private Long newUsersThisWeek;

    /**
     * New users registered this month.
     */
    private Long newUsersThisMonth;

    /**
     * Timestamp when this metric was calculated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime calculatedAt;

    /**
     * Calculate and set all ratio metrics.
     */
    public void calculateRatios() {
        if (monthlyActiveUsers != null && monthlyActiveUsers > 0) {
            this.dauMauRatio = dailyActiveUsers != null ?
                    Math.round((double) dailyActiveUsers / monthlyActiveUsers * 10000) / 10000.0 : 0.0;
            this.wauMauRatio = weeklyActiveUsers != null ?
                    Math.round((double) weeklyActiveUsers / monthlyActiveUsers * 10000) / 10000.0 : 0.0;
        }
        if (weeklyActiveUsers != null && weeklyActiveUsers > 0) {
            this.dauWauRatio = dailyActiveUsers != null ?
                    Math.round((double) dailyActiveUsers / weeklyActiveUsers * 10000) / 10000.0 : 0.0;
        }
    }
}

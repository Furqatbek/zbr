package com.fooddelivery.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO containing churn metrics for users, restaurants, and couriers.
 *
 * <p>Churn Definitions:</p>
 * <ul>
 *   <li><b>User Churn</b>: Users not ordering again within 30 days</li>
 *   <li><b>Restaurant Churn</b>: Restaurants with zero orders for last 30 days</li>
 *   <li><b>Courier Churn</b>: Couriers inactive for last 14 days</li>
 * </ul>
 *
 * <p>Churn Rate Formula:</p>
 * <pre>
 * User Churn Rate = (Users who haven't ordered in 30 days / Total active users) * 100
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChurnDto {

    // ============ User Churn Metrics ============

    /**
     * Total active users (ordered at least once ever).
     */
    private Long totalActiveUsers;

    /**
     * Users who ordered in the last 30 days.
     */
    private Long usersOrderedLast30Days;

    /**
     * Users who haven't ordered in the last 30 days.
     */
    private Long usersChurnedLast30Days;

    /**
     * User churn rate (percentage).
     */
    private Double userChurnRate;

    /**
     * Users at risk of churning (last order 15-30 days ago).
     */
    private Long usersAtRiskOfChurn;

    /**
     * Average days since last order for churned users.
     */
    private Double averageDaysSinceLastOrderChurned;

    // ============ Restaurant Churn Metrics ============

    /**
     * Total active restaurants (status = ACTIVE).
     */
    private Long totalActiveRestaurants;

    /**
     * Restaurants with orders in the last 30 days.
     */
    private Long restaurantsWithOrdersLast30Days;

    /**
     * Restaurants with zero orders in the last 30 days.
     */
    private Long restaurantsChurnedLast30Days;

    /**
     * Restaurant churn rate (percentage).
     */
    private Double restaurantChurnRate;

    /**
     * Restaurants at risk (low orders in last 30 days).
     */
    private Long restaurantsAtRisk;

    // ============ Courier Churn Metrics ============

    /**
     * Total verified couriers.
     */
    private Long totalVerifiedCouriers;

    /**
     * Couriers active in the last 14 days.
     */
    private Long couriersActiveLast14Days;

    /**
     * Couriers inactive for last 14 days.
     */
    private Long couriersChurnedLast14Days;

    /**
     * Courier churn rate (percentage).
     */
    private Double courierChurnRate;

    /**
     * Couriers at risk (inactive 7-14 days).
     */
    private Long couriersAtRisk;

    // ============ Retention Cohort Analysis ============

    /**
     * User retention by cohort (signup month).
     */
    private List<RetentionCohort> userRetentionCohorts;

    /**
     * Restaurant retention by cohort.
     */
    private List<RetentionCohort> restaurantRetentionCohorts;

    // ============ Churn Reasons Analysis ============

    /**
     * Common reasons for user churn (if tracked).
     */
    private List<ChurnReason> userChurnReasons;

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
     * Calculate churn rates.
     */
    public void calculateRates() {
        // User churn rate
        if (totalActiveUsers != null && totalActiveUsers > 0) {
            this.userChurnRate = usersChurnedLast30Days != null ?
                    Math.round((double) usersChurnedLast30Days / totalActiveUsers * 10000) / 100.0 : 0.0;
        }

        // Restaurant churn rate
        if (totalActiveRestaurants != null && totalActiveRestaurants > 0) {
            this.restaurantChurnRate = restaurantsChurnedLast30Days != null ?
                    Math.round((double) restaurantsChurnedLast30Days / totalActiveRestaurants * 10000) / 100.0 : 0.0;
        }

        // Courier churn rate
        if (totalVerifiedCouriers != null && totalVerifiedCouriers > 0) {
            this.courierChurnRate = couriersChurnedLast14Days != null ?
                    Math.round((double) couriersChurnedLast14Days / totalVerifiedCouriers * 10000) / 100.0 : 0.0;
        }
    }

    /**
     * Inner class for retention cohort analysis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetentionCohort {
        @JsonFormat(pattern = "yyyy-MM")
        private String cohortMonth;
        private Long cohortSize;
        private Long stillActiveAfter30Days;
        private Long stillActiveAfter60Days;
        private Long stillActiveAfter90Days;
        private Double retentionRate30Days;
        private Double retentionRate60Days;
        private Double retentionRate90Days;
    }

    /**
     * Inner class for churn reasons.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChurnReason {
        private String reason;
        private Long count;
        private Double percentage;
    }
}

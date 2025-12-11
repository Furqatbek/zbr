package com.fooddelivery.admin.dashboard.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Utility class for resolving dashboard status and health indicators.
 */
@UtilityClass
public class DashboardStatusResolver {

    /**
     * System status levels.
     */
    public enum SystemStatus {
        HEALTHY("All systems operational"),
        DEGRADED("Some systems experiencing issues"),
        CRITICAL("Major system issues detected"),
        UNKNOWN("Unable to determine system status");

        private final String description;

        SystemStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Order status for dashboard display.
     */
    public enum OrderDisplayStatus {
        PENDING("Awaiting restaurant confirmation"),
        ACCEPTED("Confirmed by restaurant"),
        PREPARING("Being prepared"),
        READY("Ready for pickup"),
        PICKED_UP("Picked up by courier"),
        IN_TRANSIT("On the way to customer"),
        DELIVERED("Successfully delivered"),
        CANCELLED("Order cancelled"),
        REJECTED("Rejected by restaurant"),
        STUCK("Delayed beyond threshold");

        private final String description;

        OrderDisplayStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Priority levels for dashboard items.
     */
    public enum PriorityLevel {
        CRITICAL(1, "Requires immediate attention"),
        HIGH(2, "Should be addressed soon"),
        MEDIUM(3, "Normal priority"),
        LOW(4, "Can be addressed later");

        private final int weight;
        private final String description;

        PriorityLevel(int weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public int getWeight() {
            return weight;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Determine overall system status based on health metrics.
     *
     * @param apiLatencyMs API latency in milliseconds
     * @param dbLoadPercent database load percentage
     * @param redisLatencyMs Redis latency in milliseconds
     * @param errorRate error rate percentage
     * @return system status
     */
    public static SystemStatus determineSystemStatus(Double apiLatencyMs, Double dbLoadPercent,
                                                      Double redisLatencyMs, Double errorRate) {
        // Critical thresholds
        if (apiLatencyMs != null && apiLatencyMs > 2000) return SystemStatus.CRITICAL;
        if (dbLoadPercent != null && dbLoadPercent > 90) return SystemStatus.CRITICAL;
        if (redisLatencyMs != null && redisLatencyMs > 100) return SystemStatus.CRITICAL;
        if (errorRate != null && errorRate > 5) return SystemStatus.CRITICAL;

        // Degraded thresholds
        if (apiLatencyMs != null && apiLatencyMs > 1000) return SystemStatus.DEGRADED;
        if (dbLoadPercent != null && dbLoadPercent > 70) return SystemStatus.DEGRADED;
        if (redisLatencyMs != null && redisLatencyMs > 50) return SystemStatus.DEGRADED;
        if (errorRate != null && errorRate > 1) return SystemStatus.DEGRADED;

        // Check if we have any data
        if (apiLatencyMs == null && dbLoadPercent == null &&
            redisLatencyMs == null && errorRate == null) {
            return SystemStatus.UNKNOWN;
        }

        return SystemStatus.HEALTHY;
    }

    /**
     * Determine if an order is stuck based on its current stage and time.
     *
     * @param orderStatus current order status
     * @param statusChangedAt when the status last changed
     * @param thresholds map of status to threshold minutes
     * @return true if the order is stuck
     */
    public static boolean isOrderStuck(String orderStatus, LocalDateTime statusChangedAt,
                                        Map<String, Integer> thresholds) {
        if (orderStatus == null || statusChangedAt == null || thresholds == null) {
            return false;
        }

        Integer threshold = thresholds.get(orderStatus.toUpperCase());
        if (threshold == null) {
            return false;
        }

        long minutesInStatus = ChronoUnit.MINUTES.between(statusChangedAt, LocalDateTime.now());
        return minutesInStatus > threshold;
    }

    /**
     * Calculate priority score for a stuck order.
     * Higher score = higher priority.
     *
     * @param minutesStuck how long the order has been stuck
     * @param thresholdMinutes the threshold for this status
     * @param orderValue the order value (optional)
     * @return priority score (0-100)
     */
    public static Double calculateStuckOrderPriority(Long minutesStuck, Integer thresholdMinutes,
                                                      Double orderValue) {
        if (minutesStuck == null || thresholdMinutes == null || thresholdMinutes == 0) {
            return 0.0;
        }

        // Base priority from time ratio (up to 70 points)
        double timeRatio = Math.min(2.0, (double) minutesStuck / thresholdMinutes);
        double baseScore = timeRatio * 35;

        // Additional points for extreme delays (up to 30 points)
        double delayMultiplier = minutesStuck > thresholdMinutes * 2 ? 30 :
                minutesStuck > thresholdMinutes * 1.5 ? 20 :
                        minutesStuck > thresholdMinutes ? 10 : 0;

        // Order value bonus (up to 10 points for high-value orders)
        double valueBonus = 0;
        if (orderValue != null && orderValue > 100) {
            valueBonus = Math.min(10, (orderValue - 100) / 50);
        }

        return Math.min(100, baseScore + delayMultiplier + valueBonus);
    }

    /**
     * Determine priority level from priority score.
     *
     * @param priorityScore the priority score (0-100)
     * @return priority level
     */
    public static PriorityLevel determinePriorityLevel(Double priorityScore) {
        if (priorityScore == null) {
            return PriorityLevel.LOW;
        }
        if (priorityScore >= 80) return PriorityLevel.CRITICAL;
        if (priorityScore >= 60) return PriorityLevel.HIGH;
        if (priorityScore >= 30) return PriorityLevel.MEDIUM;
        return PriorityLevel.LOW;
    }

    /**
     * Determine restaurant display status.
     *
     * @param isOnline whether restaurant is online
     * @param isAcceptingOrders whether accepting orders
     * @param isBusy whether currently busy
     * @param isTemporarilyClosed whether temporarily closed
     * @return display status string
     */
    public static String determineRestaurantStatus(Boolean isOnline, Boolean isAcceptingOrders,
                                                    Boolean isBusy, Boolean isTemporarilyClosed) {
        if (isTemporarilyClosed != null && isTemporarilyClosed) {
            return "TEMPORARILY_CLOSED";
        }
        if (isOnline == null || !isOnline) {
            return "OFFLINE";
        }
        if (isBusy != null && isBusy) {
            return "BUSY";
        }
        if (isAcceptingOrders == null || !isAcceptingOrders) {
            return "PAUSED";
        }
        return "ONLINE";
    }

    /**
     * Determine courier display status.
     *
     * @param isActive whether courier is active (recent ping)
     * @param isOnDelivery whether currently on delivery
     * @param isOnBreak whether on break
     * @param isReturning whether returning from delivery
     * @return display status string
     */
    public static String determineCourierStatus(Boolean isActive, Boolean isOnDelivery,
                                                 Boolean isOnBreak, Boolean isReturning) {
        if (isActive == null || !isActive) {
            return "OFFLINE";
        }
        if (isOnBreak != null && isOnBreak) {
            return "ON_BREAK";
        }
        if (isOnDelivery != null && isOnDelivery) {
            return "ON_DELIVERY";
        }
        if (isReturning != null && isReturning) {
            return "RETURNING";
        }
        return "AVAILABLE";
    }

    /**
     * Determine ticket urgency level based on priority and age.
     *
     * @param priority ticket priority
     * @param createdAt when ticket was created
     * @param firstResponseAt when first response was made (null if none)
     * @return urgency level string
     */
    public static String determineTicketUrgency(String priority, LocalDateTime createdAt,
                                                 LocalDateTime firstResponseAt) {
        if (createdAt == null) {
            return "NORMAL";
        }

        long ageMinutes = ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
        boolean hasResponse = firstResponseAt != null;

        // SLA thresholds (minutes) by priority
        int slaThreshold = switch (priority != null ? priority.toUpperCase() : "MEDIUM") {
            case "CRITICAL" -> 15;
            case "HIGH" -> 30;
            case "MEDIUM" -> 60;
            case "LOW" -> 120;
            default -> 60;
        };

        if (!hasResponse && ageMinutes > slaThreshold * 2) {
            return "CRITICAL";
        }
        if (!hasResponse && ageMinutes > slaThreshold) {
            return "HIGH";
        }
        if (!hasResponse && ageMinutes > slaThreshold / 2) {
            return "ELEVATED";
        }
        return "NORMAL";
    }

    /**
     * Get suggested action for a stuck order.
     *
     * @param orderStatus current order status
     * @param minutesStuck minutes stuck in current status
     * @return suggested action string
     */
    public static String getSuggestedActionForStuckOrder(String orderStatus, Long minutesStuck) {
        if (orderStatus == null) {
            return "Review order status";
        }

        return switch (orderStatus.toUpperCase()) {
            case "PENDING" -> minutesStuck > 30
                    ? "Consider auto-cancellation or reassignment"
                    : "Contact restaurant to confirm acceptance";
            case "ACCEPTED" -> "Check with restaurant on preparation start";
            case "PREPARING" -> minutesStuck > 45
                    ? "Escalate to restaurant manager"
                    : "Verify preparation progress with restaurant";
            case "READY" -> "Dispatch available courier immediately";
            case "IN_TRANSIT" -> minutesStuck > 60
                    ? "Contact courier and consider redelivery"
                    : "Check courier location and ETA";
            default -> "Review order and take appropriate action";
        };
    }

    /**
     * Calculate health score (0-100) from system metrics.
     *
     * @param apiLatencyMs API latency in milliseconds
     * @param dbLoadPercent database load percentage
     * @param redisLatencyMs Redis latency in milliseconds
     * @param errorRate error rate percentage
     * @return health score (100 = perfect health)
     */
    public static Double calculateHealthScore(Double apiLatencyMs, Double dbLoadPercent,
                                               Double redisLatencyMs, Double errorRate) {
        double score = 100.0;

        // API latency impact (up to -30 points)
        if (apiLatencyMs != null) {
            if (apiLatencyMs > 2000) score -= 30;
            else if (apiLatencyMs > 1000) score -= 20;
            else if (apiLatencyMs > 500) score -= 10;
            else if (apiLatencyMs > 200) score -= 5;
        }

        // DB load impact (up to -25 points)
        if (dbLoadPercent != null) {
            if (dbLoadPercent > 90) score -= 25;
            else if (dbLoadPercent > 70) score -= 15;
            else if (dbLoadPercent > 50) score -= 5;
        }

        // Redis latency impact (up to -25 points)
        if (redisLatencyMs != null) {
            if (redisLatencyMs > 100) score -= 25;
            else if (redisLatencyMs > 50) score -= 15;
            else if (redisLatencyMs > 20) score -= 5;
        }

        // Error rate impact (up to -20 points)
        if (errorRate != null) {
            if (errorRate > 5) score -= 20;
            else if (errorRate > 2) score -= 10;
            else if (errorRate > 0.5) score -= 5;
        }

        return Math.max(0, score);
    }
}

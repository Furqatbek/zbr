package com.fooddelivery.admin.dashboard.mapper;

import com.fooddelivery.admin.dashboard.dto.SupportMetricsDto.SupportTicketItemDto;
import com.fooddelivery.admin.dashboard.dto.SupportMetricsDto.AgentPerformanceDto;
import com.fooddelivery.admin.dashboard.util.DashboardMetricsCalculator;
import com.fooddelivery.admin.dashboard.util.DashboardConstants;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * MapStruct mapper for support-related dashboard DTOs.
 */
@Mapper(componentModel = "spring", imports = {ChronoUnit.class, LocalDateTime.class, DashboardMetricsCalculator.class, DashboardConstants.class})
public interface DashboardSupportMapper {

    /**
     * Map to SupportTicketItemDto.
     */
    @Mapping(target = "ticketId", source = "ticketId")
    @Mapping(target = "ticketNumber", source = "ticketNumber")
    @Mapping(target = "userId", source = "customerId")
    @Mapping(target = "userName", source = "customerName")
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "ticketType", source = "category")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "assignedToId", source = "assignedAgentId")
    @Mapping(target = "assignedToName", source = "assignedAgentName")
    @Mapping(target = "firstResponseAt", source = "firstResponseAt")
    @Mapping(target = "resolvedAt", source = "resolvedAt")
    @Mapping(target = "escalated", source = "isEscalated")
    @Mapping(target = "refundAmount", source = "refundAmount")
    @Mapping(target = "responseTimeMinutes", expression = "java(calculateWaitTime(createdAt, firstResponseAt, status))")
    @Mapping(target = "ageMinutes", expression = "java(calculateAge(createdAt))")
    @Mapping(target = "slaBreach", expression = "java(isSlaBreached(priority, calculateWaitTime(createdAt, firstResponseAt, status), status))")
    SupportTicketItemDto toSupportTicketItem(
            Long ticketId,
            String ticketNumber,
            Long customerId,
            String customerName,
            Long orderId,
            String category,
            String priority,
            String status,
            String subject,
            LocalDateTime createdAt,
            Long assignedAgentId,
            String assignedAgentName,
            LocalDateTime firstResponseAt,
            LocalDateTime resolvedAt,
            Boolean isEscalated,
            BigDecimal refundAmount
    );

    /**
     * Map to AgentPerformanceDto.
     */
    @Mapping(target = "agentId", source = "agentId")
    @Mapping(target = "agentName", source = "agentName")
    @Mapping(target = "ticketsAssigned", source = "ticketsHandled")
    @Mapping(target = "ticketsResolved", source = "ticketsResolved")
    @Mapping(target = "slaComplianceRate", expression = "java(calculateResolutionRate(ticketsHandled, ticketsResolved))")
    @Mapping(target = "avgResolutionTimeHours", expression = "java(avgResolutionTime != null ? avgResolutionTime / 60.0 : null)")
    @Mapping(target = "avgFirstResponseTimeMinutes", source = "avgFirstResponseTime")
    @Mapping(target = "csatScore", source = "avgSatisfactionScore")
    @Mapping(target = "currentOpenTickets", source = "currentOpenTickets")
    AgentPerformanceDto toAgentPerformance(
            Long agentId,
            String agentName,
            Long ticketsHandled,
            Long ticketsResolved,
            Double avgResolutionTime,
            Double avgFirstResponseTime,
            Double avgSatisfactionScore,
            Long currentOpenTickets
    );

    /**
     * Calculate wait time in minutes.
     */
    default Long calculateWaitTime(LocalDateTime createdAt, LocalDateTime firstResponseAt, String status) {
        if (firstResponseAt != null) {
            return ChronoUnit.MINUTES.between(createdAt, firstResponseAt);
        }
        if ("OPEN".equalsIgnoreCase(status)) {
            return ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
        }
        return null;
    }

    /**
     * Calculate ticket age in minutes.
     */
    default Long calculateAge(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0L;
        }
        return ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
    }

    /**
     * Check if SLA is breached based on priority.
     */
    default Boolean isSlaBreached(String priority, Long waitTimeMinutes, String status) {
        if (waitTimeMinutes == null || "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return false;
        }

        int slaThreshold = DashboardConstants.getSlaThreshold(priority);
        return waitTimeMinutes > slaThreshold;
    }

    /**
     * Calculate resolution rate.
     */
    default Double calculateResolutionRate(Long ticketsHandled, Long ticketsResolved) {
        if (ticketsHandled == null || ticketsHandled == 0) {
            return 0.0;
        }
        return DashboardMetricsCalculator.roundToTwoDecimals(
                (ticketsResolved.doubleValue() / ticketsHandled) * 100);
    }

    /**
     * Calculate agent performance score.
     */
    default Double calculateAgentPerformanceScore(Double resolutionRate, Double avgResolutionTime,
                                                   Double satisfactionScore) {
        if (resolutionRate == null) resolutionRate = 0.0;
        if (avgResolutionTime == null) avgResolutionTime = 0.0;
        if (satisfactionScore == null) satisfactionScore = 0.0;

        // Resolution rate: 30%
        double resolutionScore = (resolutionRate / 100.0) * 30;

        // Resolution time: 30% (lower is better, assuming 120 min is baseline)
        double timeScore = Math.max(0, (1 - (avgResolutionTime / 120.0))) * 30;

        // Satisfaction: 40% (assuming 5-point scale)
        double satisfactionPart = (satisfactionScore / 5.0) * 40;

        return DashboardMetricsCalculator.roundToTwoDecimals(resolutionScore + timeScore + satisfactionPart);
    }

    /**
     * Format duration to human-readable string.
     */
    default String formatDuration(Long minutes) {
        return DashboardMetricsCalculator.formatDuration(minutes);
    }

    /**
     * Round to two decimal places.
     */
    default Double roundValue(Double value) {
        return DashboardMetricsCalculator.roundToTwoDecimals(value);
    }
}

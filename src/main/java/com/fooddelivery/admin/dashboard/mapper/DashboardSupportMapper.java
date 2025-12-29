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
 * Uses correct property names from SupportMetricsDto inner classes.
 */
@Mapper(componentModel = "spring", imports = {ChronoUnit.class, LocalDateTime.class, DashboardMetricsCalculator.class, DashboardConstants.class})
public interface DashboardSupportMapper {

    /**
     * Map to SupportTicketItemDto.
     * Property names match SupportTicketItemDto fields.
     */
    @Mapping(target = "ticketId", source = "ticketId")
    @Mapping(target = "ticketNumber", source = "ticketNumber")
    @Mapping(target = "ticketType", source = "ticketType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "userName", source = "userName")
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "assignedToId", source = "assignedToId")
    @Mapping(target = "assignedToName", source = "assignedToName")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "firstResponseAt", source = "firstResponseAt")
    @Mapping(target = "resolvedAt", source = "resolvedAt")
    @Mapping(target = "ageMinutes", expression = "java(calculateAge(createdAt))")
    @Mapping(target = "responseTimeMinutes", expression = "java(calculateResponseTime(createdAt, firstResponseAt))")
    @Mapping(target = "slaBreach", expression = "java(isSlaBreached(priority, calculateResponseTime(createdAt, firstResponseAt), status))")
    @Mapping(target = "escalated", source = "escalated")
    @Mapping(target = "isRefundRequest", source = "isRefundRequest")
    @Mapping(target = "refundAmount", source = "refundAmount")
    SupportTicketItemDto toSupportTicketItem(
            Long ticketId,
            String ticketNumber,
            String ticketType,
            String status,
            String priority,
            String channel,
            Long userId,
            String userName,
            Long orderId,
            String subject,
            Long assignedToId,
            String assignedToName,
            LocalDateTime createdAt,
            LocalDateTime firstResponseAt,
            LocalDateTime resolvedAt,
            Boolean escalated,
            Boolean isRefundRequest,
            BigDecimal refundAmount
    );

    /**
     * Map to AgentPerformanceDto.
     * Property names match AgentPerformanceDto fields.
     */
    @Mapping(target = "agentId", source = "agentId")
    @Mapping(target = "agentName", source = "agentName")
    @Mapping(target = "ticketsAssigned", source = "ticketsAssigned")
    @Mapping(target = "ticketsResolved", source = "ticketsResolved")
    @Mapping(target = "avgResolutionTimeHours", source = "avgResolutionTimeHours")
    @Mapping(target = "avgFirstResponseTimeMinutes", source = "avgFirstResponseTimeMinutes")
    @Mapping(target = "csatScore", source = "csatScore")
    @Mapping(target = "slaComplianceRate", source = "slaComplianceRate")
    @Mapping(target = "currentOpenTickets", source = "currentOpenTickets")
    AgentPerformanceDto toAgentPerformance(
            Long agentId,
            String agentName,
            Long ticketsAssigned,
            Long ticketsResolved,
            Double avgResolutionTimeHours,
            Double avgFirstResponseTimeMinutes,
            Double csatScore,
            Double slaComplianceRate,
            Long currentOpenTickets
    );

    /**
     * Calculate response time in minutes.
     */
    default Long calculateResponseTime(LocalDateTime createdAt, LocalDateTime firstResponseAt) {
        if (firstResponseAt != null && createdAt != null) {
            return ChronoUnit.MINUTES.between(createdAt, firstResponseAt);
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
    default Boolean isSlaBreached(String priority, Long responseTimeMinutes, String status) {
        if (responseTimeMinutes == null || "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return false;
        }

        int slaThreshold = DashboardConstants.getSlaThreshold(priority);
        return responseTimeMinutes > slaThreshold;
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

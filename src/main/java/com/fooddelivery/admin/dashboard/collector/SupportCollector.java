package com.fooddelivery.admin.dashboard.collector;

import com.fooddelivery.admin.dashboard.dto.DashboardFilterRequest;
import com.fooddelivery.admin.dashboard.dto.SupportMetricsDto;
import com.fooddelivery.admin.dashboard.dto.SupportMetricsDto.*;
import com.fooddelivery.admin.dashboard.repository.DashboardSupportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for customer support metrics and ticket data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupportCollector {

    private final DashboardSupportRepository supportRepository;

    /**
     * Collect comprehensive support metrics.
     */
    public SupportMetricsDto collectSupportMetrics(DashboardFilterRequest filter) {
        log.debug("Collecting support metrics with filter: {}", filter);

        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        Pageable pageable = createPageable(filter);

        // Get ticket counts
        Long totalTickets = supportRepository.countTotalTickets(startDate, endDate);
        Long openTickets = supportRepository.countOpenTickets(startDate, endDate);
        Long inProgressTickets = supportRepository.countInProgressTickets(startDate, endDate);
        Long resolvedTickets = supportRepository.countResolvedTickets(startDate, endDate);
        Long closedTickets = supportRepository.countClosedTickets(startDate, endDate);
        Long escalatedTickets = supportRepository.countEscalatedTickets(startDate, endDate);

        // Get ticket types breakdown
        Long complaintCount = supportRepository.countComplaints(startDate, endDate);
        Long refundRequestCount = supportRepository.countRefundRequests(startDate, endDate);
        Long inquiryCount = supportRepository.countInquiries(startDate, endDate);
        Long feedbackCount = supportRepository.countFeedback(startDate, endDate);

        // Performance metrics
        Double avgResolutionTimeMinutes = supportRepository.avgResolutionTimeMinutes(startDate, endDate);
        Double avgFirstResponseTimeMinutes = supportRepository.avgFirstResponseTimeMinutes(startDate, endDate);
        Double customerSatisfactionScore = supportRepository.avgCustomerSatisfactionScore(startDate, endDate);

        // Get detailed ticket list
        List<SupportTicketItemDto> tickets = collectTicketDetails(filter, pageable);

        // Get status breakdown
        Map<String, Long> statusBreakdown = collectStatusBreakdown(startDate, endDate);

        // Get priority breakdown
        Map<String, Long> priorityBreakdown = collectPriorityBreakdown(startDate, endDate);

        // Get category breakdown
        Map<String, Long> categoryBreakdown = collectCategoryBreakdown(startDate, endDate);

        // Get agent performance
        List<AgentPerformanceDto> agentPerformance = collectAgentPerformance(startDate, endDate, 20);

        // Get common issues
        List<CommonIssueDto> commonIssues = collectCommonIssues(startDate, endDate, 10);

        // Get hourly ticket distribution
        Map<Integer, Long> hourlyDistribution = collectHourlyDistribution(startDate, endDate);

        // Calculate SLA metrics
        Map<String, Object> slaMetrics = collectSlaMetrics(startDate, endDate);

        // Pending tickets requiring attention
        List<SupportTicketItemDto> pendingAttention = collectPendingAttentionTickets(startDate, endDate, 20);

        return SupportMetricsDto.builder()
                .totalTickets(totalTickets)
                .openTickets(openTickets)
                .inProgressTickets(inProgressTickets)
                .resolvedTickets(resolvedTickets)
                .closedTickets(closedTickets)
                .escalatedTickets(escalatedTickets)
                .complaintCount(complaintCount)
                .refundRequestCount(refundRequestCount)
                .inquiryCount(inquiryCount)
                .feedbackCount(feedbackCount)
                .avgResolutionTimeMinutes(roundToTwoDecimals(avgResolutionTimeMinutes))
                .avgResolutionTimeFormatted(formatDuration(avgResolutionTimeMinutes))
                .avgFirstResponseTimeMinutes(roundToTwoDecimals(avgFirstResponseTimeMinutes))
                .avgFirstResponseTimeFormatted(formatDuration(avgFirstResponseTimeMinutes))
                .customerSatisfactionScore(roundToTwoDecimals(customerSatisfactionScore))
                .resolutionRate(calculateResolutionRate(resolvedTickets, closedTickets, totalTickets))
                .tickets(tickets)
                .statusBreakdown(statusBreakdown)
                .priorityBreakdown(priorityBreakdown)
                .categoryBreakdown(categoryBreakdown)
                .agentPerformance(agentPerformance)
                .commonIssues(commonIssues)
                .hourlyDistribution(hourlyDistribution)
                .slaMetrics(slaMetrics)
                .pendingAttentionTickets(pendingAttention)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Collect detailed ticket information.
     */
    private List<SupportTicketItemDto> collectTicketDetails(DashboardFilterRequest filter, Pageable pageable) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();

        List<Object[]> ticketData = supportRepository.findTicketDetails(startDate, endDate, pageable);

        return ticketData.stream()
                .map(this::mapToTicketItem)
                .collect(Collectors.toList());
    }

    /**
     * Map raw query result to SupportTicketItemDto.
     */
    private SupportTicketItemDto mapToTicketItem(Object[] row) {
        int idx = 0;
        Long ticketId = ((Number) row[idx++]).longValue();
        String ticketNumber = (String) row[idx++];
        Long customerId = row[idx] != null ? ((Number) row[idx]).longValue() : null;
        idx++;
        String customerName = (String) row[idx++];
        Long orderId = row[idx] != null ? ((Number) row[idx]).longValue() : null;
        idx++;
        String category = (String) row[idx++];
        String priority = (String) row[idx++];
        String status = (String) row[idx++];
        String subject = (String) row[idx++];
        LocalDateTime createdAt = (LocalDateTime) row[idx++];
        LocalDateTime updatedAt = row[idx] != null ? (LocalDateTime) row[idx] : null;
        idx++;
        Long assignedAgentId = row[idx] != null ? ((Number) row[idx]).longValue() : null;
        idx++;
        String assignedAgentName = (String) row[idx++];
        LocalDateTime firstResponseAt = row[idx] != null ? (LocalDateTime) row[idx] : null;
        idx++;
        LocalDateTime resolvedAt = row[idx] != null ? (LocalDateTime) row[idx] : null;
        idx++;
        Boolean isEscalated = row[idx] != null ? (Boolean) row[idx] : false;
        idx++;
        BigDecimal refundAmount = row[idx] != null ? (BigDecimal) row[idx] : null;

        // Calculate wait time
        Long waitTimeMinutes = calculateWaitTime(createdAt, firstResponseAt, status);

        // Calculate age
        Long ageMinutes = ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());

        return SupportTicketItemDto.builder()
                .ticketId(ticketId)
                .ticketNumber(ticketNumber)
                .customerId(customerId)
                .customerName(customerName)
                .orderId(orderId)
                .category(category)
                .priority(priority)
                .status(status)
                .subject(subject)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .assignedAgentId(assignedAgentId)
                .assignedAgentName(assignedAgentName)
                .firstResponseAt(firstResponseAt)
                .resolvedAt(resolvedAt)
                .isEscalated(isEscalated)
                .refundAmount(refundAmount)
                .waitTimeMinutes(waitTimeMinutes)
                .waitTimeFormatted(formatDuration(waitTimeMinutes != null ? waitTimeMinutes.doubleValue() : null))
                .ageMinutes(ageMinutes)
                .ageFormatted(formatDuration(ageMinutes.doubleValue()))
                .slaBreached(isSlaBreached(priority, waitTimeMinutes, status))
                .build();
    }

    /**
     * Calculate wait time in minutes.
     */
    private Long calculateWaitTime(LocalDateTime createdAt, LocalDateTime firstResponseAt, String status) {
        if (firstResponseAt != null) {
            return ChronoUnit.MINUTES.between(createdAt, firstResponseAt);
        }
        if ("OPEN".equalsIgnoreCase(status)) {
            return ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
        }
        return null;
    }

    /**
     * Check if SLA is breached based on priority.
     */
    private Boolean isSlaBreached(String priority, Long waitTimeMinutes, String status) {
        if (waitTimeMinutes == null || "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return false;
        }

        // SLA thresholds in minutes
        int slaThreshold = switch (priority.toUpperCase()) {
            case "CRITICAL" -> 15;
            case "HIGH" -> 30;
            case "MEDIUM" -> 60;
            case "LOW" -> 120;
            default -> 60;
        };

        return waitTimeMinutes > slaThreshold;
    }

    /**
     * Collect status breakdown.
     */
    private Map<String, Long> collectStatusBreakdown(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        breakdown.put("OPEN", supportRepository.countOpenTickets(startDate, endDate));
        breakdown.put("IN_PROGRESS", supportRepository.countInProgressTickets(startDate, endDate));
        breakdown.put("WAITING_CUSTOMER", supportRepository.countWaitingCustomerTickets(startDate, endDate));
        breakdown.put("RESOLVED", supportRepository.countResolvedTickets(startDate, endDate));
        breakdown.put("CLOSED", supportRepository.countClosedTickets(startDate, endDate));
        return breakdown;
    }

    /**
     * Collect priority breakdown.
     */
    private Map<String, Long> collectPriorityBreakdown(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> priorityData = supportRepository.countByPriority(startDate, endDate);
        Map<String, Long> breakdown = new LinkedHashMap<>();

        // Initialize with all priorities
        breakdown.put("CRITICAL", 0L);
        breakdown.put("HIGH", 0L);
        breakdown.put("MEDIUM", 0L);
        breakdown.put("LOW", 0L);

        for (Object[] row : priorityData) {
            String priority = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            breakdown.put(priority, count);
        }

        return breakdown;
    }

    /**
     * Collect category breakdown.
     */
    private Map<String, Long> collectCategoryBreakdown(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> categoryData = supportRepository.countByCategory(startDate, endDate);
        Map<String, Long> breakdown = new LinkedHashMap<>();

        for (Object[] row : categoryData) {
            String category = row[0] != null ? (String) row[0] : "OTHER";
            Long count = ((Number) row[1]).longValue();
            breakdown.put(category, count);
        }

        return breakdown;
    }

    /**
     * Collect agent performance metrics.
     */
    private List<AgentPerformanceDto> collectAgentPerformance(LocalDateTime startDate,
                                                               LocalDateTime endDate, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> agentData = supportRepository.getAgentPerformance(startDate, endDate, pageable);

        return agentData.stream()
                .map(row -> {
                    int idx = 0;
                    Long agentId = ((Number) row[idx++]).longValue();
                    String agentName = (String) row[idx++];
                    Long ticketsHandled = ((Number) row[idx++]).longValue();
                    Long ticketsResolved = ((Number) row[idx++]).longValue();
                    Double avgResolutionTime = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
                    idx++;
                    Double avgFirstResponseTime = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
                    idx++;
                    Double avgSatisfactionScore = row[idx] != null ? ((Number) row[idx]).doubleValue() : 0.0;
                    idx++;
                    Long currentOpenTickets = row[idx] != null ? ((Number) row[idx]).longValue() : 0L;

                    Double resolutionRate = ticketsHandled > 0
                            ? (ticketsResolved.doubleValue() / ticketsHandled) * 100
                            : 0.0;

                    return AgentPerformanceDto.builder()
                            .agentId(agentId)
                            .agentName(agentName)
                            .ticketsHandled(ticketsHandled)
                            .ticketsResolved(ticketsResolved)
                            .resolutionRate(roundToTwoDecimals(resolutionRate))
                            .avgResolutionTimeMinutes(roundToTwoDecimals(avgResolutionTime))
                            .avgFirstResponseTimeMinutes(roundToTwoDecimals(avgFirstResponseTime))
                            .avgSatisfactionScore(roundToTwoDecimals(avgSatisfactionScore))
                            .currentOpenTickets(currentOpenTickets)
                            .performanceScore(calculateAgentPerformanceScore(
                                    resolutionRate, avgResolutionTime, avgSatisfactionScore))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate agent performance score.
     */
    private Double calculateAgentPerformanceScore(Double resolutionRate, Double avgResolutionTime,
                                                   Double satisfactionScore) {
        // Resolution rate: 30%
        double resolutionScore = (resolutionRate / 100.0) * 30;

        // Resolution time: 30% (lower is better, assuming 60 min is baseline)
        double timeScore = Math.max(0, (1 - (avgResolutionTime / 120.0))) * 30;

        // Satisfaction: 40% (assuming 5-point scale)
        double satisfactionPart = (satisfactionScore / 5.0) * 40;

        return roundToTwoDecimals(resolutionScore + timeScore + satisfactionPart);
    }

    /**
     * Collect common issues.
     */
    private List<CommonIssueDto> collectCommonIssues(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        List<Object[]> issueData = supportRepository.getCommonIssues(startDate, endDate, limit);

        Long totalTickets = supportRepository.countTotalTickets(startDate, endDate);

        return issueData.stream()
                .map(row -> {
                    String category = (String) row[0];
                    String subcategory = row[1] != null ? (String) row[1] : "General";
                    Long count = ((Number) row[2]).longValue();
                    Double avgResolutionTime = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;

                    Double percentage = totalTickets > 0
                            ? (count.doubleValue() / totalTickets) * 100
                            : 0.0;

                    return CommonIssueDto.builder()
                            .category(category)
                            .subcategory(subcategory)
                            .count(count)
                            .percentage(roundToTwoDecimals(percentage))
                            .avgResolutionTimeMinutes(roundToTwoDecimals(avgResolutionTime))
                            .trend(determineTrend(category, startDate, endDate))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Determine trend for an issue category.
     */
    private String determineTrend(String category, LocalDateTime startDate, LocalDateTime endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween == 0) daysBetween = 1;

        LocalDateTime prevStart = startDate.minusDays(daysBetween);
        LocalDateTime prevEnd = startDate;

        Long currentCount = supportRepository.countByCategory(category, startDate, endDate);
        Long previousCount = supportRepository.countByCategory(category, prevStart, prevEnd);

        if (previousCount == 0) {
            return currentCount > 0 ? "UP" : "STABLE";
        }

        double change = ((currentCount - previousCount) / (double) previousCount) * 100;

        if (change > 10) return "UP";
        if (change < -10) return "DOWN";
        return "STABLE";
    }

    /**
     * Collect hourly ticket distribution.
     */
    private Map<Integer, Long> collectHourlyDistribution(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> hourlyData = supportRepository.getHourlyDistribution(startDate, endDate);
        Map<Integer, Long> distribution = new LinkedHashMap<>();

        // Initialize all hours
        for (int hour = 0; hour < 24; hour++) {
            distribution.put(hour, 0L);
        }

        for (Object[] row : hourlyData) {
            Integer hour = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            distribution.put(hour, count);
        }

        return distribution;
    }

    /**
     * Collect SLA metrics.
     */
    private Map<String, Object> collectSlaMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> slaMetrics = new LinkedHashMap<>();

        Long totalTickets = supportRepository.countTotalTickets(startDate, endDate);
        Long slaBreachedTickets = supportRepository.countSlaBreachedTickets(startDate, endDate);
        Long slaMetTickets = totalTickets - slaBreachedTickets;

        Double slaComplianceRate = totalTickets > 0
                ? (slaMetTickets.doubleValue() / totalTickets) * 100
                : 100.0;

        slaMetrics.put("totalTickets", totalTickets);
        slaMetrics.put("slaMetTickets", slaMetTickets);
        slaMetrics.put("slaBreachedTickets", slaBreachedTickets);
        slaMetrics.put("slaComplianceRate", roundToTwoDecimals(slaComplianceRate));

        // SLA by priority
        Map<String, Double> slaByPriority = new LinkedHashMap<>();
        slaByPriority.put("CRITICAL", supportRepository.getSlaComplianceByPriority("CRITICAL", startDate, endDate));
        slaByPriority.put("HIGH", supportRepository.getSlaComplianceByPriority("HIGH", startDate, endDate));
        slaByPriority.put("MEDIUM", supportRepository.getSlaComplianceByPriority("MEDIUM", startDate, endDate));
        slaByPriority.put("LOW", supportRepository.getSlaComplianceByPriority("LOW", startDate, endDate));
        slaMetrics.put("slaByPriority", slaByPriority);

        return slaMetrics;
    }

    /**
     * Collect tickets pending attention (open, high priority, or SLA breached).
     */
    private List<SupportTicketItemDto> collectPendingAttentionTickets(LocalDateTime startDate,
                                                                       LocalDateTime endDate, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> ticketData = supportRepository.findPendingAttentionTickets(startDate, endDate, pageable);

        return ticketData.stream()
                .map(this::mapToTicketItem)
                .collect(Collectors.toList());
    }

    /**
     * Calculate resolution rate.
     */
    private Double calculateResolutionRate(Long resolved, Long closed, Long total) {
        if (total == null || total == 0) {
            return 0.0;
        }
        return roundToTwoDecimals(((resolved + closed) * 100.0) / total);
    }

    /**
     * Format duration in minutes to human-readable string.
     */
    private String formatDuration(Double minutes) {
        if (minutes == null || minutes == 0) {
            return "0m";
        }

        long totalMinutes = Math.round(minutes);
        long hours = totalMinutes / 60;
        long mins = totalMinutes % 60;

        if (hours > 24) {
            long days = hours / 24;
            hours = hours % 24;
            return String.format("%dd %dh %dm", days, hours, mins);
        }
        if (hours > 0) {
            return String.format("%dh %dm", hours, mins);
        }
        return String.format("%dm", mins);
    }

    /**
     * Create pageable from filter.
     */
    private Pageable createPageable(DashboardFilterRequest filter) {
        int page = filter.getPageNumber();
        int size = filter.getPageSize();
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        String sortDir = filter.getSortDirection() != null ? filter.getSortDirection() : "DESC";

        Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return PageRequest.of(page, size, sort);
    }

    /**
     * Round to two decimal places.
     */
    private Double roundToTwoDecimals(Double value) {
        if (value == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

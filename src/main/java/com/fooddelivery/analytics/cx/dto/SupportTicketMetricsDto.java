package com.fooddelivery.analytics.cx.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for support ticket metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketMetricsDto {

    // Volume metrics
    private Long totalTickets;
    private Long openTickets;
    private Long inProgressTickets;
    private Long resolvedTickets;
    private Long closedTickets;

    // Volume by period
    private Double ticketsPerDay;
    private Double ticketsPerWeek;
    private Double ticketsPerMonth;

    // Volume by type
    private Map<String, Long> ticketsByType;
    private Map<String, Double> ticketsByTypePercentage;

    // Volume by status
    private Map<String, Long> ticketsByStatus;

    // Volume by priority
    private Map<String, Long> ticketsByPriority;

    // Volume by channel
    private Map<String, Long> ticketsByChannel;

    // Performance metrics
    private PerformanceMetricsDto performance;

    // SLA metrics
    private SlaMetricsDto slaMetrics;

    // Agent performance
    private List<AgentPerformanceDto> agentPerformance;

    // Trend data
    private List<TicketTrendDto> ticketTrend;

    // Resolution breakdown
    private ResolutionBreakdownDto resolutionBreakdown;

    // Customer satisfaction
    private CsatMetricsDto csatMetrics;

    // Period info
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Comparison
    private Long previousPeriodTickets;
    private Double ticketVolumeChange;

    /**
     * Performance metrics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetricsDto {
        private Double avgResolutionTimeHours;
        private Double medianResolutionTimeHours;
        private Double p90ResolutionTimeHours;
        private Double avgFirstResponseTimeMinutes;
        private Double medianFirstResponseTimeMinutes;
        private Double resolutionRate;
        private Double firstContactResolutionRate;
        private Double reopenRate;
        private Double escalationRate;
    }

    /**
     * SLA metrics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlaMetricsDto {
        private Integer slaHours;
        private Long totalTicketsInPeriod;
        private Long ticketsWithinSla;
        private Long ticketsBreachedSla;
        private Double slaComplianceRate;
        private Long currentlyBreached; // Open tickets that breached SLA
        private Long atRiskOfBreach; // Open tickets close to breaching
        private Map<String, Long> slaBreachesByType;
        private Map<String, Long> slaBreachesByPriority;
    }

    /**
     * Agent performance.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentPerformanceDto {
        private Long agentId;
        private String agentName;
        private Long ticketsHandled;
        private Long ticketsResolved;
        private Double avgResolutionTimeHours;
        private Double avgFirstResponseMinutes;
        private Double csatScore;
        private Long slaBreaches;
        private Double resolutionRate;
    }

    /**
     * Ticket trend data point.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTrendDto {
        private LocalDateTime date;
        private Long ticketsCreated;
        private Long ticketsResolved;
        private Long ticketsClosed;
        private Double avgResolutionTimeHours;
        private Map<String, Long> ticketsByType;
    }

    /**
     * Resolution breakdown.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolutionBreakdownDto {
        private Long refundsIssued;
        private Double totalRefundAmount;
        private Double avgRefundAmount;
        private Long creditsIssued;
        private Double totalCreditsAmount;
        private Long apologies; // Resolved without compensation
        private Map<String, Long> resolutionByType;
    }

    /**
     * Customer satisfaction metrics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CsatMetricsDto {
        private Double avgCsatScore; // 1-5
        private Long csatResponseCount;
        private Double csatResponseRate;
        private Map<Integer, Long> csatDistribution; // 1-5
        private Double satisfiedRate; // Score >= 4
        private Double dissatisfiedRate; // Score <= 2
    }
}

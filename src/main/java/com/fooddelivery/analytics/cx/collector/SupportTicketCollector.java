package com.fooddelivery.analytics.cx.collector;

import com.fooddelivery.analytics.cx.dto.SupportTicketMetricsDto;
import com.fooddelivery.analytics.cx.model.SupportTicket;
import com.fooddelivery.analytics.cx.repository.SupportTicketRepository;
import com.fooddelivery.analytics.technical.util.PercentileCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for support ticket metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupportTicketCollector {

    private final SupportTicketRepository supportTicketRepository;
    private final PercentileCalculator percentileCalculator;

    // Default SLA hours
    private static final int DEFAULT_SLA_HOURS = 24;
    private static final int WARNING_HOURS = 20; // 80% of SLA

    /**
     * Collect support ticket metrics.
     */
    public SupportTicketMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate,
                                           int slaHours, boolean includeTrend, boolean includeAgentPerformance) {
        log.debug("Collecting support ticket metrics from {} to {}", startDate, endDate);

        if (slaHours <= 0) slaHours = DEFAULT_SLA_HOURS;

        // Volume metrics
        Long totalTickets = supportTicketRepository.countTicketsInPeriod(startDate, endDate);
        Long openTickets = supportTicketRepository.countOpenTickets();

        if (totalTickets == null) totalTickets = 0L;
        if (openTickets == null) openTickets = 0L;

        // Volume by type
        Map<String, Long> ticketsByType = new HashMap<>();
        supportTicketRepository.countByType(startDate, endDate).forEach(row -> {
            String type = ((SupportTicket.TicketType) row[0]).name();
            Long count = ((Number) row[1]).longValue();
            ticketsByType.put(type, count);
        });

        // Volume by status
        Map<String, Long> ticketsByStatus = new HashMap<>();
        Long inProgressTickets = 0L;
        Long resolvedTickets = 0L;
        Long closedTickets = 0L;
        for (Object[] row : supportTicketRepository.countByStatus(startDate, endDate)) {
            SupportTicket.TicketStatus status = (SupportTicket.TicketStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            ticketsByStatus.put(status.name(), count);

            switch (status) {
                case IN_PROGRESS -> inProgressTickets = count;
                case RESOLVED -> resolvedTickets = count;
                case CLOSED -> closedTickets = count;
            }
        }

        // Volume by priority
        Map<String, Long> ticketsByPriority = new HashMap<>();
        supportTicketRepository.countByPriority(startDate, endDate).forEach(row -> {
            String priority = ((SupportTicket.TicketPriority) row[0]).name();
            Long count = ((Number) row[1]).longValue();
            ticketsByPriority.put(priority, count);
        });

        // Volume by channel
        Map<String, Long> ticketsByChannel = new HashMap<>();
        supportTicketRepository.countByChannel(startDate, endDate).forEach(row -> {
            if (row[0] != null) {
                String channel = ((SupportTicket.TicketChannel) row[0]).name();
                Long count = ((Number) row[1]).longValue();
                ticketsByChannel.put(channel, count);
            }
        });

        // Calculate percentages
        Map<String, Double> ticketsByTypePercentage = new HashMap<>();
        final long finalTotalTickets = totalTickets;
        ticketsByType.forEach((type, count) -> {
            ticketsByTypePercentage.put(type, finalTotalTickets > 0 ? count * 100.0 / finalTotalTickets : 0.0);
        });

        // Calculate tickets per period
        long periodDays = Duration.between(startDate, endDate).toDays();
        if (periodDays == 0) periodDays = 1;
        double ticketsPerDay = totalTickets.doubleValue() / periodDays;
        double ticketsPerWeek = ticketsPerDay * 7;
        double ticketsPerMonth = ticketsPerDay * 30;

        // Build the DTO
        SupportTicketMetricsDto.SupportTicketMetricsDtoBuilder builder = SupportTicketMetricsDto.builder()
                .totalTickets(totalTickets)
                .openTickets(openTickets)
                .inProgressTickets(inProgressTickets)
                .resolvedTickets(resolvedTickets)
                .closedTickets(closedTickets)
                .ticketsPerDay(ticketsPerDay)
                .ticketsPerWeek(ticketsPerWeek)
                .ticketsPerMonth(ticketsPerMonth)
                .ticketsByType(ticketsByType)
                .ticketsByTypePercentage(ticketsByTypePercentage)
                .ticketsByStatus(ticketsByStatus)
                .ticketsByPriority(ticketsByPriority)
                .ticketsByChannel(ticketsByChannel)
                .periodStart(startDate)
                .periodEnd(endDate);

        // Performance metrics
        SupportTicketMetricsDto.PerformanceMetricsDto performance = collectPerformanceMetrics(startDate, endDate);
        builder.performance(performance);

        // SLA metrics
        SupportTicketMetricsDto.SlaMetricsDto slaMetrics = collectSlaMetrics(startDate, endDate, slaHours);
        builder.slaMetrics(slaMetrics);

        // Resolution breakdown
        SupportTicketMetricsDto.ResolutionBreakdownDto resolution = collectResolutionBreakdown(startDate, endDate);
        builder.resolutionBreakdown(resolution);

        // CSAT metrics
        SupportTicketMetricsDto.CsatMetricsDto csat = collectCsatMetrics(startDate, endDate);
        builder.csatMetrics(csat);

        // Agent performance
        if (includeAgentPerformance) {
            List<SupportTicketMetricsDto.AgentPerformanceDto> agentPerformance = collectAgentPerformance(startDate, endDate);
            builder.agentPerformance(agentPerformance);
        }

        // Ticket trend
        if (includeTrend) {
            List<SupportTicketMetricsDto.TicketTrendDto> trend = collectTicketTrend(startDate, endDate);
            builder.ticketTrend(trend);
        }

        return builder.build();
    }

    /**
     * Collect performance metrics.
     */
    private SupportTicketMetricsDto.PerformanceMetricsDto collectPerformanceMetrics(
            LocalDateTime startDate, LocalDateTime endDate) {

        // Resolution time stats
        Object[] resolutionStats = supportTicketRepository.getResolutionTimeStats(startDate, endDate);
        Double avgResolutionTime = resolutionStats[0] != null ? ((Number) resolutionStats[0]).doubleValue() : null;
        Long resolvedCount = resolutionStats[3] != null ? ((Number) resolutionStats[3]).longValue() : 0L;

        // First response time stats
        Object[] responseStats = supportTicketRepository.getFirstResponseTimeStats(startDate, endDate);
        Double avgFirstResponse = responseStats[0] != null ? ((Number) responseStats[0]).doubleValue() : null;

        // Calculate percentiles for resolution time
        List<Double> resolutionTimes = supportTicketRepository.getResolutionTimesOrdered(startDate, endDate);
        Double medianResolution = null;
        Double p90Resolution = null;
        if (!resolutionTimes.isEmpty()) {
            List<Long> timesAsLong = resolutionTimes.stream()
                    .map(d -> d != null ? d.longValue() : 0L)
                    .collect(Collectors.toList());
            medianResolution = percentileCalculator.calculatePercentile(timesAsLong, 50).doubleValue();
            p90Resolution = percentileCalculator.calculatePercentile(timesAsLong, 90).doubleValue();
        }

        // Resolution rate
        Long totalTickets = supportTicketRepository.countTicketsInPeriod(startDate, endDate);
        Double resolutionRate = totalTickets != null && totalTickets > 0 ?
                resolvedCount * 100.0 / totalTickets : 0.0;

        // Reopen and escalation rates
        Long reopenedTickets = supportTicketRepository.countReopenedTickets(startDate, endDate);
        Long escalatedTickets = supportTicketRepository.countEscalatedTickets(startDate, endDate);
        Double reopenRate = totalTickets != null && totalTickets > 0 ?
                reopenedTickets * 100.0 / totalTickets : 0.0;
        Double escalationRate = totalTickets != null && totalTickets > 0 ?
                escalatedTickets * 100.0 / totalTickets : 0.0;

        return SupportTicketMetricsDto.PerformanceMetricsDto.builder()
                .avgResolutionTimeHours(avgResolutionTime)
                .medianResolutionTimeHours(medianResolution)
                .p90ResolutionTimeHours(p90Resolution)
                .avgFirstResponseTimeMinutes(avgFirstResponse)
                .resolutionRate(resolutionRate)
                .reopenRate(reopenRate)
                .escalationRate(escalationRate)
                .build();
    }

    /**
     * Collect SLA metrics.
     */
    private SupportTicketMetricsDto.SlaMetricsDto collectSlaMetrics(LocalDateTime startDate, LocalDateTime endDate,
                                                                    int slaHours) {
        Long totalTickets = supportTicketRepository.countTicketsInPeriod(startDate, endDate);
        Long withinSla = supportTicketRepository.countWithinSla(startDate, endDate, slaHours);
        Long slaBreaches = supportTicketRepository.countSlaBreaches(startDate, endDate);
        Long currentlyBreached = supportTicketRepository.countCurrentlyBreached(slaHours);
        Long atRisk = supportTicketRepository.countAtRiskOfBreach(WARNING_HOURS, slaHours);

        if (totalTickets == null) totalTickets = 0L;
        if (withinSla == null) withinSla = 0L;
        if (slaBreaches == null) slaBreaches = 0L;
        if (currentlyBreached == null) currentlyBreached = 0L;
        if (atRisk == null) atRisk = 0L;

        Double slaComplianceRate = totalTickets > 0 ? withinSla * 100.0 / totalTickets : 100.0;

        // SLA breaches by type
        Map<String, Long> breachesByType = new HashMap<>();
        supportTicketRepository.countSlaBreachesByType(startDate, endDate).forEach(row -> {
            String type = ((SupportTicket.TicketType) row[0]).name();
            Long count = ((Number) row[1]).longValue();
            breachesByType.put(type, count);
        });

        // SLA breaches by priority
        Map<String, Long> breachesByPriority = new HashMap<>();
        supportTicketRepository.countSlaBreachesByPriority(startDate, endDate).forEach(row -> {
            String priority = ((SupportTicket.TicketPriority) row[0]).name();
            Long count = ((Number) row[1]).longValue();
            breachesByPriority.put(priority, count);
        });

        return SupportTicketMetricsDto.SlaMetricsDto.builder()
                .slaHours(slaHours)
                .totalTicketsInPeriod(totalTickets)
                .ticketsWithinSla(withinSla)
                .ticketsBreachedSla(slaBreaches)
                .slaComplianceRate(slaComplianceRate)
                .currentlyBreached(currentlyBreached)
                .atRiskOfBreach(atRisk)
                .slaBreachesByType(breachesByType)
                .slaBreachesByPriority(breachesByPriority)
                .build();
    }

    /**
     * Collect resolution breakdown metrics.
     */
    private SupportTicketMetricsDto.ResolutionBreakdownDto collectResolutionBreakdown(
            LocalDateTime startDate, LocalDateTime endDate) {

        Object[] refundStats = supportTicketRepository.getRefundStats(startDate, endDate);
        Long refundsIssued = refundStats[0] != null ? ((Number) refundStats[0]).longValue() : 0L;
        Double totalRefundAmount = refundStats[1] != null ? ((Number) refundStats[1]).doubleValue() : 0.0;
        Double avgRefundAmount = refundStats[2] != null ? ((Number) refundStats[2]).doubleValue() : 0.0;

        return SupportTicketMetricsDto.ResolutionBreakdownDto.builder()
                .refundsIssued(refundsIssued)
                .totalRefundAmount(totalRefundAmount)
                .avgRefundAmount(avgRefundAmount)
                .build();
    }

    /**
     * Collect CSAT metrics.
     */
    private SupportTicketMetricsDto.CsatMetricsDto collectCsatMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        Object[] csatStats = supportTicketRepository.getCsatStats(startDate, endDate);

        Double avgCsat = csatStats[0] != null ? ((Number) csatStats[0]).doubleValue() : null;
        Long csatResponseCount = csatStats[1] != null ? ((Number) csatStats[1]).longValue() : 0L;
        Long satisfied = csatStats[2] != null ? ((Number) csatStats[2]).longValue() : 0L;
        Long dissatisfied = csatStats[3] != null ? ((Number) csatStats[3]).longValue() : 0L;

        Long totalTickets = supportTicketRepository.countTicketsInPeriod(startDate, endDate);
        Double csatResponseRate = totalTickets != null && totalTickets > 0 ?
                csatResponseCount * 100.0 / totalTickets : 0.0;
        Double satisfiedRate = csatResponseCount > 0 ? satisfied * 100.0 / csatResponseCount : 0.0;
        Double dissatisfiedRate = csatResponseCount > 0 ? dissatisfied * 100.0 / csatResponseCount : 0.0;

        // CSAT distribution
        Map<Integer, Long> csatDistribution = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            csatDistribution.put(i, 0L);
        }
        supportTicketRepository.getCsatDistribution(startDate, endDate).forEach(row -> {
            Integer score = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            csatDistribution.put(score, count);
        });

        return SupportTicketMetricsDto.CsatMetricsDto.builder()
                .avgCsatScore(avgCsat)
                .csatResponseCount(csatResponseCount)
                .csatResponseRate(csatResponseRate)
                .csatDistribution(csatDistribution)
                .satisfiedRate(satisfiedRate)
                .dissatisfiedRate(dissatisfiedRate)
                .build();
    }

    /**
     * Collect agent performance metrics.
     */
    private List<SupportTicketMetricsDto.AgentPerformanceDto> collectAgentPerformance(
            LocalDateTime startDate, LocalDateTime endDate) {

        return supportTicketRepository.getAgentPerformance(startDate, endDate).stream()
                .map(row -> {
                    Long agentId = row[0] != null ? ((Number) row[0]).longValue() : null;
                    Long handled = ((Number) row[1]).longValue();
                    Long resolved = ((Number) row[2]).longValue();
                    Double avgResolution = row[3] != null ? ((Number) row[3]).doubleValue() : null;
                    Double avgFirstResponse = row[4] != null ? ((Number) row[4]).doubleValue() : null;
                    Double csatScore = row[5] != null ? ((Number) row[5]).doubleValue() : null;
                    Long slaBreaches = row[6] != null ? ((Number) row[6]).longValue() : 0L;
                    Double resolutionRate = handled > 0 ? resolved * 100.0 / handled : 0.0;

                    return SupportTicketMetricsDto.AgentPerformanceDto.builder()
                            .agentId(agentId)
                            .ticketsHandled(handled)
                            .ticketsResolved(resolved)
                            .avgResolutionTimeHours(avgResolution)
                            .avgFirstResponseMinutes(avgFirstResponse)
                            .csatScore(csatScore)
                            .slaBreaches(slaBreaches)
                            .resolutionRate(resolutionRate)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Collect ticket trend data.
     */
    private List<SupportTicketMetricsDto.TicketTrendDto> collectTicketTrend(
            LocalDateTime startDate, LocalDateTime endDate) {

        return supportTicketRepository.getDailyTicketTrend(startDate, endDate).stream()
                .map(row -> SupportTicketMetricsDto.TicketTrendDto.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate().atStartOfDay())
                        .ticketsCreated(((Number) row[1]).longValue())
                        .ticketsResolved(((Number) row[2]).longValue())
                        .ticketsClosed(((Number) row[3]).longValue())
                        .avgResolutionTimeHours(row[4] != null ? ((Number) row[4]).doubleValue() : null)
                        .build())
                .collect(Collectors.toList());
    }
}

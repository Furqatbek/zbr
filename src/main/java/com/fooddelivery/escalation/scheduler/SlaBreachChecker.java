package com.fooddelivery.escalation.scheduler;

import com.fooddelivery.analytics.cx.model.SupportTicket;
import com.fooddelivery.analytics.cx.repository.SupportTicketRepository;
import com.fooddelivery.escalation.service.EscalationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to check for SLA breaches and trigger auto-escalation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaBreachChecker {

    private final SupportTicketRepository ticketRepository;
    private final EscalationService escalationService;

    @Value("${app.support.sla.urgent-minutes:60}")
    private int urgentSlaMinutes;

    @Value("${app.support.sla.high-minutes:120}")
    private int highSlaMinutes;

    @Value("${app.support.sla.medium-minutes:480}")
    private int mediumSlaMinutes;

    @Value("${app.support.sla.low-minutes:1440}")
    private int lowSlaMinutes;

    /**
     * Check for SLA breaches every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${app.support.sla.check-interval-ms:300000}")
    public void checkSlaBreaches() {
        log.debug("Running SLA breach check");

        try {
            checkAndMarkBreaches();
            checkAndAutoEscalate();
        } catch (Exception e) {
            log.error("Error during SLA breach check: {}", e.getMessage(), e);
        }
    }

    /**
     * Check open tickets for SLA breaches.
     */
    private void checkAndMarkBreaches() {
        List<SupportTicket> openTickets = ticketRepository.findByStatusInAndSlaBreach(
                List.of(SupportTicket.TicketStatus.OPEN, SupportTicket.TicketStatus.IN_PROGRESS,
                        SupportTicket.TicketStatus.WAITING_CUSTOMER, SupportTicket.TicketStatus.REOPENED),
                false
        );

        LocalDateTime now = LocalDateTime.now();
        int breachCount = 0;

        for (SupportTicket ticket : openTickets) {
            int slaMinutes = getSlaMinutes(ticket.getPriority());
            LocalDateTime slaDeadline = ticket.getCreatedAt().plusMinutes(slaMinutes);

            if (now.isAfter(slaDeadline)) {
                escalationService.markSlaBreach(ticket.getId());
                breachCount++;
            }
        }

        if (breachCount > 0) {
            log.info("Marked {} tickets as SLA breached", breachCount);
        }
    }

    /**
     * Auto-escalate tickets that meet escalation criteria.
     */
    private void checkAndAutoEscalate() {
        // Find tickets eligible for auto-escalation
        List<SupportTicket> escalationCandidates = ticketRepository.findTicketsForAutoEscalation();

        int escalatedCount = 0;
        for (SupportTicket ticket : escalationCandidates) {
            try {
                escalationService.autoEscalate(ticket.getId())
                        .ifPresent(log -> escalatedCount++);
            } catch (Exception e) {
                log.error("Failed to auto-escalate ticket {}: {}", ticket.getId(), e.getMessage());
            }
        }

        if (escalatedCount > 0) {
            log.info("Auto-escalated {} tickets", escalatedCount);
        }
    }

    /**
     * Get SLA time in minutes based on priority.
     */
    private int getSlaMinutes(SupportTicket.TicketPriority priority) {
        return switch (priority) {
            case URGENT -> urgentSlaMinutes;
            case HIGH -> highSlaMinutes;
            case MEDIUM -> mediumSlaMinutes;
            case LOW -> lowSlaMinutes;
        };
    }
}

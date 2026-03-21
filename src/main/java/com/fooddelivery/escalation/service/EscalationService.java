package com.fooddelivery.escalation.service;

import com.fooddelivery.analytics.cx.model.SupportTicket;
import com.fooddelivery.analytics.cx.repository.SupportTicketRepository;
import com.fooddelivery.escalation.entity.*;
import com.fooddelivery.escalation.repository.EscalationLogRepository;
import com.fooddelivery.escalation.repository.EscalationRuleRepository;
import com.fooddelivery.escalation.service.EscalationRuleEngine.EscalationContext;
import com.fooddelivery.escalation.service.EscalationRuleEngine.EscalationResult;
import com.fooddelivery.notification.service.PersistentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing ticket escalations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationService {

    private final EscalationRuleEngine ruleEngine;
    private final EscalationRuleRepository ruleRepository;
    private final EscalationLogRepository logRepository;
    private final SupportTicketRepository ticketRepository;
    private final PersistentNotificationService notificationService;

    /**
     * Automatically evaluate and escalate a ticket if rules match.
     */
    @Transactional
    public Optional<EscalationLog> autoEscalate(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        // Build context
        int recentComplaints = countRecentComplaints(ticket.getUserId(), 30);
        boolean isVip = checkVipStatus(ticket.getUserId());
        EscalationContext context = new EscalationContext(isVip, recentComplaints);

        // Evaluate rules
        Optional<EscalationResult> result = ruleEngine.evaluate(ticket, context);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        EscalationResult escalation = result.get();
        return Optional.of(escalateTicket(ticket, escalation.targetLevel(),
                EscalationInitiator.SYSTEM, null,
                escalation.description(), escalation.ruleId()));
    }

    /**
     * Manually escalate a ticket.
     */
    @Transactional
    public EscalationLog manualEscalate(Long ticketId, int targetLevel,
                                         Long userId, String reason) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (targetLevel <= ticket.getEscalationLevel()) {
            throw new IllegalArgumentException("Can only escalate to a higher level");
        }

        EscalationInitiator initiator = userId != null ? EscalationInitiator.AGENT : EscalationInitiator.ADMIN;
        return escalateTicket(ticket, targetLevel, initiator, userId, reason, null);
    }

    /**
     * Perform the actual escalation.
     */
    private EscalationLog escalateTicket(SupportTicket ticket, int targetLevel,
                                          EscalationInitiator initiator, Long userId,
                                          String reason, Long ruleId) {
        int previousLevel = ticket.getEscalationLevel();

        log.info("Escalating ticket {} from L{} to L{} - reason: {}",
                ticket.getId(), previousLevel, targetLevel, reason);

        // Update ticket
        ticket.setEscalationLevel(targetLevel);
        ticket.setEscalated(true);
        ticket.setLastEscalatedAt(LocalDateTime.now());
        if (ticket.getEscalatedAt() == null) {
            ticket.setEscalatedAt(LocalDateTime.now());
        }
        ticketRepository.save(ticket);

        // Create log entry
        EscalationRule rule = ruleId != null ? ruleRepository.findById(ruleId).orElse(null) : null;
        EscalationLog logEntry = EscalationLog.builder()
                .ticketId(ticket.getId())
                .previousLevel(previousLevel)
                .newLevel(targetLevel)
                .rule(rule)
                .reason(reason)
                .escalatedBy(initiator)
                .escalatedByUserId(userId)
                .build();
        logEntry = logRepository.save(logEntry);

        // Notify relevant parties
        notifyEscalation(ticket, EscalationLevel.fromLevel(targetLevel));

        return logEntry;
    }

    /**
     * Check for SLA breaches and mark tickets.
     */
    @Transactional
    public int markSlaBreach(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getSlaBreach()) {
            return 0; // Already breached
        }

        ticket.setSlaBreach(true);
        ticket.setSlaBreachedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        log.info("SLA breach marked for ticket {}", ticketId);
        return 1;
    }

    /**
     * Get escalation history for a ticket.
     */
    @Transactional(readOnly = true)
    public List<EscalationLog> getEscalationHistory(Long ticketId) {
        return logRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }

    /**
     * Get all active escalation rules.
     */
    @Transactional(readOnly = true)
    public List<EscalationRule> getActiveRules() {
        return ruleRepository.findActiveRules();
    }

    /**
     * Toggle rule active status.
     */
    @Transactional
    public EscalationRule toggleRuleStatus(Long ruleId) {
        EscalationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
        rule.setActive(!rule.getActive());
        return ruleRepository.save(rule);
    }

    /**
     * Count recent complaints from a user.
     */
    private int countRecentComplaints(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return ticketRepository.countByUserIdAndCreatedAtAfter(userId, since).intValue();
    }

    /**
     * Check if user is VIP (placeholder - integrate with actual VIP system).
     */
    private boolean checkVipStatus(Long userId) {
        // TODO: Integrate with actual customer VIP/loyalty system
        return false;
    }

    /**
     * Notify relevant parties about escalation.
     */
    private void notifyEscalation(SupportTicket ticket, EscalationLevel level) {
        log.info("Sending escalation notification for ticket {} to {}", ticket.getId(), level);

        notificationService.notifySupportTicketUpdated(
                ticket.getUserId(),
                ticket.getId(),
                "Your support ticket has been escalated to " + level.getDisplayName()
        );
    }
}

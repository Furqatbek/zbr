package com.fooddelivery.escalation.repository;

import com.fooddelivery.escalation.entity.EscalationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for escalation logs.
 */
@Repository
public interface EscalationLogRepository extends JpaRepository<EscalationLog, Long> {

    /**
     * Find escalation history for a ticket.
     */
    List<EscalationLog> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    /**
     * Count escalations in a time period.
     */
    @Query("SELECT COUNT(e) FROM EscalationLog e WHERE e.createdAt >= :since")
    Long countEscalationsSince(LocalDateTime since);

    /**
     * Get escalation statistics by level.
     */
    @Query("SELECT e.newLevel, COUNT(e) FROM EscalationLog e WHERE e.createdAt >= :since GROUP BY e.newLevel")
    List<Object[]> countByLevelSince(LocalDateTime since);
}

package com.fooddelivery.analytics.cx.repository;

import com.fooddelivery.analytics.cx.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for support tickets.
 */
@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    /**
     * Count tickets in period.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end")
    Long countTicketsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count open tickets.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED')")
    Long countOpenTickets();

    /**
     * Count tickets by status.
     */
    @Query("SELECT t.status, COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "GROUP BY t.status")
    List<Object[]> countByStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count tickets by type.
     */
    @Query("SELECT t.ticketType, COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "GROUP BY t.ticketType ORDER BY COUNT(t) DESC")
    List<Object[]> countByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count tickets by priority.
     */
    @Query("SELECT t.priority, COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "GROUP BY t.priority")
    List<Object[]> countByPriority(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count tickets by channel.
     */
    @Query("SELECT t.channel, COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "GROUP BY t.channel")
    List<Object[]> countByChannel(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get resolution time statistics.
     * Returns a list containing a single [avg, min, max, count] row.
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600), " +
           "MIN(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600), " +
           "MAX(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600), " +
           "COUNT(*) " +
           "FROM support_tickets WHERE created_at BETWEEN :start AND :end " +
           "AND resolved_at IS NOT NULL", nativeQuery = true)
    List<Object[]> getResolutionTimeStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get first response time statistics.
     * Returns a list containing a single [avg, min, max, count] row.
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (first_response_at - created_at)) / 60), " +
           "MIN(EXTRACT(EPOCH FROM (first_response_at - created_at)) / 60), " +
           "MAX(EXTRACT(EPOCH FROM (first_response_at - created_at)) / 60), " +
           "COUNT(*) " +
           "FROM support_tickets WHERE created_at BETWEEN :start AND :end " +
           "AND first_response_at IS NOT NULL", nativeQuery = true)
    List<Object[]> getFirstResponseTimeStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count SLA breaches.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "AND t.slaBreach = true")
    Long countSlaBreaches(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count tickets within SLA.
     */
    @Query(value = "SELECT COUNT(*) FROM support_tickets WHERE created_at BETWEEN :start AND :end " +
           "AND resolved_at IS NOT NULL " +
           "AND EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600 <= :slaHours", nativeQuery = true)
    Long countWithinSla(@Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("slaHours") int slaHours);

    /**
     * Count SLA breaches by type.
     */
    @Query("SELECT t.ticketType, COUNT(t) FROM SupportTicket t " +
           "WHERE t.createdAt BETWEEN :start AND :end AND t.slaBreach = true " +
           "GROUP BY t.ticketType")
    List<Object[]> countSlaBreachesByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count SLA breaches by priority.
     */
    @Query("SELECT t.priority, COUNT(t) FROM SupportTicket t " +
           "WHERE t.createdAt BETWEEN :start AND :end AND t.slaBreach = true " +
           "GROUP BY t.priority")
    List<Object[]> countSlaBreachesByPriority(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count currently breached open tickets.
     */
    @Query(value = "SELECT COUNT(*) FROM support_tickets WHERE status NOT IN ('RESOLVED', 'CLOSED') " +
           "AND EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - created_at)) / 3600 > :slaHours", nativeQuery = true)
    Long countCurrentlyBreached(@Param("slaHours") int slaHours);

    /**
     * Count tickets at risk of breach.
     */
    @Query(value = "SELECT COUNT(*) FROM support_tickets WHERE status NOT IN ('RESOLVED', 'CLOSED') " +
           "AND EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - created_at)) / 3600 BETWEEN :warningHours AND :slaHours", nativeQuery = true)
    Long countAtRiskOfBreach(@Param("warningHours") int warningHours, @Param("slaHours") int slaHours);

    /**
     * Get agent performance.
     */
    @Query(value = "SELECT assigned_to, COUNT(*), " +
           "SUM(CASE WHEN status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN resolved_at IS NOT NULL THEN EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600 ELSE NULL END), " +
           "AVG(CASE WHEN first_response_at IS NOT NULL THEN EXTRACT(EPOCH FROM (first_response_at - created_at)) / 60 ELSE NULL END), " +
           "AVG(customer_satisfaction_score), " +
           "SUM(CASE WHEN sla_breach = true THEN 1 ELSE 0 END) " +
           "FROM support_tickets WHERE created_at BETWEEN :start AND :end AND assigned_to IS NOT NULL " +
           "GROUP BY assigned_to ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> getAgentPerformance(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get daily ticket trend.
     */
    @Query(value = "SELECT DATE(created_at), " +
           "COUNT(*), " +
           "SUM(CASE WHEN resolved_at IS NOT NULL THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN resolved_at IS NOT NULL THEN EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600 ELSE NULL END) " +
           "FROM support_tickets WHERE created_at BETWEEN :start AND :end " +
           "GROUP BY DATE(created_at) ORDER BY DATE(created_at)", nativeQuery = true)
    List<Object[]> getDailyTicketTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get refund statistics.
     */
    @Query("SELECT COUNT(t), SUM(t.refundAmount), AVG(t.refundAmount) " +
           "FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end AND t.isRefunded = true")
    Object[] getRefundStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get CSAT statistics.
     */
    @Query("SELECT AVG(t.customerSatisfactionScore), COUNT(t.customerSatisfactionScore), " +
           "SUM(CASE WHEN t.customerSatisfactionScore >= 4 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.customerSatisfactionScore <= 2 THEN 1 ELSE 0 END) " +
           "FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "AND t.customerSatisfactionScore IS NOT NULL")
    Object[] getCsatStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get CSAT distribution.
     */
    @Query("SELECT t.customerSatisfactionScore, COUNT(t) FROM SupportTicket t " +
           "WHERE t.createdAt BETWEEN :start AND :end AND t.customerSatisfactionScore IS NOT NULL " +
           "GROUP BY t.customerSatisfactionScore ORDER BY t.customerSatisfactionScore")
    List<Object[]> getCsatDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count reopened tickets.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "AND t.reopenCount > 0")
    Long countReopenedTickets(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count escalated tickets.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "AND t.escalated = true")
    Long countEscalatedTickets(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get resolution time percentiles (approximation via ordering).
     */
    @Query(value = "SELECT EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600 " +
           "FROM support_tickets WHERE created_at BETWEEN :start AND :end " +
           "AND resolved_at IS NOT NULL ORDER BY (resolved_at - created_at)", nativeQuery = true)
    List<Double> getResolutionTimesOrdered(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get tickets by restaurant.
     */
    @Query("SELECT t.restaurantId, COUNT(t) FROM SupportTicket t " +
           "WHERE t.createdAt BETWEEN :start AND :end AND t.restaurantId IS NOT NULL " +
           "GROUP BY t.restaurantId ORDER BY COUNT(t) DESC")
    List<Object[]> getTicketsByRestaurant(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get tickets by courier.
     */
    @Query("SELECT t.courierId, COUNT(t) FROM SupportTicket t " +
           "WHERE t.createdAt BETWEEN :start AND :end AND t.courierId IS NOT NULL " +
           "GROUP BY t.courierId ORDER BY COUNT(t) DESC")
    List<Object[]> getTicketsByCourier(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count tickets by user since a date.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.userId = :userId AND t.createdAt > :since")
    Long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Find open tickets by status and SLA breach flag.
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.status IN :statuses AND t.slaBreach = :slaBreach")
    List<SupportTicket> findByStatusInAndSlaBreach(
            @Param("statuses") List<SupportTicket.TicketStatus> statuses,
            @Param("slaBreach") boolean slaBreach);

    /**
     * Find tickets eligible for auto-escalation.
     * Returns open tickets that have SLA breached or are high priority.
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.status NOT IN " +
           "(com.fooddelivery.analytics.cx.model.SupportTicket$TicketStatus.RESOLVED, " +
           "com.fooddelivery.analytics.cx.model.SupportTicket$TicketStatus.CLOSED) " +
           "AND (t.slaBreach = true OR t.priority = com.fooddelivery.analytics.cx.model.SupportTicket$TicketPriority.URGENT)")
    List<SupportTicket> findTicketsForAutoEscalation();
}

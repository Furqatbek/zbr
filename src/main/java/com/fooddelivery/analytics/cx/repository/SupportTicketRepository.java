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
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt)) / 3600), " +
           "MIN(EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt)) / 3600), " +
           "MAX(EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt)) / 3600), " +
           "COUNT(t) " +
           "FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "AND t.resolvedAt IS NOT NULL")
    Object[] getResolutionTimeStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get first response time statistics.
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (t.firstResponseAt - t.createdAt)) / 60), " +
           "MIN(EXTRACT(EPOCH FROM (t.firstResponseAt - t.createdAt)) / 60), " +
           "MAX(EXTRACT(EPOCH FROM (t.firstResponseAt - t.createdAt)) / 60), " +
           "COUNT(t) " +
           "FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "AND t.firstResponseAt IS NOT NULL")
    Object[] getFirstResponseTimeStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count SLA breaches.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "AND t.slaBreach = true")
    Long countSlaBreaches(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count tickets within SLA.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "AND t.resolvedAt IS NOT NULL " +
           "AND EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt)) / 3600 <= :slaHours")
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
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') " +
           "AND EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - t.createdAt)) / 3600 > :slaHours")
    Long countCurrentlyBreached(@Param("slaHours") int slaHours);

    /**
     * Count tickets at risk of breach.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') " +
           "AND EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - t.createdAt)) / 3600 BETWEEN :warningHours AND :slaHours")
    Long countAtRiskOfBreach(@Param("warningHours") int warningHours, @Param("slaHours") int slaHours);

    /**
     * Get agent performance.
     */
    @Query("SELECT t.assignedTo, COUNT(t), " +
           "SUM(CASE WHEN t.status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN t.resolvedAt IS NOT NULL THEN EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt)) / 3600 ELSE NULL END), " +
           "AVG(CASE WHEN t.firstResponseAt IS NOT NULL THEN EXTRACT(EPOCH FROM (t.firstResponseAt - t.createdAt)) / 60 ELSE NULL END), " +
           "AVG(t.customerSatisfactionScore), " +
           "SUM(CASE WHEN t.slaBreach = true THEN 1 ELSE 0 END) " +
           "FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end AND t.assignedTo IS NOT NULL " +
           "GROUP BY t.assignedTo ORDER BY COUNT(t) DESC")
    List<Object[]> getAgentPerformance(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get daily ticket trend.
     */
    @Query("SELECT CAST(t.createdAt AS date), " +
           "COUNT(t), " +
           "SUM(CASE WHEN t.resolvedAt IS NOT NULL THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status = 'CLOSED' THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN t.resolvedAt IS NOT NULL THEN EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt)) / 3600 ELSE NULL END) " +
           "FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "GROUP BY CAST(t.createdAt AS date) ORDER BY CAST(t.createdAt AS date)")
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
    @Query("SELECT EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt)) / 3600 " +
           "FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end " +
           "AND t.resolvedAt IS NOT NULL ORDER BY (t.resolvedAt - t.createdAt)")
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
}

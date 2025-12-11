package com.fooddelivery.admin.dashboard.repository;

import com.fooddelivery.analytics.cx.model.SupportTicket;
import com.fooddelivery.analytics.cx.model.SupportTicket.TicketPriority;
import com.fooddelivery.analytics.cx.model.SupportTicket.TicketStatus;
import com.fooddelivery.analytics.cx.model.SupportTicket.TicketType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for dashboard support ticket queries.
 * Provides optimized queries for support metrics and analytics.
 */
@Repository
public interface DashboardSupportRepository extends JpaRepository<SupportTicket, Long> {

    // ==================== Count Queries ====================

    /**
     * Count tickets by status.
     */
    Long countByStatus(TicketStatus status);

    /**
     * Count tickets by priority.
     */
    Long countByPriority(TicketPriority priority);

    /**
     * Count tickets by type.
     */
    Long countByTicketType(TicketType ticketType);

    /**
     * Count open tickets (not resolved or closed).
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED')")
    Long countOpenTickets();

    /**
     * Count tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Count resolved tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status IN ('RESOLVED', 'CLOSED') " +
            "AND t.resolvedAt BETWEEN :startDate AND :endDate")
    Long countResolvedByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Count escalated tickets.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.escalated = true " +
            "AND t.status NOT IN ('RESOLVED', 'CLOSED')")
    Long countEscalatedTickets();

    /**
     * Count SLA breached tickets.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.slaBreach = true " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countSlaBreachedTickets(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Count by status within date range.
     */
    @Query("SELECT t.status, COUNT(t) FROM SupportTicket t " +
            "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY t.status")
    List<Object[]> countByStatusAndDateRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Count by type within date range.
     */
    @Query("SELECT t.ticketType, COUNT(t) FROM SupportTicket t " +
            "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY t.ticketType")
    List<Object[]> countByTypeAndDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Count by channel within date range.
     */
    @Query("SELECT t.channel, COUNT(t) FROM SupportTicket t " +
            "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY t.channel")
    List<Object[]> countByChannelAndDateRange(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Count by priority within date range.
     */
    @Query("SELECT t.priority, COUNT(t) FROM SupportTicket t " +
            "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY t.priority")
    List<Object[]> countByPriorityAndDateRange(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // ==================== Performance Metrics ====================

    /**
     * Average resolution time in hours.
     */
    @Query("SELECT COALESCE(AVG(TIMESTAMPDIFF(HOUR, t.createdAt, t.resolvedAt)), 0) " +
            "FROM SupportTicket t WHERE t.resolvedAt IS NOT NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Double avgResolutionTimeHours(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Average first response time in minutes.
     */
    @Query("SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, t.createdAt, t.firstResponseAt)), 0) " +
            "FROM SupportTicket t WHERE t.firstResponseAt IS NOT NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Double avgFirstResponseTimeMinutes(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Average CSAT score.
     */
    @Query("SELECT COALESCE(AVG(t.customerSatisfactionScore), 0) FROM SupportTicket t " +
            "WHERE t.customerSatisfactionScore IS NOT NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Double avgCsatScore(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);

    // ==================== Refund Queries ====================

    /**
     * Count refund requests within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType = 'REFUND_REQUEST' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countRefundRequests(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);

    /**
     * Count approved refunds.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.isRefunded = true " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countApprovedRefunds(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Total refund amount.
     */
    @Query("SELECT COALESCE(SUM(t.refundAmount), 0) FROM SupportTicket t " +
            "WHERE t.isRefunded = true " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumRefundAmount(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    // ==================== List Queries ====================

    /**
     * Find open tickets ordered by priority and age.
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') " +
            "ORDER BY t.priority DESC, t.createdAt ASC")
    Page<SupportTicket> findOpenTickets(Pageable pageable);

    /**
     * Find tickets with filters.
     */
    @Query("SELECT t FROM SupportTicket t WHERE " +
            "(:status IS NULL OR t.status = :status) " +
            "AND (:type IS NULL OR t.ticketType = :type) " +
            "AND (:priority IS NULL OR t.priority = :priority) " +
            "AND (:assignedTo IS NULL OR t.assignedTo = :assignedTo) " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY t.createdAt DESC")
    Page<SupportTicket> findWithFilters(@Param("status") TicketStatus status,
                                         @Param("type") TicketType type,
                                         @Param("priority") TicketPriority priority,
                                         @Param("assignedTo") Long assignedTo,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         Pageable pageable);

    /**
     * Find urgent and high priority open tickets.
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') " +
            "AND t.priority IN ('URGENT', 'HIGH') " +
            "ORDER BY t.priority DESC, t.createdAt ASC")
    List<SupportTicket> findUrgentTickets();

    /**
     * Find SLA breached tickets.
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.slaBreach = true " +
            "AND t.status NOT IN ('RESOLVED', 'CLOSED') " +
            "ORDER BY t.createdAt ASC")
    List<SupportTicket> findSlaBreachedTickets();

    // ==================== Agent Performance ====================

    /**
     * Agent performance metrics.
     */
    @Query("SELECT t.assignedTo, COUNT(t), " +
            "SUM(CASE WHEN t.status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END), " +
            "COALESCE(AVG(TIMESTAMPDIFF(HOUR, t.createdAt, t.resolvedAt)), 0), " +
            "COALESCE(AVG(TIMESTAMPDIFF(MINUTE, t.createdAt, t.firstResponseAt)), 0), " +
            "COALESCE(AVG(t.customerSatisfactionScore), 0) " +
            "FROM SupportTicket t WHERE t.assignedTo IS NOT NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY t.assignedTo")
    List<Object[]> getAgentPerformanceMetrics(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // ==================== Hourly Distribution ====================

    /**
     * Ticket count by hour.
     */
    @Query("SELECT HOUR(t.createdAt), COUNT(t) FROM SupportTicket t " +
            "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY HOUR(t.createdAt) " +
            "ORDER BY HOUR(t.createdAt)")
    List<Object[]> ticketCountByHour(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
}

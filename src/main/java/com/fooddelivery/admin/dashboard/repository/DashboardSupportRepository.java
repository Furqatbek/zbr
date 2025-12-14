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
     * Count open tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countOpenTickets(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Count total tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Long countTotalTickets(@Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);

    /**
     * Count in-progress tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = 'IN_PROGRESS' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countInProgressTickets(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Count resolved tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = 'RESOLVED' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countResolvedTickets(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Count closed tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = 'CLOSED' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countClosedTickets(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    /**
     * Count escalated tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.escalated = true " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countEscalatedTickets(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Count complaints within date range (negative experience tickets).
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType IN " +
            "('LATE_DELIVERY', 'MISSING_ITEMS', 'WRONG_ORDER', 'COLD_FOOD', 'COURIER_BEHAVIOR', 'RESTAURANT_QUALITY') " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countComplaints(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Count inquiries within date range (information-seeking tickets).
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType IN " +
            "('APP_ISSUE', 'ACCOUNT_ISSUE', 'PROMO_CODE_ISSUE') " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countInquiries(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);

    /**
     * Count feedback within date range (general tickets).
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType = 'OTHER' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countFeedback(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

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
     * Average resolution time in minutes.
     */
    @Query("SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, t.createdAt, t.resolvedAt)), 0) " +
            "FROM SupportTicket t WHERE t.resolvedAt IS NOT NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Double avgResolutionTimeMinutes(@Param("startDate") LocalDateTime startDate,
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

    /**
     * Average customer satisfaction score.
     */
    @Query("SELECT COALESCE(AVG(t.customerSatisfactionScore), 0) FROM SupportTicket t " +
            "WHERE t.customerSatisfactionScore IS NOT NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Double avgCustomerSatisfactionScore(@Param("startDate") LocalDateTime startDate,
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

    // ==================== Ticket Details ====================

    /**
     * Find ticket details for dashboard display.
     */
    @Query("SELECT t.id, t.ticketNumber, t.userId, " +
            "t.orderId, t.ticketType, t.priority, t.status, t.subject, " +
            "t.createdAt, t.updatedAt, t.assignedTo, " +
            "t.firstResponseAt, t.resolvedAt, t.escalated, t.refundAmount " +
            "FROM SupportTicket t " +
            "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY t.createdAt DESC")
    List<Object[]> findTicketDetails(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    // ==================== Additional Queries for Collectors ====================

    /**
     * Count waiting customer tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = 'WAITING_CUSTOMER' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countWaitingCustomerTickets(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Count by priority within date range (returns list for breakdown).
     */
    @Query(value = "SELECT t.priority, COUNT(*) FROM support_tickets t " +
            "WHERE t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY t.priority", nativeQuery = true)
    List<Object[]> countByPriority(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Count by category/type within date range.
     */
    @Query(value = "SELECT t.ticket_type, COUNT(*) FROM support_tickets t " +
            "WHERE t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY t.ticket_type", nativeQuery = true)
    List<Object[]> countByCategory(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Count by specific category within date range.
     */
    @Query(value = "SELECT COUNT(*) FROM support_tickets t WHERE t.ticket_type = :category " +
            "AND t.created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    Long countByCategory(@Param("category") String category,
                          @Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Get agent performance with pagination.
     */
    @Query("SELECT t.assignedTo, " +
            "COUNT(t), " +
            "SUM(CASE WHEN t.status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END), " +
            "COALESCE(AVG(TIMESTAMPDIFF(MINUTE, t.createdAt, t.resolvedAt)), 0), " +
            "COALESCE(AVG(TIMESTAMPDIFF(MINUTE, t.createdAt, t.firstResponseAt)), 0), " +
            "COALESCE(AVG(t.customerSatisfactionScore), 0), " +
            "SUM(CASE WHEN t.status NOT IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END) " +
            "FROM SupportTicket t " +
            "WHERE t.assignedTo IS NOT NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY t.assignedTo " +
            "ORDER BY COUNT(t) DESC")
    List<Object[]> getAgentPerformance(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);

    /**
     * Get common issues by category.
     */
    @Query(value = "SELECT t.ticket_type, t.subject, COUNT(*), " +
            "COALESCE(AVG(EXTRACT(EPOCH FROM (t.resolved_at - t.created_at)) / 60), 0) " +
            "FROM support_tickets t " +
            "WHERE t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY t.ticket_type, t.subject " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> getCommonIssues(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    Pageable pageable);

    /**
     * Get hourly ticket distribution.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM t.created_at), COUNT(*) FROM support_tickets t " +
            "WHERE t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY EXTRACT(HOUR FROM t.created_at) " +
            "ORDER BY EXTRACT(HOUR FROM t.created_at)", nativeQuery = true)
    List<Object[]> getHourlyDistribution(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Get SLA compliance rate by priority.
     */
    @Query(value = "SELECT COALESCE(" +
            "CAST(SUM(CASE WHEN t.sla_breach = false THEN 1 ELSE 0 END) AS double precision) / " +
            "NULLIF(CAST(COUNT(*) AS double precision), 0) * 100, 100.0) " +
            "FROM support_tickets t WHERE t.priority = :priority " +
            "AND t.created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    Double getSlaComplianceByPriority(@Param("priority") String priority,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find pending attention tickets (open, high priority, or SLA breached).
     */
    @Query("SELECT t.id, t.ticketNumber, t.userId, " +
            "t.orderId, t.ticketType, t.priority, t.status, t.subject, " +
            "t.createdAt, t.updatedAt, t.assignedTo, " +
            "t.firstResponseAt, t.resolvedAt, t.escalated, t.refundAmount " +
            "FROM SupportTicket t " +
            "WHERE t.status NOT IN ('RESOLVED', 'CLOSED') " +
            "AND (t.priority IN ('URGENT', 'HIGH') OR t.slaBreach = true) " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY t.priority DESC, t.createdAt ASC")
    List<Object[]> findPendingAttentionTickets(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);

    /**
     * Calculate total refunds within date range.
     */
    @Query("SELECT COALESCE(SUM(t.refundAmount), 0) FROM SupportTicket t " +
            "WHERE t.isRefunded = true " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalRefunds(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Count pending refunds.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType = 'REFUND_REQUEST' " +
            "AND t.status NOT IN ('RESOLVED', 'CLOSED') " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countPendingRefunds(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);

    /**
     * Count rejected refunds.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType = 'REFUND_REQUEST' " +
            "AND t.isRefunded = false AND t.status IN ('RESOLVED', 'CLOSED') " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countRejectedRefunds(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Get refunds breakdown by ticket type (for refund-related tickets).
     */
    @Query(value = "SELECT t.ticket_type, COUNT(*) FROM support_tickets t " +
            "WHERE t.ticket_type IN ('REFUND_REQUEST', 'PAYMENT_ISSUE', 'MISSING_ITEMS', 'WRONG_ORDER') " +
            "AND t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY t.ticket_type", nativeQuery = true)
    List<Object[]> getRefundsByReason(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
}

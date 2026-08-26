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
    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600), 0) " +
            "FROM support_tickets WHERE resolved_at IS NOT NULL " +
            "AND created_at BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    Double avgResolutionTimeHours(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Average first response time in minutes.
     */
    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (first_response_at - created_at)) / 60), 0) " +
            "FROM support_tickets WHERE first_response_at IS NOT NULL " +
            "AND created_at BETWEEN :startDate AND :endDate",
            nativeQuery = true)
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
    @Query(value = "SELECT assigned_to, COUNT(*), " +
            "SUM(CASE WHEN status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END), " +
            "COALESCE(AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600), 0), " +
            "COALESCE(AVG(EXTRACT(EPOCH FROM (first_response_at - created_at)) / 60), 0), " +
            "COALESCE(AVG(customer_satisfaction_score), 0) " +
            "FROM support_tickets WHERE assigned_to IS NOT NULL " +
            "AND created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY assigned_to",
            nativeQuery = true)
    List<Object[]> getAgentPerformanceMetrics(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // ==================== Hourly Distribution ====================

    /**
     * Ticket count by hour.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM business_ts(created_at)), COUNT(*) FROM support_tickets " +
            "WHERE created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY EXTRACT(HOUR FROM business_ts(created_at)) " +
            "ORDER BY EXTRACT(HOUR FROM business_ts(created_at))",
            nativeQuery = true)
    List<Object[]> ticketCountByHour(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // ==================== SupportCollector Required Methods ====================

    /**
     * Count total tickets within date range.
     */
    default Long countTotalTickets(LocalDateTime startDate, LocalDateTime endDate) {
        return countByDateRange(startDate, endDate);
    }

    /**
     * Count open tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = 'OPEN' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countOpenTickets(@Param("startDate") LocalDateTime startDate,
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
     * Count waiting for customer tickets within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = 'WAITING_CUSTOMER' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countWaitingCustomerTickets(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Count complaints (OTHER type tickets) within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType = 'OTHER' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countComplaints(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);

    /**
     * Count inquiries (APP_ISSUE type tickets) within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType = 'APP_ISSUE' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countInquiries(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

    /**
     * Count feedback (RESTAURANT_QUALITY type tickets) within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType = 'RESTAURANT_QUALITY' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countFeedback(@Param("startDate") LocalDateTime startDate,
                       @Param("endDate") LocalDateTime endDate);

    /**
     * Average resolution time in minutes.
     */
    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 60), 0) " +
            "FROM support_tickets WHERE resolved_at IS NOT NULL " +
            "AND created_at BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    Double avgResolutionTimeMinutes(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Average customer satisfaction score.
     */
    default Double avgCustomerSatisfactionScore(LocalDateTime startDate, LocalDateTime endDate) {
        return avgCsatScore(startDate, endDate);
    }

    /**
     * Count by priority within date range.
     */
    default List<Object[]> countByPriority(LocalDateTime startDate, LocalDateTime endDate) {
        return countByPriorityAndDateRange(startDate, endDate);
    }

    /**
     * Count by ticket type within date range (category not available on entity).
     */
    default List<Object[]> countByCategory(LocalDateTime startDate, LocalDateTime endDate) {
        return countByTypeAndDateRange(startDate, endDate);
    }

    /**
     * Count by specific ticket type within date range.
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.ticketType = :ticketType " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Long countByTicketType(@Param("ticketType") TicketType ticketType,
                           @Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);

    /**
     * Get agent performance with pagination.
     */
    @Query(value = "SELECT t.assigned_to, " +
            "COALESCE((SELECT u.first_name || ' ' || u.last_name FROM users u WHERE u.id = t.assigned_to), 'Unassigned'), " +
            "COUNT(*), " +
            "SUM(CASE WHEN t.status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END), " +
            "COALESCE(AVG(EXTRACT(EPOCH FROM (t.resolved_at - t.created_at)) / 60), 0), " +
            "COALESCE(AVG(EXTRACT(EPOCH FROM (t.first_response_at - t.created_at)) / 60), 0), " +
            "COALESCE(AVG(t.customer_satisfaction_score), 0), " +
            "SUM(CASE WHEN t.status NOT IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END) " +
            "FROM support_tickets t WHERE t.assigned_to IS NOT NULL " +
            "AND t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY t.assigned_to",
            nativeQuery = true)
    List<Object[]> getAgentPerformance(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    /**
     * Get common issues by ticket type with limit.
     */
    @Query(value = "SELECT ticket_type, ticket_type, COUNT(*), " +
            "COALESCE(AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 60), 0) " +
            "FROM support_tickets WHERE created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY ticket_type " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> getCommonIssues(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   @Param("limit") int limit);

    /**
     * Get hourly distribution.
     */
    default List<Object[]> getHourlyDistribution(LocalDateTime startDate, LocalDateTime endDate) {
        return ticketCountByHour(startDate, endDate);
    }

    /**
     * Get SLA compliance by priority.
     */
    @Query("SELECT CASE WHEN COUNT(t) = 0 THEN 100.0 " +
            "ELSE (SUM(CASE WHEN t.slaBreach = false THEN 1 ELSE 0 END) * 100.0 / COUNT(t)) END " +
            "FROM SupportTicket t WHERE t.priority = :priority " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    Double getSlaComplianceByPriority(@Param("priority") TicketPriority priority,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Find ticket details for dashboard.
     */
    @Query(value = "SELECT t.id, t.ticket_number, t.user_id, " +
            "COALESCE((SELECT u.first_name || ' ' || u.last_name FROM users u WHERE u.id = t.user_id), 'Unknown'), " +
            "t.order_id, t.ticket_type, t.priority, t.status, t.subject, t.created_at, t.updated_at, " +
            "t.assigned_to, " +
            "COALESCE((SELECT u.first_name || ' ' || u.last_name FROM users u WHERE u.id = t.assigned_to), 'Unassigned'), " +
            "t.first_response_at, t.resolved_at, t.escalated, t.refund_amount " +
            "FROM support_tickets t WHERE t.created_at BETWEEN :startDate AND :endDate " +
            "ORDER BY t.created_at DESC",
            nativeQuery = true)
    List<Object[]> findTicketDetails(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);

    /**
     * Find pending attention tickets.
     */
    @Query(value = "SELECT t.id, t.ticket_number, t.user_id, " +
            "COALESCE((SELECT u.first_name || ' ' || u.last_name FROM users u WHERE u.id = t.user_id), 'Unknown'), " +
            "t.order_id, t.ticket_type, t.priority, t.status, t.subject, t.created_at, t.updated_at, " +
            "t.assigned_to, " +
            "COALESCE((SELECT u.first_name || ' ' || u.last_name FROM users u WHERE u.id = t.assigned_to), 'Unassigned'), " +
            "t.first_response_at, t.resolved_at, t.escalated, t.refund_amount " +
            "FROM support_tickets t WHERE (t.status = 'OPEN' OR t.priority IN ('URGENT', 'HIGH') OR t.sla_breach = true) " +
            "AND t.created_at BETWEEN :startDate AND :endDate " +
            "ORDER BY t.priority DESC, t.created_at ASC",
            nativeQuery = true)
    List<Object[]> findPendingAttentionTickets(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable);

    /**
     * Calculate total refunds.
     */
    default BigDecimal calculateTotalRefunds(LocalDateTime startDate, LocalDateTime endDate) {
        return sumRefundAmount(startDate, endDate);
    }

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
     * Get refunds by ticket type (refundReason not available on entity).
     */
    @Query("SELECT t.ticketType, COUNT(t) FROM SupportTicket t " +
            "WHERE t.ticketType = 'REFUND_REQUEST' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY t.ticketType")
    List<Object[]> getRefundsByReason(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
}

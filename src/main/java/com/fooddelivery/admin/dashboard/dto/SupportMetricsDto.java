package com.fooddelivery.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for customer support metrics panel.
 * Provides support ticket analytics and agent performance metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportMetricsDto {

    // Ticket counts
    private Long totalTickets;
    private Long openTickets;
    private Long inProgressTickets;
    private Long waitingCustomerTickets;
    private Long waitingRestaurantTickets;
    private Long waitingCourierTickets;
    private Long resolvedToday;
    private Long closedToday;
    private Long resolvedTickets;
    private Long closedTickets;

    // Type breakdown counts
    private Long complaintCount;
    private Long refundRequestCount;
    private Long inquiryCount;
    private Long feedbackCount;

    // Priority breakdown
    private Long urgentTickets;
    private Long highPriorityTickets;
    private Long mediumPriorityTickets;
    private Long lowPriorityTickets;

    // Performance metrics
    private Double avgResolutionTimeHours;
    private Double avgResolutionTimeMinutes;
    private String avgResolutionTimeFormatted;
    private Double avgFirstResponseTimeMinutes;
    private String avgFirstResponseTimeFormatted;
    private Double firstContactResolutionRate;
    private Double customerSatisfactionScore; // CSAT average
    private Double resolutionRate;

    // SLA compliance
    private Long slaBreach;
    private Double slaComplianceRate;

    // Escalations
    private Long escalatedTickets;
    private Long reopenedTickets;

    // Refund requests
    private Long refundRequestsTotal;
    private Long refundRequestsPending;
    private Long refundRequestsApproved;
    private Long refundRequestsDenied;
    private BigDecimal totalRefundAmount;

    // By ticket type
    private Map<String, Long> ticketsByType;

    // By channel
    private Map<String, Long> ticketsByChannel;

    // Hourly ticket volume
    private Map<Integer, Long> ticketsByHour;

    // Ticket list
    private List<SupportTicketItemDto> tickets;

    // Agent performance
    private List<AgentPerformanceDto> agentPerformance;

    // Common issues
    private List<CommonIssueDto> commonIssues;

    // Pagination
    private Integer currentPage;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportTicketItemDto {
        private Long ticketId;
        private String ticketNumber;
        private String ticketType;
        private String category;
        private String status;
        private String priority;
        private String channel;

        // Related entities
        private Long userId;
        private String userName;
        private Long customerId;
        private String customerName;
        private Long orderId;
        private String orderNumber;
        private Long restaurantId;
        private String restaurantName;
        private Long courierId;
        private String courierName;

        // Content
        private String subject;
        private String description;

        // Assignment
        private Long assignedToId;
        private String assignedToName;
        private Long assignedAgentId;
        private String assignedAgentName;
        private LocalDateTime assignedAt;

        // Timing
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime firstResponseAt;
        private LocalDateTime resolvedAt;
        private Long ageMinutes;
        private String ageFormatted;
        private Long responseTimeMinutes;
        private Long waitTimeMinutes;
        private String waitTimeFormatted;

        // Flags
        private Boolean slaBreach;
        private Boolean slaBreached;
        private Boolean escalated;
        private Boolean isEscalated;
        private Integer reopenCount;

        // Refund info
        private Boolean isRefundRequest;
        private BigDecimal refundAmount;
        private Boolean refundApproved;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentPerformanceDto {
        private Long agentId;
        private String agentName;
        private Long ticketsAssigned;
        private Long ticketsResolved;
        private Double avgResolutionTimeHours;
        private Double avgFirstResponseTimeMinutes;
        private Double csatScore;
        private Double slaComplianceRate;
        private Long currentOpenTickets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommonIssueDto {
        private String issueType;
        private Long ticketCount;
        private Double percentageOfTotal;
        private Double avgResolutionTimeHours;
        private BigDecimal avgRefundAmount;
        private String suggestedAction;
    }
}

package com.fooddelivery.analytics.cx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for individual support ticket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketResponseDto {

    private Long id;
    private String ticketNumber;
    private Long userId;
    private Long orderId;
    private String typeName;
    private String statusName;
    private String priorityName;
    private String channelName;
    private String subject;
    private String description;
    private Long assignedAgentId;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private Double resolutionTimeHours;
    private Double firstResponseTimeMinutes;
    private Boolean isEscalated;
    private Boolean isReopened;
    private Integer csatScore;
    private Boolean refundIssued;
    private BigDecimal refundAmount;
    private Boolean isOpen;
    private LocalDateTime createdAt;
}

package com.fooddelivery.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenTicketDto {
    private String ticketNumber;
    private Long orderId;
    private String externalOrderNo;
    private Long restaurantId;
    private String orderType;
    private String tableId;
    private List<TicketItem> items;
    private String notes;
    private Integer estimatedPrepTime;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketItem {
        private String name;
        private Integer quantity;
        private String variant;
        private String modifiers;
        private String specialInstructions;
    }
}

package com.fooddelivery.order.event;

import com.fooddelivery.common.event.DomainEvent;
import com.fooddelivery.order.entity.OrderStatus;
import lombok.Getter;

/**
 * Event published when order status changes.
 */
@Getter
public class OrderStatusChangedEvent extends DomainEvent {

    private final Long orderId;
    private final String externalOrderNo;
    private final Long restaurantId;
    private final Long consumerId;
    private final Long courierId;
    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;
    private final String reason;

    public OrderStatusChangedEvent(Long orderId, String externalOrderNo, Long restaurantId,
                                    Long consumerId, Long courierId, OrderStatus previousStatus,
                                    OrderStatus newStatus, String reason) {
        super();
        this.orderId = orderId;
        this.externalOrderNo = externalOrderNo;
        this.restaurantId = restaurantId;
        this.consumerId = consumerId;
        this.courierId = courierId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    @Override
    public String getAggregateId() {
        return orderId.toString();
    }

    @Override
    public String getAggregateType() {
        return "Order";
    }
}

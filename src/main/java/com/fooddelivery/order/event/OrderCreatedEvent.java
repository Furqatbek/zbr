package com.fooddelivery.order.event;

import com.fooddelivery.common.event.DomainEvent;
import com.fooddelivery.order.entity.OrderType;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Event published when a new order is created.
 */
@Getter
public class OrderCreatedEvent extends DomainEvent {

    private final Long orderId;
    private final String externalOrderNo;
    private final Long consumerId;
    private final Long restaurantId;
    private final OrderType orderType;
    private final BigDecimal total;
    private final String deliveryAddress;

    public OrderCreatedEvent(Long orderId, String externalOrderNo, Long consumerId,
                              Long restaurantId, OrderType orderType, BigDecimal total,
                              String deliveryAddress) {
        super();
        this.orderId = orderId;
        this.externalOrderNo = externalOrderNo;
        this.consumerId = consumerId;
        this.restaurantId = restaurantId;
        this.orderType = orderType;
        this.total = total;
        this.deliveryAddress = deliveryAddress;
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

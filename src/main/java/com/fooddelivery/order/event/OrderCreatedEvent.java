package com.fooddelivery.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fooddelivery.common.event.DomainEvent;
import com.fooddelivery.order.entity.OrderType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when a new order is created.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonCreator
    public OrderCreatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("occurredAt") LocalDateTime occurredAt,
            @JsonProperty("version") int version,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("externalOrderNo") String externalOrderNo,
            @JsonProperty("consumerId") Long consumerId,
            @JsonProperty("restaurantId") Long restaurantId,
            @JsonProperty("orderType") OrderType orderType,
            @JsonProperty("total") BigDecimal total,
            @JsonProperty("deliveryAddress") String deliveryAddress) {
        super(eventId, eventType, occurredAt, version);
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

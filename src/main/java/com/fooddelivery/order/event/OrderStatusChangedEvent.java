package com.fooddelivery.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fooddelivery.common.event.DomainEvent;
import com.fooddelivery.order.entity.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when order status changes.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderStatusChangedEvent extends DomainEvent {

    private final Long orderId;
    private final String externalOrderNo;
    private final Long restaurantId;
    private final Long consumerId;
    private final Long courierId;
    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;
    private final String reason;

    /**
     * The partner whose system caused this change, when one did.
     *
     * <p>Null means the change originated here — a tap in the vendor app, a
     * courier, a scheduler. That distinction is what stops us reporting a
     * partner's own kitchen state back to the kitchen that just set it, while
     * still telling them about one a restaurant set on our tablet.
     */
    private final Long reportedByPartnerId;

    public OrderStatusChangedEvent(Long orderId, String externalOrderNo, Long restaurantId,
                                    Long consumerId, Long courierId, OrderStatus previousStatus,
                                    OrderStatus newStatus, String reason) {
        this(orderId, externalOrderNo, restaurantId, consumerId, courierId,
                previousStatus, newStatus, reason, null);
    }

    public OrderStatusChangedEvent(Long orderId, String externalOrderNo, Long restaurantId,
                                    Long consumerId, Long courierId, OrderStatus previousStatus,
                                    OrderStatus newStatus, String reason, Long reportedByPartnerId) {
        super();
        this.reportedByPartnerId = reportedByPartnerId;
        this.orderId = orderId;
        this.externalOrderNo = externalOrderNo;
        this.restaurantId = restaurantId;
        this.consumerId = consumerId;
        this.courierId = courierId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    @JsonCreator
    public OrderStatusChangedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("occurredAt") LocalDateTime occurredAt,
            @JsonProperty("version") int version,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("externalOrderNo") String externalOrderNo,
            @JsonProperty("restaurantId") Long restaurantId,
            @JsonProperty("consumerId") Long consumerId,
            @JsonProperty("courierId") Long courierId,
            @JsonProperty("previousStatus") OrderStatus previousStatus,
            @JsonProperty("newStatus") OrderStatus newStatus,
            @JsonProperty("reason") String reason,
            @JsonProperty("reportedByPartnerId") Long reportedByPartnerId) {
        super(eventId, eventType, occurredAt, version);
        this.reportedByPartnerId = reportedByPartnerId;
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

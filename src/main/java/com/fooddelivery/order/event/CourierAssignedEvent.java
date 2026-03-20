package com.fooddelivery.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fooddelivery.common.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when a courier is assigned to an order.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourierAssignedEvent extends DomainEvent {

    private final Long orderId;
    private final String externalOrderNo;
    private final Long courierId;
    private final Long restaurantId;
    private final String pickupAddress;
    private final String deliveryAddress;
    private final BigDecimal pickupLat;
    private final BigDecimal pickupLng;
    private final BigDecimal deliveryLat;
    private final BigDecimal deliveryLng;

    public CourierAssignedEvent(Long orderId, String externalOrderNo, Long courierId,
                                 Long restaurantId, String pickupAddress, String deliveryAddress,
                                 BigDecimal pickupLat, BigDecimal pickupLng,
                                 BigDecimal deliveryLat, BigDecimal deliveryLng) {
        super();
        this.orderId = orderId;
        this.externalOrderNo = externalOrderNo;
        this.courierId = courierId;
        this.restaurantId = restaurantId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.deliveryLat = deliveryLat;
        this.deliveryLng = deliveryLng;
    }

    @JsonCreator
    public CourierAssignedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("occurredAt") LocalDateTime occurredAt,
            @JsonProperty("version") int version,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("externalOrderNo") String externalOrderNo,
            @JsonProperty("courierId") Long courierId,
            @JsonProperty("restaurantId") Long restaurantId,
            @JsonProperty("pickupAddress") String pickupAddress,
            @JsonProperty("deliveryAddress") String deliveryAddress,
            @JsonProperty("pickupLat") BigDecimal pickupLat,
            @JsonProperty("pickupLng") BigDecimal pickupLng,
            @JsonProperty("deliveryLat") BigDecimal deliveryLat,
            @JsonProperty("deliveryLng") BigDecimal deliveryLng) {
        super(eventId, eventType, occurredAt, version);
        this.orderId = orderId;
        this.externalOrderNo = externalOrderNo;
        this.courierId = courierId;
        this.restaurantId = restaurantId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.deliveryLat = deliveryLat;
        this.deliveryLng = deliveryLng;
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

package com.fooddelivery.order.event;

import com.fooddelivery.common.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Event published when a courier is assigned to an order.
 */
@Getter
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

    @Override
    public String getAggregateId() {
        return orderId.toString();
    }

    @Override
    public String getAggregateType() {
        return "Order";
    }
}

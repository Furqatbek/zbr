package com.fooddelivery.notification.event;

import com.fooddelivery.common.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Event published when a payment fails.
 */
@Getter
public class PaymentFailedEvent extends DomainEvent {

    private final Long paymentId;
    private final Long orderId;
    private final String externalOrderNo;
    private final Long consumerId;
    private final Long restaurantId;
    private final BigDecimal amount;
    private final String failureReason;
    private final String failureCode;
    private final String provider;

    public PaymentFailedEvent(Long paymentId, Long orderId, String externalOrderNo,
                               Long consumerId, Long restaurantId, BigDecimal amount,
                               String failureReason, String failureCode, String provider) {
        super();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.externalOrderNo = externalOrderNo;
        this.consumerId = consumerId;
        this.restaurantId = restaurantId;
        this.amount = amount;
        this.failureReason = failureReason;
        this.failureCode = failureCode;
        this.provider = provider;
    }

    @Override
    public String getAggregateId() {
        return paymentId != null ? paymentId.toString() : orderId.toString();
    }

    @Override
    public String getAggregateType() {
        return "Payment";
    }
}

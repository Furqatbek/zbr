package com.fooddelivery.order.event;

import com.fooddelivery.common.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Event published when payment is confirmed.
 */
@Getter
public class PaymentConfirmedEvent extends DomainEvent {

    private final Long paymentId;
    private final Long orderId;
    private final String externalOrderNo;
    private final Long consumerId;
    private final Long restaurantId;
    private final BigDecimal amount;
    private final String provider;
    private final String providerPaymentId;

    public PaymentConfirmedEvent(Long paymentId, Long orderId, String externalOrderNo,
                                  Long consumerId, Long restaurantId, BigDecimal amount,
                                  String provider, String providerPaymentId) {
        super();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.externalOrderNo = externalOrderNo;
        this.consumerId = consumerId;
        this.restaurantId = restaurantId;
        this.amount = amount;
        this.provider = provider;
        this.providerPaymentId = providerPaymentId;
    }

    @Override
    public String getAggregateId() {
        return paymentId.toString();
    }

    @Override
    public String getAggregateType() {
        return "Payment";
    }
}

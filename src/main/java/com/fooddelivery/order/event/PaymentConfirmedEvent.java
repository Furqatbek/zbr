package com.fooddelivery.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fooddelivery.common.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when payment is confirmed.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonCreator
    public PaymentConfirmedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("occurredAt") LocalDateTime occurredAt,
            @JsonProperty("version") int version,
            @JsonProperty("paymentId") Long paymentId,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("externalOrderNo") String externalOrderNo,
            @JsonProperty("consumerId") Long consumerId,
            @JsonProperty("restaurantId") Long restaurantId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("provider") String provider,
            @JsonProperty("providerPaymentId") String providerPaymentId) {
        super(eventId, eventType, occurredAt, version);
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

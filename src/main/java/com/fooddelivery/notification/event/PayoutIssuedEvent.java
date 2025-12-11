package com.fooddelivery.notification.event;

import com.fooddelivery.common.event.DomainEvent;
import com.fooddelivery.notification.model.NotificationRole;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Event published when a payout is issued to a restaurant or courier.
 */
@Getter
public class PayoutIssuedEvent extends DomainEvent {

    private final Long payoutId;
    private final Long recipientId;
    private final NotificationRole recipientRole;
    private final BigDecimal amount;
    private final String currency;
    private final String payoutMethod;
    private final String periodStart;
    private final String periodEnd;
    private final Map<String, Object> metadata;

    public PayoutIssuedEvent(Long payoutId, Long recipientId, NotificationRole recipientRole,
                              BigDecimal amount, String currency, String payoutMethod,
                              String periodStart, String periodEnd, Map<String, Object> metadata) {
        super();
        this.payoutId = payoutId;
        this.recipientId = recipientId;
        this.recipientRole = recipientRole;
        this.amount = amount;
        this.currency = currency;
        this.payoutMethod = payoutMethod;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.metadata = metadata;
    }

    @Override
    public String getAggregateId() {
        return payoutId.toString();
    }

    @Override
    public String getAggregateType() {
        return "Payout";
    }
}

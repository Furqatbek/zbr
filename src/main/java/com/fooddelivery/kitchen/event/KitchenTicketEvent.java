package com.fooddelivery.kitchen.event;

import com.fooddelivery.common.event.DomainEvent;
import com.fooddelivery.kitchen.dto.KitchenTicketDto;
import lombok.Getter;

@Getter
public class KitchenTicketEvent extends DomainEvent {
    private final KitchenTicketDto ticket;

    public KitchenTicketEvent(KitchenTicketDto ticket) {
        super();
        this.ticket = ticket;
    }

    @Override
    public String getAggregateId() {
        return ticket.getOrderId().toString();
    }

    @Override
    public String getAggregateType() {
        return "KitchenTicket";
    }
}

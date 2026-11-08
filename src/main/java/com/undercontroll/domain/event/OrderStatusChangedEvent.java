package com.undercontroll.domain.event;

import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;

import java.time.Instant;

public record OrderStatusChangedEvent(
        Integer orderId,
        OrderStatus from,
        OrderStatus to,
        OrderOutcome outcome,
        OrderActor actor,
        Instant occurredAt
) {
}

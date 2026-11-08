package com.undercontroll.domain.model;

import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;

import java.time.Instant;

public record OrderStatusEvent(
        Long id,
        Integer orderId,
        OrderStatus from,
        OrderStatus to,
        OrderOutcome outcome,
        OrderActor actor,
        String note,
        Instant createdAt
) {
}

package com.undercontroll.application.dto.order;

import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;

import java.time.Instant;

public record OrderStatusEventDto(
        OrderStatus from,
        OrderStatus to,
        OrderOutcome outcome,
        OrderActor actor,
        String note,
        Instant createdAt
) {
}

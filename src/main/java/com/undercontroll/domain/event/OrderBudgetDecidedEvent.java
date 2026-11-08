package com.undercontroll.domain.event;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;

import java.time.Instant;

public record OrderBudgetDecidedEvent(
        Integer orderId,
        ApprovalDecision decision,
        OrderActor actor,
        Double budgetTotal,
        Instant occurredAt
) {
}

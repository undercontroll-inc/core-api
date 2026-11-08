package com.undercontroll.application.dto.order;

import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record TransitionOrderRequest(
        OrderStatus to,
        @Schema(description = "Only when moving to READY_FOR_PICKUP") OrderOutcome outcome,
        @Schema(description = "Up to 500 characters") String note
) {
}

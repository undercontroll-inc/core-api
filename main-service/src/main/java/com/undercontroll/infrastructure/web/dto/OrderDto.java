package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        List<OrderItemDto> orderItems,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedTime,
        OrderStatus status
) {
}

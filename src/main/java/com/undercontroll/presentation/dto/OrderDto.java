package com.undercontroll.presentation.dto;

import com.undercontroll.application.dto.OrderItemDto;
import com.undercontroll.domain.enums.OrderStatus;

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

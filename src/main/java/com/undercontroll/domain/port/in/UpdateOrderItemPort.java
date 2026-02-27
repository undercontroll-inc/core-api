package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.OrderItemDto;

import java.time.LocalDateTime;

public interface UpdateOrderItemPort {
    record Input(
            Integer orderItemId,
            String imageUrl,
            Double labor,
            String observation,
            String volt,
            String series,
            String type,
            String brand,
            String model,
            LocalDateTime completedAt
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}

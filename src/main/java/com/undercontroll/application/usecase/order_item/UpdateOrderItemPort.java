package com.undercontroll.application.usecase.order_item;

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

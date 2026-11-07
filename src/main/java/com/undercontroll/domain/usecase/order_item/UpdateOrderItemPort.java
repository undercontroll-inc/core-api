package com.undercontroll.domain.usecase.order_item;

import java.time.LocalDateTime;

public interface UpdateOrderItemPort {

    void execute(Integer orderId, Integer orderItemId, Input input);

    record Input(
            String imageUrl,
            Double labor,
            String volt,
            String series,
            String type,
            String brand,
            String model,
            LocalDateTime completedAt
    ) {
    }
}

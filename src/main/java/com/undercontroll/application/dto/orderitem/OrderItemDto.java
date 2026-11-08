package com.undercontroll.application.dto.orderitem;

import java.time.LocalDateTime;

public record OrderItemDto(
        Integer id,
        String imageUrl,
        String model,
        String type,
        String brand,
        String voltage,
        String serial,
        Double laborValue,
        LocalDateTime completedAt
) {
}

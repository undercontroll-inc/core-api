package com.undercontroll.domain.usecase.order_item;

import com.undercontroll.domain.model.OrderItem;

import java.time.LocalDateTime;

public record OrderItemOutput(
        Integer id,
        String imageUrl,
        String model,
        String type,
        String brand,
        String volt,
        String series,
        Double laborValue,
        LocalDateTime completedAt
) {

    public static OrderItemOutput from(OrderItem item) {
        return new OrderItemOutput(
                item.getId(),
                item.getImageUrl(),
                item.getModel(),
                item.getType(),
                item.getBrand(),
                item.getVolt(),
                item.getSeries(),
                item.getLaborValue(),
                item.getCompletedAt()
        );
    }
}

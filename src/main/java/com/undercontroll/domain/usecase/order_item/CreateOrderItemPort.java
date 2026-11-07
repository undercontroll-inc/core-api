package com.undercontroll.domain.usecase.order_item;

public interface CreateOrderItemPort {

    OrderItemOutput execute(Integer orderId, Input input);

    record Input(
            String brand,
            String model,
            String type,
            String imageUrl,
            String volt,
            String series,
            Double laborValue
    ) {
    }
}

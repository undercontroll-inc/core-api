package com.undercontroll.application.usecase.order_item;

public interface CreateOrderItemPort {
    record Input(
            String brand,
            String model,
            String type,
            String imageUrl,
            String observation,
            String volt,
            String series,
            Double laborValue
    ) {}

    record Output(
            Integer id,
            String brand,
            String model,
            String type,
            Double laborValue
    ) {}

    Output execute(Input input);
}

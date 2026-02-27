package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.OrderItemDto;

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

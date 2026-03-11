package com.undercontroll.application.usecase.order_item;

import com.undercontroll.application.dto.OrderItemDto;

public interface GetOrderItemByIdPort {
    record Input(
            Integer orderItemId
    ) {}

    record Output(
            OrderItemDto orderItem
    ) {}

    Output execute(Input input);
}

package com.undercontroll.application.usecase.order_item;

import com.undercontroll.application.dto.OrderItemDto;

import java.util.List;

public interface GetOrderItemsPort {
    record Input() {}

    record Output(
            List<OrderItemDto> orderItems
    ) {}

    Output execute(Input input);
}

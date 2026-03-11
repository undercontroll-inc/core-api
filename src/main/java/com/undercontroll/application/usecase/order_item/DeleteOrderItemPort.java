package com.undercontroll.application.usecase.order_item;

public interface DeleteOrderItemPort {
    record Input(
            Integer orderItemId
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}

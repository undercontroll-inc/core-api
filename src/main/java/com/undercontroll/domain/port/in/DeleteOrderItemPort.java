package com.undercontroll.domain.port.in;

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

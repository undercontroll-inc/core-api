package com.undercontroll.domain.port.in;

public interface DeleteOrderPort {
    record Input(
            Integer orderId
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}

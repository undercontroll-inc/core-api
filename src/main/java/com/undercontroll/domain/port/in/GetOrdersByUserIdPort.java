package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.GetOrdersByUserIdResponse;

public interface GetOrdersByUserIdPort {
    record Input(
            Integer userId
    ) {}

    record Output(
            GetOrdersByUserIdResponse orders
    ) {}

    Output execute(Input input);
}

package com.undercontroll.application.usecase.order;

import com.undercontroll.application.dto.GetOrderByIdResponse;

public interface GetOrderByIdPort {
    record Input(
            Integer orderId,
            String userEmail
    ) {}

    record Output(
            GetOrderByIdResponse order
    ) {}

    Output execute(Input input);
}

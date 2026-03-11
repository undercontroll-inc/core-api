package com.undercontroll.application.usecase.order;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.application.dto.OrdersByStatusResponse;

public interface GetOrdersByStatusPort {
    record Input(
            PeriodFilter period
    ) {}

    record Output(
            OrdersByStatusResponse response
    ) {}

    Output execute(Input input);
}

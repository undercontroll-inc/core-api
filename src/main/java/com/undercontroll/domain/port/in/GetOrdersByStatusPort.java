package com.undercontroll.domain.port.in;

import com.undercontroll.domain.model.enums.PeriodFilter;
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

package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.OrderEnrichedDto;

import java.util.List;

public interface GetOrdersPort {
    record Input() {}

    record Output(
            List<OrderEnrichedDto> orders
    ) {}

    Output execute(Input input);
}

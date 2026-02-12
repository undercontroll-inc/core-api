package com.undercontroll.domain.port.in;

import com.undercontroll.domain.entity.enums.PeriodFilter;
import com.undercontroll.domain.entity.enums.StatusFilter;

public interface GetAverageOrderPricePort {
    record Input(
            PeriodFilter period,
            StatusFilter status
    ) {}

    record Output(
            Double averageOrderPrice
    ) {}

    Output execute(Input input);
}

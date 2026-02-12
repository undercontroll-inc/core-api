package com.undercontroll.domain.port.in;

import com.undercontroll.domain.entity.enums.PeriodFilter;
import com.undercontroll.domain.entity.enums.StatusFilter;

public interface GetTotalRevenuePort {
    record Input(
            PeriodFilter period,
            StatusFilter status
    ) {}

    record Output(
            Double totalRevenue
    ) {}

    Output execute(Input input);
}

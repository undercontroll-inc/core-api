package com.undercontroll.domain.port.in;

import com.undercontroll.domain.entity.enums.PeriodFilter;
import com.undercontroll.domain.entity.enums.StatusFilter;

public interface GetProfitMarginPort {
    record Input(
            PeriodFilter period,
            StatusFilter status
    ) {}

    record Output(
            Double profitMargin
    ) {}

    Output execute(Input input);
}

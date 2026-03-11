package com.undercontroll.application.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;

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

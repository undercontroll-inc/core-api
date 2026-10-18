package com.undercontroll.domain.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;

public interface GetAverageOrderPricePort {
    Double execute(PeriodFilter period, StatusFilter status);
}

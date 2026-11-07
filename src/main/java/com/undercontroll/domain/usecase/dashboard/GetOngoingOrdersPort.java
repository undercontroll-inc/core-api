package com.undercontroll.domain.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;

public interface GetOngoingOrdersPort {
    Double execute(PeriodFilter period);
}

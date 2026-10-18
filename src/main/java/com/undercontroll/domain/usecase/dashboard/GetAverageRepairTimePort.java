package com.undercontroll.domain.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;

public interface GetAverageRepairTimePort {
    Double execute(PeriodFilter period, StatusFilter status);
}

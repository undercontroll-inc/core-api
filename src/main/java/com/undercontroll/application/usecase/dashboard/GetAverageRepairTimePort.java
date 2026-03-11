package com.undercontroll.application.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.application.dto.DashboardMetricsResponse;

public interface GetAverageRepairTimePort {
    record Input(
            PeriodFilter period,
            StatusFilter status
    ) {}

    record Output(
            DashboardMetricsResponse response
    ) {}

    Output execute(Input input);
}

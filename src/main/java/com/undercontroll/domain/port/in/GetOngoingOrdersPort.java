package com.undercontroll.domain.port.in;

import com.undercontroll.domain.model.enums.PeriodFilter;
import com.undercontroll.domain.model.enums.StatusFilter;
import com.undercontroll.application.dto.DashboardMetricsResponse;

public interface GetOngoingOrdersPort {
    record Input(
            PeriodFilter period,
            StatusFilter status
    ) {}

    record Output(
            DashboardMetricsResponse response
    ) {}

    Output execute(Input input);
}

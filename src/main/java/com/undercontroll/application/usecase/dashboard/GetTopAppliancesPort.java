package com.undercontroll.application.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.application.dto.TopAppliancesResponse;

public interface GetTopAppliancesPort {
    record Input(
            PeriodFilter period,
            StatusFilter status
    ) {}

    record Output(
            TopAppliancesResponse response
    ) {}

    Output execute(Input input);
}

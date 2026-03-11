package com.undercontroll.application.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.application.dto.TopComponentsResponse;

public interface GetTopComponentsPort {
    record Input(
            PeriodFilter period,
            StatusFilter status
    ) {}

    record Output(
            TopComponentsResponse response
    ) {}

    Output execute(Input input);
}

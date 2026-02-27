package com.undercontroll.domain.port.in;

import com.undercontroll.domain.model.enums.PeriodFilter;
import com.undercontroll.domain.model.enums.StatusFilter;
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

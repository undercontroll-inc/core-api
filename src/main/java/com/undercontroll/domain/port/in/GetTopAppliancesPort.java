package com.undercontroll.domain.port.in;

import com.undercontroll.domain.model.enums.PeriodFilter;
import com.undercontroll.domain.model.enums.StatusFilter;
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

package com.undercontroll.application.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.application.dto.CustomerTypeResponse;

public interface GetCustomerTypeEvolutionPort {
    record Input(
            PeriodFilter period,
            StatusFilter status
    ) {}

    record Output(
            CustomerTypeResponse response
    ) {}

    Output execute(Input input);
}

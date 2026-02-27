package com.undercontroll.domain.port.in;

import com.undercontroll.domain.model.enums.PeriodFilter;
import com.undercontroll.domain.model.enums.StatusFilter;
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

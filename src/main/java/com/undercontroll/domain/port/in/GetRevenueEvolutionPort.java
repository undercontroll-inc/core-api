package com.undercontroll.domain.port.in;

import com.undercontroll.domain.model.enums.PeriodFilter;
import com.undercontroll.domain.model.enums.StatusFilter;
import com.undercontroll.application.dto.RevenueEvolutionResponse;

public interface GetRevenueEvolutionPort {
    record Input(
            PeriodFilter period,
            StatusFilter status
    ) {}

    record Output(
            RevenueEvolutionResponse response
    ) {}

    Output execute(Input input);
}

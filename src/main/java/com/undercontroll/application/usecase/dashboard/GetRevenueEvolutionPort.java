package com.undercontroll.application.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
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

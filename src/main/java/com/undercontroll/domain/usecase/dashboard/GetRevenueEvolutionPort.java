package com.undercontroll.domain.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.model.dashboard.RevenuePoint;

import java.util.List;

public interface GetRevenueEvolutionPort {
    List<RevenuePoint> execute(PeriodFilter period, StatusFilter status);
}

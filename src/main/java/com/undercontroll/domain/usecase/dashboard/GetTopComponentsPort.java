package com.undercontroll.domain.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.model.dashboard.ComponentUsage;

import java.util.List;

public interface GetTopComponentsPort {
    List<ComponentUsage> execute(PeriodFilter period, StatusFilter status);
}

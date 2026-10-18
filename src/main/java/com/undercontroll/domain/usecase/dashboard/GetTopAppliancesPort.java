package com.undercontroll.domain.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.model.dashboard.ApplianceCount;

import java.util.List;

public interface GetTopAppliancesPort {
    List<ApplianceCount> execute(PeriodFilter period, StatusFilter status);
}

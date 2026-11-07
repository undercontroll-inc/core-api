package com.undercontroll.domain.usecase.dashboard;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;

import java.util.List;

public interface GetCustomerTypeEvolutionPort {
    List<CustomerTypePoint> execute(PeriodFilter period, StatusFilter status);
}

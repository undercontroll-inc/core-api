package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.model.dashboard.StatusCount;

import java.util.List;

public interface GetOrdersByStatusPort {
    List<StatusCount> execute(PeriodFilter period);
}

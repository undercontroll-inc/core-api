package com.undercontroll.domain.usecase.dashboard.impl;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.usecase.dashboard.DashboardDateFilter;
import com.undercontroll.domain.usecase.dashboard.GetTotalRevenuePort;
import com.undercontroll.domain.gateway.OrderGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetTotalRevenueImpl implements GetTotalRevenuePort {

    private final OrderGateway orderGateway;

    @Override
    @Cacheable(value = "dashboardMetrics", key = "#period.toString() + '-' + #status.toString() + '-totalRevenue'", unless = "#result == null")
    public Double execute(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = DashboardDateFilter.from(period);
        var statuses = status.getStatuses().stream().map(Enum::name).toList();

        Double totalRevenue = orderGateway.calculateTotalRevenueFiltered(startDate, statuses);

        return totalRevenue;
    }
}

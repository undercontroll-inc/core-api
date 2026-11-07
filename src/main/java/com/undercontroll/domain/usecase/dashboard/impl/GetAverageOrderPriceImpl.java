package com.undercontroll.domain.usecase.dashboard.impl;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.usecase.dashboard.DashboardDateFilter;
import com.undercontroll.domain.usecase.dashboard.GetAverageOrderPricePort;
import com.undercontroll.domain.gateway.OrderGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetAverageOrderPriceImpl implements GetAverageOrderPricePort {

    private final OrderGateway orderGateway;

    @Override
    @Cacheable(value = "dashboardMetrics", key = "#period.toString() + '-' + #status.toString() + '-averageOrderPrice'", unless = "#result == null")
    public Double execute(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = DashboardDateFilter.from(period);
        var statusStrings = status.getStatuses().stream()
                .map(Enum::name)
                .toList();

        Double averagePrice = orderGateway.calculateAverageOrderPriceFiltered(startDate, statusStrings);

        return averagePrice;
    }
}

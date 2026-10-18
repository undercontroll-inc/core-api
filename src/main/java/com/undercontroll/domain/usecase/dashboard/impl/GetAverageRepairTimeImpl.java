package com.undercontroll.domain.usecase.dashboard.impl;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.usecase.dashboard.DashboardDateFilter;
import com.undercontroll.domain.usecase.dashboard.GetAverageRepairTimePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAverageRepairTimeImpl implements GetAverageRepairTimePort {

    private final OrderGateway orderGateway;

    @Override
    public Double execute(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = DashboardDateFilter.from(period);
        List<String> statuses = status.getStatuses().stream()
                .map(OrderStatus::name)
                .toList();

        Double avgHours = orderGateway.calculateAverageRepairTimeFiltered(startDate, statuses);

        return avgHours;
    }
}

package com.undercontroll.domain.usecase.dashboard.impl;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.usecase.dashboard.DashboardDateFilter;
import com.undercontroll.domain.usecase.dashboard.GetOngoingOrdersPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOngoingOrdersImpl implements GetOngoingOrdersPort {

    private final OrderGateway orderGateway;

    @Override
    public Double execute(PeriodFilter period) {
        LocalDate startDate = DashboardDateFilter.from(period);
        List<String> statuses = StatusFilter.ONGOING.getStatuses().stream()
                .map(OrderStatus::name)
                .toList();
        Long count = orderGateway.countOngoingOrdersFiltered(startDate, statuses);
        return count.doubleValue();
    }
}

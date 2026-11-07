package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.dashboard.StatusCount;
import com.undercontroll.domain.usecase.dashboard.DashboardDateFilter;
import com.undercontroll.domain.usecase.order.GetOrdersByStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrdersByStatusImpl implements GetOrdersByStatusPort {

    private final OrderGateway orderGateway;

    @Override
    public List<StatusCount> execute(PeriodFilter period) {
        LocalDate startDate = DashboardDateFilter.from(period);

        return orderGateway.getOrdersByStatus(startDate);
    }
}

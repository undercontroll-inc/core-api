package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.OrderStatus;
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

        List<Object[]> rows = orderGateway.getOrdersByStatus(startDate);

        return rows.stream()
                .map(row -> new StatusCount(
                        OrderStatus.valueOf((String) row[0]),
                        toLong(row[1])
                ))
                .toList();
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }
}

package com.undercontroll.domain.usecase.dashboard.impl;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;
import com.undercontroll.domain.usecase.dashboard.DashboardDateFilter;
import com.undercontroll.domain.usecase.dashboard.GetCustomerTypeEvolutionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCustomerTypeEvolutionImpl implements GetCustomerTypeEvolutionPort {

    private final OrderGateway orderGateway;

    @Override
    public List<CustomerTypePoint> execute(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = DashboardDateFilter.from(period);
        List<String> statuses = status.getStatuses().stream()
                .map(OrderStatus::name)
                .toList();

        List<Object[]> rows = orderGateway.getCustomerTypeEvolution(startDate, statuses);

        return rows.stream()
                .map(row -> new CustomerTypePoint(
                        toLocalDate(row[0]),
                        toLong(row[1]),
                        toLong(row[2])
                ))
                .toList();
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof Date sqlDate) return sqlDate.toLocalDate();
        if (value instanceof LocalDate ld) return ld;
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }
}

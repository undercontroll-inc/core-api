package com.undercontroll.domain.usecase.dashboard.impl;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.dashboard.ComponentUsage;
import com.undercontroll.domain.usecase.dashboard.DashboardDateFilter;
import com.undercontroll.domain.usecase.dashboard.GetTopComponentsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTopComponentsImpl implements GetTopComponentsPort {

    private final OrderGateway orderGateway;

    @Override
    public List<ComponentUsage> execute(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = DashboardDateFilter.from(period);
        List<String> statuses = status.getStatuses().stream()
                .map(OrderStatus::name)
                .toList();

        List<Object[]> rows = orderGateway.getTopComponents(startDate, statuses);

        return rows.stream()
                .map(row -> new ComponentUsage(
                        toInteger(row[0]),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        toLong(row[4])
                ))
                .toList();
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof BigDecimal bd) return bd.longValue();
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }
}

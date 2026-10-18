package com.undercontroll.domain.usecase.dashboard.impl;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.dashboard.ApplianceCount;
import com.undercontroll.domain.usecase.dashboard.DashboardDateFilter;
import com.undercontroll.domain.usecase.dashboard.GetTopAppliancesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTopAppliancesImpl implements GetTopAppliancesPort {

    private final OrderGateway orderGateway;

    @Override
    public List<ApplianceCount> execute(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = DashboardDateFilter.from(period);
        List<String> statuses = status.getStatuses().stream()
                .map(OrderStatus::name)
                .toList();

        List<Object[]> rows = orderGateway.getTopAppliances(startDate, statuses);

        return rows.stream()
                .map(row -> new ApplianceCount(
                        (String) row[0],
                        (String) row[1],
                        toLong(row[2])
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

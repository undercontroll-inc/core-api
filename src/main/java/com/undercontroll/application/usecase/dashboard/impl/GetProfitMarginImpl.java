package com.undercontroll.application.usecase.dashboard.impl;

import com.undercontroll.application.usecase.dashboard.GetProfitMarginPort;
import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.repository.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetProfitMarginImpl implements GetProfitMarginPort {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    @Cacheable(value = "dashboardMetrics", key = "#input.period().toString() + '-' + #input.status().toString() + '-profitMargin'")
    public Output execute(Input input) {
        LocalDate startDate = calculateStartDate(input.period());
        var statuses = input.status().getStatuses();

        var statusStrings = statuses.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        Double totalRevenue = orderRepositoryPort.calculateTotalRevenueFiltered(startDate, statuses);
        Double totalPartsCost = orderRepositoryPort.calculateTotalPartsCostFiltered(startDate, statusStrings);
        Double profitMargin = totalRevenue - totalPartsCost;

        return new Output(profitMargin);
    }

    private LocalDate calculateStartDate(PeriodFilter period) {
        if (period == null) return null;
        
        String periodStr = period.toString();
        return switch (periodStr) {
            case "LAST_7_DAYS" -> LocalDate.now().minusDays(7);
            case "LAST_30_DAYS" -> LocalDate.now().minusDays(30);
            case "LAST_90_DAYS" -> LocalDate.now().minusDays(90);
            case "ALL_TIME" -> null;
            default -> null;
        };
    }
}

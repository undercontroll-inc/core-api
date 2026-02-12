package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetProfitMarginPort;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetProfitMarginImpl implements GetProfitMarginPort {

    private final OrderJpaRepository orderRepository;

    @Override
    @Cacheable(value = "dashboardMetrics", key = "#input.period().toString() + '-' + #input.status().toString() + '-profitMargin'")
    public Output execute(Input input) {
        LocalDate startDate = calculateStartDate(input.period());
        var statuses = input.status().getStatuses();

        var statusStrings = statuses.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        Double totalRevenue = orderRepository.calculateTotalRevenueFiltered(startDate, statuses);
        Double totalPartsCost = orderRepository.calculateTotalPartsCostFiltered(startDate, statusStrings);
        Double profitMargin = totalRevenue - totalPartsCost;

        return new Output(profitMargin);
    }

    private LocalDate calculateStartDate(Object period) {
        // This is a simplified version - implement based on your PeriodFilter enum
        return null;
    }
}

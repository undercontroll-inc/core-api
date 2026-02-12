package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetAverageOrderPricePort;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetAverageOrderPriceImpl implements GetAverageOrderPricePort {

    private final OrderJpaRepository orderRepository;

    @Override
    @Cacheable(value = "dashboardMetrics", key = "#input.period().toString() + '-' + #input.status().toString() + '-averageOrderPrice'")
    public Output execute(Input input) {
        LocalDate startDate = calculateStartDate(input.period());
        var statuses = input.status().getStatuses();

        Double averagePrice = orderRepository.calculateAverageOrderPriceFiltered(startDate, statuses);

        return new Output(averagePrice);
    }

    private LocalDate calculateStartDate(Object period) {
        // This is a simplified version - implement based on your PeriodFilter enum
        return null;
    }
}

package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.enums.PeriodFilter;
import com.undercontroll.domain.port.in.GetAverageOrderPricePort;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetAverageOrderPriceImpl implements GetAverageOrderPricePort {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    @Cacheable(value = "dashboardMetrics", key = "#input.period().toString() + '-' + #input.status().toString() + '-averageOrderPrice'")
    public Output execute(Input input) {
        LocalDate startDate = calculateStartDate(input.period());
        var statuses = input.status().getStatuses();


        var statusStrings = statuses.stream()
                .map(Enum::name)
                .toList();

        Double averagePrice = orderRepositoryPort.calculateAverageOrderPriceFiltered(startDate, statusStrings);

        return new Output(averagePrice);
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

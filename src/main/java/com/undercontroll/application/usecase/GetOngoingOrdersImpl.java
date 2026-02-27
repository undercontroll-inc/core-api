package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.enums.PeriodFilter;
import com.undercontroll.domain.model.enums.StatusFilter;
import com.undercontroll.domain.port.in.GetOngoingOrdersPort;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
import com.undercontroll.application.dto.DashboardMetricsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetOngoingOrdersImpl implements GetOngoingOrdersPort {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public Output execute(Input input) {
        LocalDate startDate = getStartDate(input.period());
        Long count = orderRepositoryPort.countOngoingOrdersFiltered(startDate, null);
        return new Output(new DashboardMetricsResponse(count.doubleValue()));
    }

    private LocalDate getStartDate(PeriodFilter period) {
        if (period == null || period == PeriodFilter.ALL) {
            return LocalDate.now().minusYears(10);
        }
        Integer days = period.getDays();
        return LocalDate.now().minusDays(days);
    }
}

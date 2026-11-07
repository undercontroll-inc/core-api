package com.undercontroll.domain.usecase.dashboard.impl;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.dashboard.ApplianceCount;
import com.undercontroll.domain.model.dashboard.ComponentUsage;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;
import com.undercontroll.domain.model.dashboard.RevenuePoint;
import com.undercontroll.domain.model.dashboard.StatusCount;
import com.undercontroll.domain.usecase.order.impl.GetOrdersByStatusImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardUseCasesTest {

    private static final List<String> COMPLETED = List.of("COMPLETED");
    private static final List<String> ONGOING = List.of("PENDING", "IN_ANALYSIS");

    @Mock
    private OrderGateway orderGateway;

    @Test
    @DisplayName("scalar metrics query the gateway with the status filter names")
    void scalarMetrics() {
        when(orderGateway.calculateTotalRevenueFiltered(isNull(), eq(COMPLETED))).thenReturn(1000.0);
        when(orderGateway.calculateTotalPartsCostFiltered(isNull(), eq(COMPLETED))).thenReturn(400.0);
        when(orderGateway.calculateAverageOrderPriceFiltered(isNull(), eq(COMPLETED))).thenReturn(250.0);
        when(orderGateway.calculateAverageRepairTimeFiltered(isNull(), eq(COMPLETED))).thenReturn(30.0);
        when(orderGateway.countOngoingOrdersFiltered(any(LocalDate.class), eq(ONGOING))).thenReturn(3L);

        assertEquals(1000.0, new GetTotalRevenueImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED));
        assertEquals(600.0, new GetProfitMarginImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED));
        assertEquals(250.0, new GetAverageOrderPriceImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED));
        assertEquals(30.0, new GetAverageRepairTimeImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED));
        assertEquals(3.0, new GetOngoingOrdersImpl(orderGateway).execute(PeriodFilter.SEVEN_DAYS));
    }

    @Test
    @DisplayName("series and rankings pass the period start and status names to the gateway")
    void seriesAndRankings() {
        LocalDate day = LocalDate.of(2026, 1, 10);
        List<RevenuePoint> revenue = List.of(new RevenuePoint(day, 300.5, 120.0, 4L));
        List<CustomerTypePoint> customers = List.of(new CustomerTypePoint(day, 2L, 1L));
        List<ApplianceCount> appliances = List.of(new ApplianceCount("TV", "LG", 7L));
        List<ComponentUsage> components = List.of(new ComponentUsage(3, "Capacitor", "Epcos", "Elétrica", 12L));
        List<StatusCount> statuses = List.of(new StatusCount(OrderStatus.PENDING, 5L));
        when(orderGateway.getRevenueEvolution(isNull(), eq(COMPLETED))).thenReturn(revenue);
        when(orderGateway.getCustomerTypeEvolution(isNull(), eq(COMPLETED))).thenReturn(customers);
        when(orderGateway.getTopAppliances(isNull(), eq(COMPLETED))).thenReturn(appliances);
        when(orderGateway.getTopComponents(any(LocalDate.class), eq(COMPLETED))).thenReturn(components);
        when(orderGateway.getOrdersByStatus(isNull())).thenReturn(statuses);

        assertEquals(revenue, new GetRevenueEvolutionImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED));
        assertEquals(customers, new GetCustomerTypeEvolutionImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED));
        assertEquals(appliances, new GetTopAppliancesImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED));
        assertEquals(components, new GetTopComponentsImpl(orderGateway).execute(PeriodFilter.YEAR, StatusFilter.COMPLETED));
        assertEquals(statuses, new GetOrdersByStatusImpl(orderGateway).execute(PeriodFilter.ALL));
    }
}

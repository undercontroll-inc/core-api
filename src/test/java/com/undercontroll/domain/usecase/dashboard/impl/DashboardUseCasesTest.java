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

import java.math.BigDecimal;
import java.sql.Date;
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
    @DisplayName("revenue evolution converts SQL dates and numeric types")
    void revenueEvolution() {
        when(orderGateway.getRevenueEvolution(isNull(), eq(COMPLETED))).thenReturn(List.of(
                new Object[]{Date.valueOf("2026-01-10"), new BigDecimal("300.50"), 120, 4L},
                new Object[]{LocalDate.of(2026, 1, 11), null, 1.5d, null},
                new Object[]{"bad", "x", "y", "z"}
        ));

        List<RevenuePoint> points = new GetRevenueEvolutionImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED);

        assertEquals(new RevenuePoint(LocalDate.of(2026, 1, 10), 300.5, 120.0, 4L), points.get(0));
        assertEquals(new RevenuePoint(LocalDate.of(2026, 1, 11), 0.0, 1.5, 0L), points.get(1));
        assertEquals(new RevenuePoint(null, 0.0, 0.0, 0L), points.get(2));
    }

    @Test
    @DisplayName("customer type evolution converts dates and counts")
    void customerTypeEvolution() {
        when(orderGateway.getCustomerTypeEvolution(isNull(), eq(COMPLETED))).thenReturn(List.of(
                new Object[]{Date.valueOf("2026-01-10"), 2L, 1},
                new Object[]{null, null, "x"}
        ));

        List<CustomerTypePoint> points = new GetCustomerTypeEvolutionImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED);

        assertEquals(new CustomerTypePoint(LocalDate.of(2026, 1, 10), 2L, 1L), points.get(0));
        assertEquals(new CustomerTypePoint(null, 0L, 0L), points.get(1));
    }

    @Test
    @DisplayName("rankings keep gateway order and convert numeric columns")
    void rankings() {
        when(orderGateway.getTopAppliances(isNull(), eq(COMPLETED))).thenReturn(List.<Object[]>of(
                new Object[]{"TV", "LG", 7}
        ));
        when(orderGateway.getTopComponents(isNull(), eq(COMPLETED))).thenReturn(List.of(
                new Object[]{3L, "Capacitor", "Epcos", "Elétrica", new BigDecimal("12")},
                new Object[]{null, "Fusível", null, null, 2}
        ));

        List<ApplianceCount> appliances = new GetTopAppliancesImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED);
        List<ComponentUsage> components = new GetTopComponentsImpl(orderGateway).execute(PeriodFilter.ALL, StatusFilter.COMPLETED);

        assertEquals(new ApplianceCount("TV", "LG", 7L), appliances.get(0));
        assertEquals(new ComponentUsage(3, "Capacitor", "Epcos", "Elétrica", 12L), components.get(0));
        assertEquals(new ComponentUsage(null, "Fusível", null, null, 2L), components.get(1));
    }

    @Test
    @DisplayName("orders by status maps the status column")
    void ordersByStatus() {
        when(orderGateway.getOrdersByStatus(isNull())).thenReturn(List.<Object[]>of(new Object[]{"PENDING", 5}));

        List<StatusCount> counts = new GetOrdersByStatusImpl(orderGateway).execute(PeriodFilter.ALL);

        assertEquals(List.of(new StatusCount(OrderStatus.PENDING, 5L)), counts);
    }
}

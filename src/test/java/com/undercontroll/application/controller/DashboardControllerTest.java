package com.undercontroll.application.controller;

import com.undercontroll.application.controller.impl.DashboardController;
import com.undercontroll.application.mapper.DashboardDtoMapperImpl;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.model.dashboard.ApplianceCount;
import com.undercontroll.domain.model.dashboard.ComponentUsage;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;
import com.undercontroll.domain.model.dashboard.RevenuePoint;
import com.undercontroll.domain.model.dashboard.StatusCount;
import com.undercontroll.domain.usecase.dashboard.*;
import com.undercontroll.domain.usecase.order.GetOrdersByStatusPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, DashboardDtoMapperImpl.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetTotalRevenuePort getTotalRevenuePort;
    @MockitoBean
    private GetProfitMarginPort getProfitMarginPort;
    @MockitoBean
    private GetAverageOrderPricePort getAverageOrderPricePort;
    @MockitoBean
    private GetOngoingOrdersPort getOngoingOrdersPort;
    @MockitoBean
    private GetAverageRepairTimePort getAverageRepairTimePort;
    @MockitoBean
    private GetRevenueEvolutionPort getRevenueEvolutionPort;
    @MockitoBean
    private GetCustomerTypeEvolutionPort getCustomerTypeEvolutionPort;
    @MockitoBean
    private GetOrdersByStatusPort getOrdersByStatusPort;
    @MockitoBean
    private GetTopAppliancesPort getTopAppliancesPort;
    @MockitoBean
    private GetTopComponentsPort getTopComponentsPort;
    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("scalar metrics are wrapped in a value object")
    void scalarMetrics() throws Exception {
        when(getTotalRevenuePort.execute(PeriodFilter.ALL, StatusFilter.ALL)).thenReturn(1500.0);
        when(getProfitMarginPort.execute(PeriodFilter.THIRTY_DAYS, StatusFilter.COMPLETED)).thenReturn(700.0);
        when(getAverageOrderPricePort.execute(PeriodFilter.ALL, StatusFilter.ALL)).thenReturn(150.0);
        when(getOngoingOrdersPort.execute(PeriodFilter.SEVEN_DAYS)).thenReturn(4.0);
        when(getAverageRepairTimePort.execute(PeriodFilter.ALL, StatusFilter.ALL)).thenReturn(36.5);

        mockMvc.perform(get("/v1/api/dashboard/metrics")).andExpect(status().isOk()).andExpect(jsonPath("$.value").value(1500.0));
        mockMvc.perform(get("/v1/api/dashboard/profit-margin").param("period", "THIRTY_DAYS").param("status", "COMPLETED"))
                .andExpect(jsonPath("$.value").value(700.0));
        mockMvc.perform(get("/v1/api/dashboard/average-order-price")).andExpect(jsonPath("$.value").value(150.0));
        mockMvc.perform(get("/v1/api/dashboard/ongoing-orders").param("period", "SEVEN_DAYS")).andExpect(jsonPath("$.value").value(4.0));
        mockMvc.perform(get("/v1/api/dashboard/average-repair-time")).andExpect(jsonPath("$.value").value(36.5));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("series and rankings keep the response shape")
    void seriesAndRankings() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 10);
        when(getRevenueEvolutionPort.execute(PeriodFilter.THIRTY_DAYS, StatusFilter.ALL))
                .thenReturn(List.of(new RevenuePoint(day, 300.0, 120.0, 3L)));
        when(getCustomerTypeEvolutionPort.execute(PeriodFilter.THIRTY_DAYS, StatusFilter.ALL))
                .thenReturn(List.of(new CustomerTypePoint(day, 2L, 1L)));
        when(getOrdersByStatusPort.execute(PeriodFilter.ALL))
                .thenReturn(List.of(new StatusCount(OrderStatus.PENDING, 5L)));
        when(getTopAppliancesPort.execute(PeriodFilter.ALL, StatusFilter.ALL))
                .thenReturn(List.of(new ApplianceCount("TV", "LG", 7L)));
        when(getTopComponentsPort.execute(PeriodFilter.ALL, StatusFilter.ALL))
                .thenReturn(List.of(new ComponentUsage(3, "Capacitor", "Epcos", "Elétrica", 12L)));

        mockMvc.perform(get("/v1/api/dashboard/charts/revenue-evolution"))
                .andExpect(jsonPath("$.dataPoints[0].date").value("2026-01-10"))
                .andExpect(jsonPath("$.dataPoints[0].orderCount").value(3));
        mockMvc.perform(get("/v1/api/dashboard/charts/customer-type"))
                .andExpect(jsonPath("$.dataPoints[0].recurrentCustomers").value(2));
        mockMvc.perform(get("/v1/api/dashboard/charts/orders-by-status"))
                .andExpect(jsonPath("$.statusCounts[0].status").value("PENDING"))
                .andExpect(jsonPath("$.statusCounts[0].count").value(5));
        mockMvc.perform(get("/v1/api/dashboard/charts/top-appliances"))
                .andExpect(jsonPath("$.appliances[0].brand").value("LG"));
        mockMvc.perform(get("/v1/api/dashboard/charts/top-components"))
                .andExpect(jsonPath("$.components[0].totalQuantityUsed").value(12));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("dashboard is restricted to administrators")
    void customerForbidden() throws Exception {
        mockMvc.perform(get("/v1/api/dashboard/metrics")).andExpect(status().isForbidden());
    }
}

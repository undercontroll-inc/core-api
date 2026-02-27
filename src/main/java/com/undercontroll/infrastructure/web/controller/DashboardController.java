package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.domain.port.in.*;
import com.undercontroll.application.dto.*;
import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.model.enums.PeriodFilter;
import com.undercontroll.domain.model.enums.StatusFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/api/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DashboardController {

    private final GetTotalRevenuePort getTotalRevenuePort;
    private final GetProfitMarginPort getProfitMarginPort;
    private final GetAverageOrderPricePort getAverageOrderPricePort;
    private final GetOngoingOrdersPort getOngoingOrdersPort;
    private final GetAverageRepairTimePort getAverageRepairTimePort;
    private final GetRevenueEvolutionPort getRevenueEvolutionPort;
    private final GetCustomerTypeEvolutionPort getCustomerTypeEvolutionPort;
    private final GetOrdersByStatusPort getOrdersByStatusPort;
    private final GetTopAppliancesPort getTopAppliancesPort;
    private final GetTopComponentsPort getTopComponentsPort;

    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        var output = getTotalRevenuePort.execute(new GetTotalRevenuePort.Input(period, status));
        return ResponseEntity.ok(new DashboardMetricsResponse(output.totalRevenue()));
    }

    @GetMapping("/profit-margin")
    public ResponseEntity<DashboardMetricsResponse> getProfitMargin(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        var output = getProfitMarginPort.execute(new GetProfitMarginPort.Input(period, status));
        return ResponseEntity.ok(new DashboardMetricsResponse(output.profitMargin()));
    }

    @GetMapping("/average-order-price")
    public ResponseEntity<DashboardMetricsResponse> getAverageOrderPrice(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        var output = getAverageOrderPricePort.execute(new GetAverageOrderPricePort.Input(period, status));
        return ResponseEntity.ok(new DashboardMetricsResponse(output.averageOrderPrice()));
    }

    @GetMapping("/ongoing-orders")
    public ResponseEntity<DashboardMetricsResponse> getOngoingOrders(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period) {
        var output = getOngoingOrdersPort.execute(new GetOngoingOrdersPort.Input(period, null));
        return ResponseEntity.ok(output.response());
    }

    @GetMapping("/average-repair-time")
    public ResponseEntity<DashboardMetricsResponse> getAverageRepairTime(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        var output = getAverageRepairTimePort.execute(new GetAverageRepairTimePort.Input(period, status));
        return ResponseEntity.ok(output.response() != null ? output.response() : new DashboardMetricsResponse(null));
    }

    @GetMapping("/charts/revenue-evolution")
    public ResponseEntity<RevenueEvolutionResponse> getRevenueEvolution(
            @RequestParam(required = false, defaultValue = "THIRTY_DAYS") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        var output = getRevenueEvolutionPort.execute(new GetRevenueEvolutionPort.Input(period, status));
        return ResponseEntity.ok(output.response());
    }

    @GetMapping("/charts/customer-type")
    public ResponseEntity<CustomerTypeResponse> getCustomerTypeEvolution(
            @RequestParam(required = false, defaultValue = "THIRTY_DAYS") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        var output = getCustomerTypeEvolutionPort.execute(new GetCustomerTypeEvolutionPort.Input(period, status));
        return ResponseEntity.ok(output.response());
    }

    @GetMapping("/charts/orders-by-status")
    public ResponseEntity<OrdersByStatusResponse> getOrdersByStatus(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period) {
        var output = getOrdersByStatusPort.execute(new GetOrdersByStatusPort.Input(period));
        return ResponseEntity.ok(output.response());
    }

    @GetMapping("/charts/top-appliances")
    public ResponseEntity<TopAppliancesResponse> getTopAppliances(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        var output = getTopAppliancesPort.execute(new GetTopAppliancesPort.Input(period, status));
        return ResponseEntity.ok(output.response());
    }

    @GetMapping("/charts/top-components")
    public ResponseEntity<TopComponentsResponse> getTopComponents(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        var output = getTopComponentsPort.execute(new GetTopComponentsPort.Input(period, status));
        return ResponseEntity.ok(output.response());
    }
}

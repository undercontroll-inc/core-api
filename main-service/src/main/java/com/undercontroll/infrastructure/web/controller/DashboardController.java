package com.undercontroll.infrastructure.web.controller;

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

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        DashboardMetricsResponse response = dashboardService.getTotalRevenue(period, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profit-margin")
    public ResponseEntity<DashboardMetricsResponse> getProfitMargin(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        DashboardMetricsResponse response = dashboardService.getProfitMargin(period, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/average-order-price")
    public ResponseEntity<DashboardMetricsResponse> getAverageOrderPrice(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        DashboardMetricsResponse response = dashboardService.getAverageOrderPrice(period, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ongoing-orders")
    public ResponseEntity<DashboardMetricsResponse> getOngoingOrders(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period) {
        DashboardMetricsResponse response = dashboardService.getOngoingOrders(period, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/average-repair-time")
    public ResponseEntity<DashboardMetricsResponse> getAverageRepairTime(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        DashboardMetricsResponse response = dashboardService.getAverageRepairTime(period, status);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/charts/revenue-evolution")
    public ResponseEntity<RevenueEvolutionResponse> getRevenueEvolution(
            @RequestParam(required = false, defaultValue = "THIRTY_DAYS") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        RevenueEvolutionResponse response = dashboardService.getRevenueEvolution(period, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/charts/customer-type")
    public ResponseEntity<CustomerTypeResponse> getCustomerTypeEvolution(
            @RequestParam(required = false, defaultValue = "THIRTY_DAYS") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        CustomerTypeResponse response = dashboardService.getCustomerTypeEvolution(period, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/charts/orders-by-status")
    public ResponseEntity<OrdersByStatusResponse> getOrdersByStatus(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period) {
        OrdersByStatusResponse response = dashboardService.getOrdersByStatus(period);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/charts/top-appliances")
    public ResponseEntity<TopAppliancesResponse> getTopAppliances(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        TopAppliancesResponse response = dashboardService.getTopAppliances(period, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/charts/top-components")
    public ResponseEntity<TopComponentsResponse> getTopComponents(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status) {
        TopComponentsResponse response = dashboardService.getTopComponents(period, status);
        return ResponseEntity.ok(response);
    }
}


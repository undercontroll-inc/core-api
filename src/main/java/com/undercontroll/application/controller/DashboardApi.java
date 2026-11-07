package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.dashboard.CustomerTypeResponse;
import com.undercontroll.application.dto.dashboard.DashboardMetricsResponse;
import com.undercontroll.application.dto.dashboard.OrdersByStatusResponse;
import com.undercontroll.application.dto.dashboard.RevenueEvolutionResponse;
import com.undercontroll.application.dto.dashboard.TopAppliancesResponse;
import com.undercontroll.application.dto.dashboard.TopComponentsResponse;
import com.undercontroll.domain.enums.PeriodFilter;
import com.undercontroll.domain.enums.StatusFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Dashboard", description = "APIs for administrative dashboard metrics and charts")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
public interface DashboardApi {

    @Operation(summary = "Get total revenue")
    @GetApiResponses
    @GetMapping("/metrics")
    ResponseEntity<DashboardMetricsResponse> getDashboardMetrics(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status
    );

    @Operation(summary = "Get profit margin")
    @GetApiResponses
    @GetMapping("/profit-margin")
    ResponseEntity<DashboardMetricsResponse> getProfitMargin(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status
    );

    @Operation(summary = "Get average order price")
    @GetApiResponses
    @GetMapping("/average-order-price")
    ResponseEntity<DashboardMetricsResponse> getAverageOrderPrice(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status
    );

    @Operation(summary = "Get ongoing orders count")
    @GetApiResponses
    @GetMapping("/ongoing-orders")
    ResponseEntity<DashboardMetricsResponse> getOngoingOrders(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period
    );

    @Operation(summary = "Get average repair time")
    @GetApiResponses
    @GetMapping("/average-repair-time")
    ResponseEntity<DashboardMetricsResponse> getAverageRepairTime(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status
    );

    @Operation(summary = "Get revenue evolution")
    @GetApiResponses
    @GetMapping("/charts/revenue-evolution")
    ResponseEntity<RevenueEvolutionResponse> getRevenueEvolution(
            @RequestParam(required = false, defaultValue = "THIRTY_DAYS") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status
    );

    @Operation(summary = "Get customer type evolution")
    @GetApiResponses
    @GetMapping("/charts/customer-type")
    ResponseEntity<CustomerTypeResponse> getCustomerTypeEvolution(
            @RequestParam(required = false, defaultValue = "THIRTY_DAYS") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status
    );

    @Operation(summary = "Get orders grouped by status")
    @GetApiResponses
    @GetMapping("/charts/orders-by-status")
    ResponseEntity<OrdersByStatusResponse> getOrdersByStatus(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period
    );

    @Operation(summary = "Get most frequent appliances")
    @GetApiResponses
    @GetMapping("/charts/top-appliances")
    ResponseEntity<TopAppliancesResponse> getTopAppliances(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status
    );

    @Operation(summary = "Get most used components")
    @GetApiResponses
    @GetMapping("/charts/top-components")
    ResponseEntity<TopComponentsResponse> getTopComponents(
            @RequestParam(required = false, defaultValue = "ALL") PeriodFilter period,
            @RequestParam(required = false, defaultValue = "ALL") StatusFilter status
    );
}

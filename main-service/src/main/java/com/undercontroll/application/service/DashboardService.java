package com.undercontroll.application.service;

import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.entity.enums.OrderStatus;
import com.undercontroll.domain.entity.enums.PeriodFilter;
import com.undercontroll.domain.entity.enums.StatusFilter;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderJpaRepository orderRepository;

    @Cacheable(value = "dashboardMetrics", key = "#period.toString() + '-' + #status.toString() + '-totalRevenue'")
    public DashboardMetricsResponse getTotalRevenue(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = calculateStartDate(period);
        List<OrderStatus> statuses = status.getStatuses();

        Double totalRevenue = orderRepository.calculateTotalRevenueFiltered(startDate, statuses);
        return new DashboardMetricsResponse(totalRevenue);
    }

    @Cacheable(value = "dashboardMetrics", key = "#period.toString() + '-' + #status.toString() + '-profitMargin'")
    public DashboardMetricsResponse getProfitMargin(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = calculateStartDate(period);
        List<OrderStatus> statuses = status.getStatuses();
        List<String> statusStrings = statuses.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        Double totalRevenue = orderRepository.calculateTotalRevenueFiltered(startDate, statuses);
        Double totalPartsCost = orderRepository.calculateTotalPartsCostFiltered(startDate, statusStrings);
        Double profitMargin = totalRevenue - totalPartsCost;

        return new DashboardMetricsResponse(profitMargin);
    }

    @Cacheable(value = "dashboardMetrics", key = "#period.toString() + '-' + #status.toString() + '-averageOrderPrice'")
    public DashboardMetricsResponse getAverageOrderPrice(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = calculateStartDate(period);
        List<OrderStatus> statuses = status.getStatuses();

        Double averagePrice = orderRepository.calculateAverageOrderPriceFiltered(startDate, statuses);
        return new DashboardMetricsResponse(averagePrice);
    }

    @Cacheable(value = "dashboardMetrics", key = "#period.toString() + '-ongoingOrders'")
    public DashboardMetricsResponse getOngoingOrders(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = calculateStartDate(period);
        // Ongoing orders are always PENDING + IN_ANALYSIS, regardless of status filter
        List<OrderStatus> ongoingStatuses = List.of(OrderStatus.PENDING, OrderStatus.IN_ANALYSIS);

        Long ongoingOrders = orderRepository.countOngoingOrdersFiltered(startDate, ongoingStatuses);
        return new DashboardMetricsResponse(ongoingOrders.doubleValue());
    }

    @Cacheable(value = "dashboardMetrics", key = "#period.toString() + '-' + #status.toString() + '-averageRepairTime'")
    public DashboardMetricsResponse getAverageRepairTime(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = calculateStartDate(period);
        List<String> statusStrings = status.getStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        Double averageRepairTime = orderRepository.calculateAverageRepairTimeFiltered(startDate, statusStrings);
        return new DashboardMetricsResponse(averageRepairTime);
    }

    private LocalDate calculateStartDate(PeriodFilter period) {
        if (period == PeriodFilter.ALL || period.getDays() == null) {
            return null;
        }
        return LocalDate.now().minusDays(period.getDays());
    }

    // ====== Chart Methods ======

    @Cacheable(value = "dashboardCharts", key = "#period.toString() + '-' + #status.toString() + '-revenueEvolution'")
    public RevenueEvolutionResponse getRevenueEvolution(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = calculateStartDate(period);
        LocalDate endDate = LocalDate.now();

        // If startDate is null (ALL period), use a reasonable default
        if (startDate == null) {
            startDate = endDate.minusMonths(12);
        }

        List<String> statusStrings = status.getStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        List<Object[]> results = orderRepository.getRevenueEvolution(startDate, statusStrings);

        // Convert database results to a map for quick lookup
        var dataMap = results.stream()
                .collect(Collectors.toMap(
                        row -> ((Date) row[0]).toLocalDate(),
                        row -> new RevenueEvolutionResponse.DataPoint(
                                ((Date) row[0]).toLocalDate(),
                                ((Number) row[1]).doubleValue(),
                                ((Number) row[2]).doubleValue(),
                                ((Number) row[3]).longValue()
                        )
                ));

        // Fill in missing dates with zero values
        List<RevenueEvolutionResponse.DataPoint> dataPoints = new java.util.ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (dataMap.containsKey(currentDate)) {
                dataPoints.add(dataMap.get(currentDate));
            } else {
                dataPoints.add(new RevenueEvolutionResponse.DataPoint(
                        currentDate, 0.0, 0.0, 0L
                ));
            }
            currentDate = currentDate.plusDays(1);
        }

        return new RevenueEvolutionResponse(dataPoints);
    }

    @Cacheable(value = "dashboardCharts", key = "#period.toString() + '-' + #status.toString() + '-customerType'")
    public CustomerTypeResponse getCustomerTypeEvolution(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = calculateStartDate(period);
        LocalDate endDate = LocalDate.now();

        // If startDate is null (ALL period), use a reasonable default
        if (startDate == null) {
            startDate = endDate.minusMonths(12);
        }

        List<String> statusStrings = status.getStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        List<Object[]> results = orderRepository.getCustomerTypeEvolution(startDate, statusStrings);

        // Convert database results to a map for quick lookup
        var dataMap = results.stream()
                .collect(Collectors.toMap(
                        row -> ((Date) row[0]).toLocalDate(),
                        row -> new CustomerTypeResponse.DataPoint(
                                ((Date) row[0]).toLocalDate(),
                                ((Number) row[1]).longValue(),
                                ((Number) row[2]).longValue()
                        )
                ));

        // Fill in missing dates with zero values
        List<CustomerTypeResponse.DataPoint> dataPoints = new java.util.ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (dataMap.containsKey(currentDate)) {
                dataPoints.add(dataMap.get(currentDate));
            } else {
                dataPoints.add(new CustomerTypeResponse.DataPoint(
                        currentDate, 0L, 0L
                ));
            }
            currentDate = currentDate.plusDays(1);
        }

        return new CustomerTypeResponse(dataPoints);
    }

    @Cacheable(value = "dashboardCharts", key = "#period.toString() + '-ordersByStatus'")
    public OrdersByStatusResponse getOrdersByStatus(PeriodFilter period) {
        LocalDate startDate = calculateStartDate(period);

        List<Object[]> results = orderRepository.getOrdersByStatus(startDate);

        List<OrdersByStatusResponse.StatusCount> statusCounts = results.stream()
                .map(row -> new OrdersByStatusResponse.StatusCount(
                        OrderStatus.valueOf((String) row[0]),
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());

        return new OrdersByStatusResponse(statusCounts);
    }

    @Cacheable(value = "dashboardCharts", key = "#period.toString() + '-' + #status.toString() + '-topAppliances'")
    public TopAppliancesResponse getTopAppliances(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = calculateStartDate(period);
        List<String> statusStrings = status.getStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        List<Object[]> results = orderRepository.getTopAppliances(startDate, statusStrings);

        List<TopAppliancesResponse.ApplianceCount> appliances = results.stream()
                .map(row -> new TopAppliancesResponse.ApplianceCount(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());

        return new TopAppliancesResponse(appliances);
    }

    @Cacheable(value = "dashboardCharts", key = "#period.toString() + '-' + #status.toString() + '-topComponents'")
    public TopComponentsResponse getTopComponents(PeriodFilter period, StatusFilter status) {
        LocalDate startDate = calculateStartDate(period);
        List<String> statusStrings = status.getStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        List<Object[]> results = orderRepository.getTopComponents(startDate, statusStrings);

        List<TopComponentsResponse.ComponentUsage> components = results.stream()
                .map(row -> new TopComponentsResponse.ComponentUsage(
                        ((Number) row[0]).intValue(),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        ((Number) row[4]).longValue()
                ))
                .collect(Collectors.toList());

        return new TopComponentsResponse(components);
    }
}


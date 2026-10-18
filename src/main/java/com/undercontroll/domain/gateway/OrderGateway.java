package com.undercontroll.domain.gateway;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.model.dashboard.ApplianceCount;
import com.undercontroll.domain.model.dashboard.ComponentUsage;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;
import com.undercontroll.domain.model.dashboard.RevenuePoint;
import com.undercontroll.domain.model.dashboard.StatusCount;
import com.undercontroll.domain.model.market.RepairCatalogRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderGateway {

    Order save(Order order);

    void deleteById(Integer id);

    Optional<Order> findById(Integer id);

    List<Order> findAll();

    List<Order> findRecent(OrderStatus status, int limit);

    List<Order> findOpenRepairs(int limit);

    List<Order> findReadyForPickup(int limit);

    Optional<Order> findDetailById(Integer id);

    PaginatedResult<Order> findAllPaginated(Integer offset, Integer limit);

    List<Order> findByUserId(Integer userId);

    Optional<Order> findOrderByOrderItemId(Integer orderItemId);

    Double calculateTotalRevenueFiltered(LocalDate startDate, List<String> statuses);

    Double calculateTotalPartsCostFiltered(LocalDate startDate, List<String> statuses);

    Double calculateAverageOrderPriceFiltered(LocalDate startDate, List<String> statuses);

    Long countOngoingOrdersFiltered(LocalDate startDate, List<String> statuses);

    Double calculateAverageRepairTimeFiltered(LocalDate startDate, List<String> statuses);

    List<RevenuePoint> getRevenueEvolution(LocalDate startDate, List<String> statuses);

    List<CustomerTypePoint> getCustomerTypeEvolution(LocalDate startDate, List<String> statuses);

    List<StatusCount> getOrdersByStatus(LocalDate startDate);

    List<ApplianceCount> getTopAppliances(LocalDate startDate, List<String> statuses);

    List<ComponentUsage> getTopComponents(LocalDate startDate, List<String> statuses);

    List<RepairCatalogRow> getRepairCatalog(LocalDate startDate);

}

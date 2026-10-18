package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.dashboard.ApplianceCount;
import com.undercontroll.domain.model.dashboard.ComponentUsage;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;
import com.undercontroll.domain.model.dashboard.RevenuePoint;
import com.undercontroll.domain.model.dashboard.StatusCount;
import com.undercontroll.domain.model.market.RepairCatalogRow;
import com.undercontroll.infrastructure.mapper.DemandMapper;
import com.undercontroll.infrastructure.mapper.OrderMapper;
import com.undercontroll.infrastructure.persistence.entity.OrderJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.DemandRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderGatewayImpl implements OrderGateway {

    private final OrderJpaRepository orderJpaRepository;
    private final DemandRepository demandRepository;
    private final OrderMapper orderMapper;
    private final DemandMapper demandMapper;

    @Override
    @Transactional
    public Order save(Order order) {
        OrderJpaEntity jpaEntity = orderMapper.toEntityWithId(order);
        OrderJpaEntity savedEntity = orderJpaRepository.save(jpaEntity);
        return orderMapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(Integer id) {
        orderJpaRepository.deleteById(id);
    }

    @Override
    public Optional<Order> findById(Integer id) {
        return orderJpaRepository.findById(id).map(orderMapper::toDomain);
    }

    @Override
    public Optional<Order> findDetailById(Integer id) {
        return orderJpaRepository.findDetailById(id).map(entity -> {
            Order order = orderMapper.toDomainWithoutDemands(entity);
            List<Demand> demands = demandRepository.findByOrder(entity).stream()
                    .map(demandMapper::toDomain)
                    .toList();
            order.setDemands(new ArrayList<>(demands));
            return order;
        });
    }

    @Override
    public List<Order> findAll() {
        return orderJpaRepository.findAll().stream()
                .map(orderMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findRecent(OrderStatus status, int limit) {
        var pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "id"));
        List<OrderJpaEntity> entities = status == null
                ? orderJpaRepository.findRecentWithUserAndItems(pageable)
                : orderJpaRepository.findByStatus(status, pageable);
        return entities.stream().map(orderMapper::toDomainWithoutDemands).toList();
    }

    @Override
    public List<Order> findOpenRepairs(int limit) {
        var pageable = PageRequest.of(
                0,
                Math.max(1, limit),
                Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"))
        );
        return orderJpaRepository.findByStatusIn(
                        List.of(OrderStatus.PENDING, OrderStatus.IN_ANALYSIS),
                        pageable
                ).stream()
                .map(orderMapper::toDomainWithoutDemands)
                .toList();
    }

    @Override
    public List<Order> findReadyForPickup(int limit) {
        var pageable = PageRequest.of(
                0,
                Math.max(1, limit),
                Sort.by(Sort.Direction.DESC, "completedTime").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        return orderJpaRepository.findByStatus(OrderStatus.COMPLETED, pageable).stream()
                .map(orderMapper::toDomainWithoutDemands)
                .toList();
    }

    @Override
    public PaginatedResult<Order> findAllPaginated(Integer offset, Integer limit) {
        Page<OrderJpaEntity> pageResult = orderJpaRepository.findAllPaginated(PageRequest.of(offset, limit));
        List<Order> content = pageResult.getContent().stream()
                .map(orderMapper::toDomain)
                .toList();
        return new PaginatedResult<>(content, pageResult.getTotalElements());
    }

    @Override
    public List<Order> findByUserId(Integer userId) {
        return orderJpaRepository.findByUser_id(userId).stream()
                .map(orderMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Order> findOrderByOrderItemId(Integer orderItemId) {
        return orderJpaRepository.findOrderByOrderItemId(orderItemId)
                .map(orderMapper::toDomain);
    }

    @Override
    public Double calculateTotalRevenueFiltered(LocalDate startDate, List<String> statuses) {
        return orderJpaRepository.calculateTotalRevenueFiltered(startDate, statuses);
    }

    @Override
    public Double calculateTotalPartsCostFiltered(LocalDate startDate, List<String> statuses) {
        return orderJpaRepository.calculateTotalPartsCostFiltered(startDate, statuses);
    }

    @Override
    public Double calculateAverageOrderPriceFiltered(LocalDate startDate, List<String> statuses) {
        return orderJpaRepository.calculateAverageOrderPriceFiltered(startDate, statuses);
    }

    @Override
    public Long countOngoingOrdersFiltered(LocalDate startDate, List<String> statuses) {
        return orderJpaRepository.countOngoingOrdersFiltered(startDate, statuses);
    }

    @Override
    public Double calculateAverageRepairTimeFiltered(LocalDate startDate, List<String> statuses) {
        return orderJpaRepository.calculateAverageRepairTimeFiltered(startDate, statuses);
    }

    @Override
    public List<RevenuePoint> getRevenueEvolution(LocalDate startDate, List<String> statuses) {
        return orderJpaRepository.getRevenueEvolution(startDate, statuses).stream().map(OrderReportRows::revenue).toList();
    }

    @Override
    public List<CustomerTypePoint> getCustomerTypeEvolution(LocalDate startDate, List<String> statuses) {
        return orderJpaRepository.getCustomerTypeEvolution(startDate, statuses).stream().map(OrderReportRows::customerType).toList();
    }

    @Override
    public List<StatusCount> getOrdersByStatus(LocalDate startDate) {
        return orderJpaRepository.getOrdersByStatus(startDate).stream().map(OrderReportRows::statusCount).toList();
    }

    @Override
    public List<ApplianceCount> getTopAppliances(LocalDate startDate, List<String> statuses) {
        return orderJpaRepository.getTopAppliances(startDate, statuses).stream().map(OrderReportRows::appliance).toList();
    }

    @Override
    public List<ComponentUsage> getTopComponents(LocalDate startDate, List<String> statuses) {
        return orderJpaRepository.getTopComponents(startDate, statuses).stream().map(OrderReportRows::componentUsage).toList();
    }

    @Override
    public List<RepairCatalogRow> getRepairCatalog(LocalDate startDate) {
        return orderJpaRepository.getRepairCatalog(startDate).stream().map(OrderReportRows::repairCatalog).toList();
    }

}

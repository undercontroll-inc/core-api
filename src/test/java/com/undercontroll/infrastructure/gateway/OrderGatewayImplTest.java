package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.dashboard.ApplianceCount;
import com.undercontroll.domain.model.dashboard.ComponentUsage;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;
import com.undercontroll.domain.model.dashboard.RevenuePoint;
import com.undercontroll.domain.model.dashboard.StatusCount;
import com.undercontroll.domain.model.market.RepairCatalogRow;
import com.undercontroll.infrastructure.mapper.DemandMapper;
import com.undercontroll.infrastructure.mapper.OrderMapper;
import com.undercontroll.infrastructure.persistence.entity.DemandJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.OrderJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.DemandRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderGatewayImplTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    @Mock
    private DemandRepository demandRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private DemandMapper demandMapper;

    @InjectMocks
    private OrderGatewayImpl orderGateway;

    @Test
    @DisplayName("findDetailById loads items from the order graph and demands in a second query")
    void findDetailByIdSplitsBags() {
        OrderJpaEntity entity = OrderJpaEntity.builder().id(9).build();
        DemandJpaEntity demandEntity = DemandJpaEntity.builder().id(4).build();
        Order header = Order.builder().id(9).build();
        Demand demand = Demand.builder().id(4).quantity(2L).build();
        when(orderJpaRepository.findDetailById(9)).thenReturn(Optional.of(entity));
        when(orderMapper.toDomainWithoutDemands(entity)).thenReturn(header);
        when(demandRepository.findByOrder(entity)).thenReturn(List.of(demandEntity));
        when(demandMapper.toDomain(demandEntity)).thenReturn(demand);

        Optional<Order> found = orderGateway.findDetailById(9);

        assertTrue(found.isPresent());
        assertEquals(9, found.get().getId());
        assertEquals(List.of(demand), found.get().getDemands());
        verify(orderMapper).toDomainWithoutDemands(entity);
        verify(demandRepository).findByOrder(entity);
    }

    @Test
    @DisplayName("report queries are converted into typed rows")
    void reportQueriesAreTyped() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        List<String> statuses = List.of("COMPLETED");
        when(orderJpaRepository.getRevenueEvolution(start, statuses))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.of(2026, 1, 10), 300.0, 100.0, 3L}));
        when(orderJpaRepository.getCustomerTypeEvolution(start, statuses))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.of(2026, 1, 10), 1L, 2L}));
        when(orderJpaRepository.getOrdersByStatus(start)).thenReturn(List.<Object[]>of(new Object[]{"COMPLETED", 4L}));
        when(orderJpaRepository.getTopAppliances(start, statuses)).thenReturn(List.<Object[]>of(new Object[]{"TV", "LG", 7L}));
        when(orderJpaRepository.getTopComponents(start, statuses))
                .thenReturn(List.<Object[]>of(new Object[]{5, "Capacitor", "Epcos", "Elétrica", 9L}));
        when(orderJpaRepository.getRepairCatalog(start)).thenReturn(List.<Object[]>of(new Object[]{"LG", "X1", "TV", 2L}));

        assertEquals(List.of(new RevenuePoint(LocalDate.of(2026, 1, 10), 300.0, 100.0, 3L)), orderGateway.getRevenueEvolution(start, statuses));
        assertEquals(List.of(new CustomerTypePoint(LocalDate.of(2026, 1, 10), 1L, 2L)), orderGateway.getCustomerTypeEvolution(start, statuses));
        assertEquals(List.of(new StatusCount(OrderStatus.COMPLETED, 4L)), orderGateway.getOrdersByStatus(start));
        assertEquals(List.of(new ApplianceCount("TV", "LG", 7L)), orderGateway.getTopAppliances(start, statuses));
        assertEquals(List.of(new ComponentUsage(5, "Capacitor", "Epcos", "Elétrica", 9L)), orderGateway.getTopComponents(start, statuses));
        assertEquals(List.of(new RepairCatalogRow("LG", "X1", "TV", 2L)), orderGateway.getRepairCatalog(start));
    }
}

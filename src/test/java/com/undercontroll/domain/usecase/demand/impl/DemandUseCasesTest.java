package com.undercontroll.domain.usecase.demand.impl;

import com.undercontroll.domain.exception.InvalidDemandException;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.DemandGateway;
import com.undercontroll.domain.gateway.MetricName;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.usecase.demand.CreateDemandPort;
import com.undercontroll.domain.usecase.demand.DemandOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandUseCasesTest {

    @Mock
    private DemandGateway demandGateway;
    @Mock
    private ComponentGateway componentGateway;
    @Mock
    private OrderGateway orderGateway;
    @Mock
    private MetricsGateway metricsGateway;

    private final Order order = Order.builder().id(5).build();
    private final ComponentPart component = ComponentPart.builder().id(10).build();

    @Test
    @DisplayName("creates a demand linking order and component")
    void create() {
        when(componentGateway.findById(10)).thenReturn(Optional.of(component));
        when(orderGateway.findById(5)).thenReturn(Optional.of(order));
        when(demandGateway.save(any(Demand.class))).thenAnswer(invocation -> {
            Demand demand = invocation.getArgument(0);
            demand.setId(25);
            return demand;
        });

        DemandOutput output = new CreateDemandImpl(demandGateway, componentGateway, orderGateway, metricsGateway)
                .execute(5, new CreateDemandPort.Input(10, 3L));

        assertEquals(new DemandOutput(25, 10, 5, 3L), output);
        verify(metricsGateway).increment(MetricName.DEMAND_CREATED);
    }

    @Test
    @DisplayName("create rejects invalid quantity and missing references")
    void createInvalid() {
        CreateDemandImpl useCase = new CreateDemandImpl(demandGateway, componentGateway, orderGateway, metricsGateway);
        when(componentGateway.findById(10)).thenReturn(Optional.of(component));
        when(componentGateway.findById(11)).thenReturn(Optional.empty());
        when(orderGateway.findById(6)).thenReturn(Optional.empty());

        assertThrows(InvalidDemandException.class, () -> useCase.execute(5, new CreateDemandPort.Input(10, 0L)));
        assertThrows(InvalidDemandException.class, () -> useCase.execute(5, new CreateDemandPort.Input(11, 1L)));
        assertThrows(InvalidDemandException.class, () -> useCase.execute(6, new CreateDemandPort.Input(10, 1L)));
    }

    @Test
    @DisplayName("lists demands of an order, optionally filtered by component")
    void list() {
        Demand demand = Demand.builder().id(25).quantity(3L).component(component).order(order).build();
        when(orderGateway.findById(5)).thenReturn(Optional.of(order));
        when(demandGateway.findByOrder(order)).thenReturn(List.of(demand));
        when(demandGateway.findByOrderAndComponentId(order, 10)).thenReturn(Optional.of(demand));
        when(orderGateway.findById(6)).thenReturn(Optional.empty());
        GetDemandsImpl useCase = new GetDemandsImpl(demandGateway, orderGateway);

        assertEquals(List.of(new DemandOutput(25, 10, 5, 3L)), useCase.execute(5, null));
        assertEquals(1, useCase.execute(5, 10).size());
        assertThrows(InvalidDemandException.class, () -> useCase.execute(6, null));
    }

    @Test
    @DisplayName("deletes one demand or all demands of an existing order")
    void delete() {
        when(orderGateway.findById(5)).thenReturn(Optional.of(order));
        when(orderGateway.findById(6)).thenReturn(Optional.empty());

        new DeleteDemandImpl(demandGateway).execute(25);
        DeleteAllDemandsByOrderImpl deleteAll = new DeleteAllDemandsByOrderImpl(demandGateway, orderGateway);
        deleteAll.execute(5);

        verify(demandGateway).deleteById(25);
        verify(demandGateway).deleteByOrder(order);
        assertThrows(OrderNotFoundException.class, () -> deleteAll.execute(6));
    }
}

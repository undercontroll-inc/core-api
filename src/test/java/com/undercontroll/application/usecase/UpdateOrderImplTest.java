package com.undercontroll.application.usecase;

import com.undercontroll.domain.gateway.DemandGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.usecase.demand.CreateDemandPort;
import com.undercontroll.domain.usecase.order.OrderPartInput;
import com.undercontroll.domain.usecase.order.UpdateOrderPort;
import com.undercontroll.domain.usecase.order_item.UpdateOrderItemPort;
import com.undercontroll.domain.usecase.order.impl.OrderDetailsAssembler;
import com.undercontroll.domain.usecase.order.impl.UpdateOrderImpl;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.exception.InvalidUpdateOrderException;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.gateway.MetricName;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateOrderImplTest {

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private MetricsGateway metricsGateway;

    @Mock
    private UpdateOrderItemPort updateOrderItemPort;

    @Mock
    private DemandGateway demandGateway;

    @Mock
    private CreateDemandPort createDemandPort;

    @Mock
    private OrderDetailsAssembler orderDetailsAssembler;

    @InjectMocks
    private UpdateOrderImpl updateOrderImpl;

    private Order existingOrder;

    @BeforeEach
    void setUp() {
        lenient().when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        existingOrder = Order.builder()
                .id(1)
                .status(OrderStatus.RECEIVED)
                .total(100.0)
                .discount(0.0)
                .customerDescription("Original customer")
                .technicalDescription("Original technical")
                .build();
    }

    @Test
    @DisplayName("Should update descriptions successfully")
    void testUpdateOrder_ShouldUpdateSuccessfully() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderPort.Input request = new UpdateOrderPort.Input(
                List.of(), List.of(), "Updated customer", "Updated technical"
        );

        updateOrderImpl.execute(1, request);

        verify(orderGateway, atLeastOnce()).findById(1);
        verify(orderGateway, atLeastOnce()).save(any(Order.class));
        assertEquals("Updated customer", existingOrder.getCustomerDescription());
        assertEquals("Updated technical", existingOrder.getTechnicalDescription());
    }

    @Test
    @DisplayName("Should apply customer and technical descriptions when both are sent")
    void shouldApplyCustomerAndTechnicalDescriptions() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderPort.Input request = new UpdateOrderPort.Input(
                null, null, "Cliente: não gela", "Compressor queimado"
        );

        updateOrderImpl.execute(1, request);

        assertEquals(OrderStatus.RECEIVED, existingOrder.getStatus());
        assertEquals("Cliente: não gela", existingOrder.getCustomerDescription());
        assertEquals("Compressor queimado", existingOrder.getTechnicalDescription());
        verify(orderGateway, atLeastOnce()).save(existingOrder);
    }

    @Test
    @DisplayName("Should keep existing descriptions when request fields are null")
    void shouldKeepExistingDescriptionsWhenRequestFieldsAreNull() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderPort.Input request = new UpdateOrderPort.Input(
                List.of(), List.of(), null, null
        );

        updateOrderImpl.execute(1, request);

        assertEquals("Original customer", existingOrder.getCustomerDescription());
        assertEquals("Original technical", existingOrder.getTechnicalDescription());
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void testUpdateOrder_ShouldThrowException_WhenOrderNotFound() {
        when(orderGateway.findById(999)).thenReturn(Optional.empty());
        doNothing().when(metricsGateway).increment(MetricName.ORDER_UPDATE_FAILED);

        UpdateOrderPort.Input request = new UpdateOrderPort.Input(
                List.of(), List.of(), null, null
        );

        assertThrows(OrderNotFoundException.class, () -> updateOrderImpl.execute(999, request));

        verify(orderGateway, times(1)).findById(999);
        verify(orderGateway, never()).save(any());
        verify(metricsGateway, times(1)).increment(MetricName.ORDER_UPDATE_FAILED);
    }

    @Test
    @DisplayName("Should throw InvalidUpdateOrderException when orderId is null")
    void testUpdateOrder_ShouldThrowException_WhenOrderIdIsNull() {
        UpdateOrderPort.Input request = new UpdateOrderPort.Input(
                List.of(), List.of(), null, null
        );

        assertThrows(InvalidUpdateOrderException.class, () -> updateOrderImpl.execute(null, request));

        verify(orderGateway, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw InvalidUpdateOrderException when orderId is zero or negative")
    void testUpdateOrder_ShouldThrowException_WhenOrderIdIsInvalid() {
        UpdateOrderPort.Input request = new UpdateOrderPort.Input(
                List.of(), List.of(), null, null
        );

        assertThrows(InvalidUpdateOrderException.class, () -> updateOrderImpl.execute(0, request));

        verify(orderGateway, never()).findById(any());
    }

    @Test
    @DisplayName("Should never change the status")
    void testUpdateOrder_WithNullStatus_ShouldNotChangeStatus() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderPort.Input request = new UpdateOrderPort.Input(
                List.of(), List.of(), "New customer description", null
        );

        updateOrderImpl.execute(1, request);

        assertEquals(OrderStatus.RECEIVED, existingOrder.getStatus());
        assertEquals("New customer description", existingOrder.getCustomerDescription());
        assertEquals("Original technical", existingOrder.getTechnicalDescription());
        verify(orderGateway, atLeastOnce()).save(existingOrder);
    }

    @Test
    @DisplayName("Updates existing appliances through the order item port and adds new ones to the order")
    void appliesAppliances() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));

        UpdateOrderPort.Input request = new UpdateOrderPort.Input(
                List.of(
                        new UpdateOrderPort.Appliance(7, "TV", "LG", "X1", "220V", "SN1", 80.0),
                        new UpdateOrderPort.Appliance(null, "Fogão", "Brastemp", "B2", "110V", "SN2", null)
                ),
                null,
                null,
                null
        );

        updateOrderImpl.execute(1, request);

        verify(updateOrderItemPort).execute(1, 7, new UpdateOrderItemPort.Input(
                null, 80.0, "220V", "SN1", "TV", "LG", "X1", null
        ));
        assertEquals(1, existingOrder.getOrderItems().size());
        assertEquals("Fogão", existingOrder.getOrderItems().get(0).getType());
        assertEquals(0.0, existingOrder.getOrderItems().get(0).getLaborValue());
    }

    @Test
    @DisplayName("Updates, removes and creates demands from the parts list")
    void appliesParts() {
        Demand kept = Demand.builder().id(10).quantity(1L).component(ComponentPart.builder().id(1).build()).build();
        Demand removed = Demand.builder().id(11).quantity(1L).component(ComponentPart.builder().id(2).build()).build();
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(demandGateway.findByOrderAndComponentId(existingOrder, 1)).thenReturn(Optional.of(kept));
        when(demandGateway.findByOrderAndComponentId(existingOrder, 2)).thenReturn(Optional.of(removed));
        when(demandGateway.findByOrderAndComponentId(existingOrder, 3)).thenReturn(Optional.empty());

        UpdateOrderPort.Input request = new UpdateOrderPort.Input(
                null,
                List.of(
                        new OrderPartInput(1, 4),
                        new OrderPartInput(2, 0),
                        new OrderPartInput(3, 2),
                        new OrderPartInput(null, 9)
                ),
                null,
                null
        );

        updateOrderImpl.execute(1, request);

        assertEquals(4L, kept.getQuantity());
        verify(demandGateway).save(kept);
        verify(demandGateway).deleteById(11);
        verify(createDemandPort).execute(eq(1), eq(new CreateDemandPort.Input(3, 2L)));
    }

    @Test
    @DisplayName("Recalculates the total after the update and returns the order")
    void recalculatesTotal() {
        Order reloaded = Order.builder()
                .id(1)
                .status(OrderStatus.RECEIVED)
                .total(100.0)
                .discount(10.0)
                .orderItems(new java.util.ArrayList<>(List.of(OrderItem.builder().laborValue(150.0).build())))
                .build();
        OrderOutput output = mock(OrderOutput.class);
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder), Optional.of(reloaded), Optional.of(reloaded));
        when(orderGateway.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderDetailsAssembler.detail(reloaded, true)).thenReturn(output);

        OrderOutput result = updateOrderImpl.execute(1, new UpdateOrderPort.Input(List.of(), List.of(), null, null));

        assertEquals(140.0, reloaded.getTotal());
        assertSame(output, result);
    }
}

package com.undercontroll.application.usecase;

import com.undercontroll.application.dto.OrderItemCreateOrderRequest;
import com.undercontroll.application.dto.PartDto;
import com.undercontroll.application.usecase.order.impl.CreateOrderImpl;
import com.undercontroll.domain.exception.InsuficientComponentException;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.application.usecase.demand.CreateDemandPort;
import com.undercontroll.application.usecase.order_item.CreateOrderItemPort;
import com.undercontroll.application.usecase.order.CreateOrderPort;
import com.undercontroll.application.port.MetricsPort;
import com.undercontroll.domain.repository.OrderRepositoryPort;
import com.undercontroll.domain.repository.StockManagementPort;
import com.undercontroll.domain.repository.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private StockManagementPort stockManagementPort;

    @Mock
    private CreateOrderItemPort createOrderItemPort;

    @Mock
    private CreateDemandPort createDemandPort;

    @Mock
    private MetricsPort metricsPort;

    @InjectMocks
    private CreateOrderImpl createOrderImpl;

    private ComponentPart component;
    private User user;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        component = new ComponentPart();
        component.setId(1);
        component.setName("Resistor 10K");
        component.setPrice(2.50);
        component.setQuantity(100L);

        user = User.builder()
                .id(1)
                .name("John")
                .email("john@test.com")
                .build();

        savedOrder = Order.builder()
                .id(1)
                .status(OrderStatus.PENDING)
                .total(50.0)
                .build();
    }

    @Test
    @DisplayName("Should create order successfully with one part and one appliance")
    void testCreateOrder_ShouldCreateSuccessfully() {
        PartDto part = new PartDto(1, 10);
        OrderItemCreateOrderRequest appliance = new OrderItemCreateOrderRequest(
                "TV", "Brand", "Model", "220V", "SN123", "Note", 50.0
        );

        when(stockManagementPort.findComponentById(1)).thenReturn(Optional.of(component));
        doNothing().when(stockManagementPort).validateStockAvailability(any(ComponentPart.class), anyInt());
        when(createOrderItemPort.execute(any(CreateOrderItemPort.Input.class)))
                .thenReturn(new CreateOrderItemPort.Output(1, "Brand", "Model", "TV", 50.0));
        when(userRepositoryPort.findById(1)).thenReturn(Optional.of(user));
        when(orderRepositoryPort.save(any(Order.class))).thenReturn(savedOrder);
        when(createDemandPort.execute(any(CreateDemandPort.Input.class)))
                .thenReturn(new CreateDemandPort.Output(1, 1, 1, 10L));
        doNothing().when(stockManagementPort).decreaseStock(anyInt(), anyInt());
        doNothing().when(metricsPort).incrementOrderCreated();
        doNothing().when(metricsPort).recordOrderProcessingTime(anyLong());

        CreateOrderPort.Input input = new CreateOrderPort.Input(
                1, List.of(part), List.of(appliance), 0.0,
                "25/11/2025", "20/11/2025", "NF123", false, true, "Service description"
        );

        CreateOrderPort.Output output = createOrderImpl.execute(input);

        assertNotNull(output);
        assertEquals(1, output.id());
        assertEquals("PENDING", output.status());

        verify(stockManagementPort, times(1)).findComponentById(1);
        verify(stockManagementPort, times(1)).validateStockAvailability(any(ComponentPart.class), eq(10));
        verify(createOrderItemPort, times(1)).execute(any(CreateOrderItemPort.Input.class));
        verify(userRepositoryPort, times(1)).findById(1);
        verify(orderRepositoryPort, times(1)).save(any(Order.class));
        verify(createDemandPort, times(1)).execute(any(CreateDemandPort.Input.class));
        verify(stockManagementPort, times(1)).decreaseStock(1, 10);
        verify(metricsPort, times(1)).incrementOrderCreated();
    }

    @Test
    @DisplayName("Should throw exception when stock is insufficient")
    void testCreateOrder_ShouldThrowException_WhenInsufficientStock() {
        PartDto part = new PartDto(1, 200);

        when(stockManagementPort.findComponentById(1)).thenReturn(Optional.of(component));
        doThrow(new InsuficientComponentException("Insufficient stock"))
                .when(stockManagementPort).validateStockAvailability(any(ComponentPart.class), eq(200));

        CreateOrderPort.Input input = new CreateOrderPort.Input(
                1, List.of(part), List.of(), 0.0,
                "25/11/2025", "20/11/2025", "NF123", false, true, "Service"
        );

        assertThrows(InsuficientComponentException.class, () -> createOrderImpl.execute(input));

        verify(stockManagementPort, times(1)).findComponentById(1);
        verify(orderRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate total correctly: partsTotal + laborTotal - discount")
    void testCreateOrder_ShouldCalculateTotalCorrectly() {
        PartDto part = new PartDto(1, 4); // 4 * 2.50 = 10.0 parts total
        OrderItemCreateOrderRequest appliance = new OrderItemCreateOrderRequest(
                "TV", "Brand", "Model", "220V", "SN123", "Note", 50.0
        ); // labor = 50.0

        when(stockManagementPort.findComponentById(1)).thenReturn(Optional.of(component));
        doNothing().when(stockManagementPort).validateStockAvailability(any(ComponentPart.class), anyInt());
        when(createOrderItemPort.execute(any(CreateOrderItemPort.Input.class)))
                .thenReturn(new CreateOrderItemPort.Output(1, "Brand", "Model", "TV", 50.0));
        when(userRepositoryPort.findById(1)).thenReturn(Optional.of(user));
        when(orderRepositoryPort.save(any(Order.class))).thenReturn(savedOrder);
        when(createDemandPort.execute(any(CreateDemandPort.Input.class)))
                .thenReturn(new CreateDemandPort.Output(1, 1, 1, 4L));
        doNothing().when(stockManagementPort).decreaseStock(anyInt(), anyInt());
        doNothing().when(metricsPort).incrementOrderCreated();
        doNothing().when(metricsPort).recordOrderProcessingTime(anyLong());

        CreateOrderPort.Input input = new CreateOrderPort.Input(
                1, List.of(part), List.of(appliance), 10.0,
                "25/11/2025", "20/11/2025", "NF123", false, true, "Service"
        );

        createOrderImpl.execute(input);

        // Verify that save was called with an order having the correct total: 10.0 + 50.0 - 10.0 = 50.0
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepositoryPort, times(1)).save(orderCaptor.capture());
        assertEquals(50.0, orderCaptor.getValue().getTotal());
    }

    @Test
    @DisplayName("Should create order with no parts and no appliances")
    void testCreateOrder_ShouldCreateSuccessfully_WithNoParts() {
        when(userRepositoryPort.findById(1)).thenReturn(Optional.of(user));
        when(orderRepositoryPort.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(metricsPort).incrementOrderCreated();
        doNothing().when(metricsPort).recordOrderProcessingTime(anyLong());

        CreateOrderPort.Input input = new CreateOrderPort.Input(
                1, List.of(), List.of(), 0.0,
                "25/11/2025", "20/11/2025", "NF123", false, true, "Service"
        );

        CreateOrderPort.Output output = createOrderImpl.execute(input);

        assertNotNull(output);
        verify(orderRepositoryPort, times(1)).save(any(Order.class));
        verify(stockManagementPort, never()).findComponentById(anyInt());
        verify(createDemandPort, never()).execute(any());
    }
}

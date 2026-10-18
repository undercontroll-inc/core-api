package com.undercontroll.application.usecase;

import com.undercontroll.domain.usecase.demand.DemandOutput;
import com.undercontroll.domain.usecase.order.CreateOrderPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.usecase.order.OrderPartInput;
import com.undercontroll.domain.usecase.order.impl.CreateOrderImpl;
import com.undercontroll.domain.exception.InsuficientComponentException;
import com.undercontroll.domain.gateway.CurrentUserAdminPort;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.usecase.demand.CreateDemandPort;
import com.undercontroll.domain.gateway.MetricName;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.StockManagementGateway;
import com.undercontroll.domain.gateway.UserGateway;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderImplTest {

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private UserGateway userGateway;

    @Mock
    private StockManagementGateway stockManagementGateway;

    @Mock
    private CreateDemandPort createDemandPort;

    @Mock
    private MetricsGateway metricsGateway;

    @Mock
    private CurrentUserAdminPort currentUserAdminPort;

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

    private void mockFinalOrderFetchAndMapping() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(savedOrder));
    }

    private static CreateOrderPort.Appliance appliance() {
        return new CreateOrderPort.Appliance("TV", "Brand", "Model", "220V", "SN123", 50.0);
    }

    private static CreateOrderPort.Input createRequest(
            List<CreateOrderPort.Appliance> appliances,
            List<OrderPartInput> parts,
            Double discount,
            String customerDescription,
            String technicalDescription
    ) {
        return new CreateOrderPort.Input(
                1, appliances, parts, discount,
                "20/11/2025", "25/11/2025", customerDescription, technicalDescription,
                "PENDING", true, false, "NF123"
        );
    }

    @Test
    @DisplayName("Should create order successfully with one part and one appliance")
    void testCreateOrder_ShouldCreateSuccessfully() {
        OrderPartInput part = new OrderPartInput(1, 10);

        when(stockManagementGateway.findComponentById(1)).thenReturn(Optional.of(component));
        doNothing().when(stockManagementGateway).validateStockAvailability(any(ComponentPart.class), anyInt());
        when(userGateway.findById(1)).thenReturn(Optional.of(user));
        when(orderGateway.save(any(Order.class))).thenReturn(savedOrder);
        when(createDemandPort.execute(anyInt(), any(CreateDemandPort.Input.class)))
                .thenReturn(new DemandOutput(1, 1, 1, 10L));
        doNothing().when(stockManagementGateway).decreaseStock(anyInt(), anyInt());
        doNothing().when(metricsGateway).increment(MetricName.ORDER_CREATED);
        doNothing().when(metricsGateway).recordOrderProcessingTime(anyLong());
        mockFinalOrderFetchAndMapping();

        OrderOutput output = createOrderImpl.execute(
                createRequest(List.of(appliance()), List.of(part), 0.0, "Não gela", null)
        );

        assertNotNull(output);
        assertEquals(1, output.id());
        assertEquals(OrderStatus.PENDING, output.status());

        verify(stockManagementGateway, times(1)).findComponentById(1);
        verify(stockManagementGateway, times(1)).validateStockAvailability(any(ComponentPart.class), eq(10));
        verify(userGateway, times(1)).findById(1);
        verify(orderGateway, times(1)).save(any(Order.class));
        verify(createDemandPort, times(1)).execute(anyInt(), any(CreateDemandPort.Input.class));
        verify(stockManagementGateway, times(1)).decreaseStock(1, 10);
        verify(metricsGateway, times(1)).increment(MetricName.ORDER_CREATED);
    }

    @Test
    @DisplayName("Should throw exception when stock is insufficient")
    void testCreateOrder_ShouldThrowException_WhenInsufficientStock() {
        OrderPartInput part = new OrderPartInput(1, 200);

        when(stockManagementGateway.findComponentById(1)).thenReturn(Optional.of(component));
        doThrow(new InsuficientComponentException("Insufficient stock"))
                .when(stockManagementGateway).validateStockAvailability(any(ComponentPart.class), eq(200));

        assertThrows(InsuficientComponentException.class, () -> createOrderImpl.execute(
                createRequest(List.of(), List.of(part), 0.0, "Não gela", null)
        ));

        verify(stockManagementGateway, times(1)).findComponentById(1);
        verify(orderGateway, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate total correctly: partsTotal + laborTotal - discount")
    void testCreateOrder_ShouldCalculateTotalCorrectly() {
        OrderPartInput part = new OrderPartInput(1, 4);

        when(stockManagementGateway.findComponentById(1)).thenReturn(Optional.of(component));
        doNothing().when(stockManagementGateway).validateStockAvailability(any(ComponentPart.class), anyInt());
        when(userGateway.findById(1)).thenReturn(Optional.of(user));
        when(orderGateway.save(any(Order.class))).thenReturn(savedOrder);
        when(createDemandPort.execute(anyInt(), any(CreateDemandPort.Input.class)))
                .thenReturn(new DemandOutput(1, 1, 1, 4L));
        doNothing().when(stockManagementGateway).decreaseStock(anyInt(), anyInt());
        doNothing().when(metricsGateway).increment(MetricName.ORDER_CREATED);
        doNothing().when(metricsGateway).recordOrderProcessingTime(anyLong());
        mockFinalOrderFetchAndMapping();

        createOrderImpl.execute(createRequest(List.of(appliance()), List.of(part), 10.0, "Não gela", null));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderGateway, times(1)).save(orderCaptor.capture());
        assertEquals(50.0, orderCaptor.getValue().getTotal());
    }

    @Test
    @DisplayName("Should create order with no parts and no appliances")
    void testCreateOrder_ShouldCreateSuccessfully_WithNoParts() {
        when(userGateway.findById(1)).thenReturn(Optional.of(user));
        when(orderGateway.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(metricsGateway).increment(MetricName.ORDER_CREATED);
        doNothing().when(metricsGateway).recordOrderProcessingTime(anyLong());
        mockFinalOrderFetchAndMapping();

        OrderOutput output = createOrderImpl.execute(
                createRequest(List.of(), List.of(), 0.0, "Não gela", null)
        );

        assertNotNull(output);
        verify(orderGateway, times(1)).save(any(Order.class));
        verify(stockManagementGateway, never()).findComponentById(anyInt());
        verify(createDemandPort, never()).execute(anyInt(), any());
    }

    @Test
    @DisplayName("Administrator persists customer and technical descriptions")
    void administratorPersistsTechnicalDescription() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(true);
        when(userGateway.findById(1)).thenReturn(Optional.of(user));
        when(orderGateway.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(metricsGateway).increment(MetricName.ORDER_CREATED);
        doNothing().when(metricsGateway).recordOrderProcessingTime(anyLong());
        mockFinalOrderFetchAndMapping();

        createOrderImpl.execute(createRequest(List.of(), List.of(), 0.0, "Não gela", "Compressor queimado"));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderGateway).save(orderCaptor.capture());
        assertEquals("Não gela", orderCaptor.getValue().getCustomerDescription());
        assertEquals("Compressor queimado", orderCaptor.getValue().getTechnicalDescription());
    }

    @Test
    @DisplayName("Customer can set customerDescription; technicalDescription is ignored")
    void customerIgnoresTechnicalDescription() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(false);
        when(userGateway.findById(1)).thenReturn(Optional.of(user));
        when(orderGateway.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(metricsGateway).increment(MetricName.ORDER_CREATED);
        doNothing().when(metricsGateway).recordOrderProcessingTime(anyLong());
        mockFinalOrderFetchAndMapping();

        createOrderImpl.execute(createRequest(List.of(), List.of(), 0.0, "Não gela", "Não deveria gravar"));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderGateway).save(orderCaptor.capture());
        assertEquals("Não gela", orderCaptor.getValue().getCustomerDescription());
        assertNull(orderCaptor.getValue().getTechnicalDescription());
    }
}

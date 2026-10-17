package com.undercontroll.application.usecase;

import com.undercontroll.application.dto.order.UpdateOrderRequest;
import com.undercontroll.domain.usecase.order.impl.UpdateOrderImpl;
import com.undercontroll.domain.exception.InvalidUpdateOrderException;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.enums.OrderStatus;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateOrderImplTest {

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private MetricsGateway metricsGateway;

    @InjectMocks
    private UpdateOrderImpl updateOrderImpl;

    private Order existingOrder;

    @BeforeEach
    void setUp() {
        existingOrder = Order.builder()
                .id(1)
                .status(OrderStatus.PENDING)
                .total(100.0)
                .discount(0.0)
                .customerDescription("Original customer")
                .technicalDescription("Original technical")
                .build();
    }

    @Test
    @DisplayName("Should update order status successfully")
    void testUpdateOrder_ShouldUpdateSuccessfully() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderRequest request = new UpdateOrderRequest(
                OrderStatus.IN_ANALYSIS, List.of(), List.of(), "Updated customer", "Updated technical"
        );

        updateOrderImpl.execute(1, request);

        verify(orderGateway, times(1)).findById(1);
        verify(orderGateway, times(1)).save(any(Order.class));
        assertEquals("Updated customer", existingOrder.getCustomerDescription());
        assertEquals("Updated technical", existingOrder.getTechnicalDescription());
    }

    @Test
    @DisplayName("Should apply customer and technical descriptions when both are sent")
    void shouldApplyCustomerAndTechnicalDescriptions() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderRequest request = new UpdateOrderRequest(
                null, null, null, "Cliente: não gela", "Compressor queimado"
        );

        updateOrderImpl.execute(1, request);

        assertEquals(OrderStatus.PENDING, existingOrder.getStatus());
        assertEquals("Cliente: não gela", existingOrder.getCustomerDescription());
        assertEquals("Compressor queimado", existingOrder.getTechnicalDescription());
        verify(orderGateway).save(existingOrder);
    }

    @Test
    @DisplayName("Should keep existing descriptions when request fields are null")
    void shouldKeepExistingDescriptionsWhenRequestFieldsAreNull() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderRequest request = new UpdateOrderRequest(
                OrderStatus.IN_ANALYSIS, List.of(), List.of(), null, null
        );

        updateOrderImpl.execute(1, request);

        assertEquals("Original customer", existingOrder.getCustomerDescription());
        assertEquals("Original technical", existingOrder.getTechnicalDescription());
        assertEquals(OrderStatus.IN_ANALYSIS, existingOrder.getStatus());
    }

    @Test
    @DisplayName("Should increment completed metric when status is COMPLETED")
    void testUpdateOrder_ShouldIncrementCompletedMetric_WhenStatusIsCompleted() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenReturn(existingOrder);
        doNothing().when(metricsGateway).incrementOrderCompleted();

        UpdateOrderRequest request = new UpdateOrderRequest(
                OrderStatus.COMPLETED, List.of(), List.of(), null, null
        );

        updateOrderImpl.execute(1, request);

        verify(metricsGateway, times(1)).incrementOrderCompleted();
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void testUpdateOrder_ShouldThrowException_WhenOrderNotFound() {
        when(orderGateway.findById(999)).thenReturn(Optional.empty());
        doNothing().when(metricsGateway).incrementOrderUpdateFailed();

        UpdateOrderRequest request = new UpdateOrderRequest(
                OrderStatus.COMPLETED, List.of(), List.of(), null, null
        );

        assertThrows(OrderNotFoundException.class, () -> updateOrderImpl.execute(999, request));

        verify(orderGateway, times(1)).findById(999);
        verify(orderGateway, never()).save(any());
        verify(metricsGateway, times(1)).incrementOrderUpdateFailed();
    }

    @Test
    @DisplayName("Should throw InvalidUpdateOrderException when orderId is null")
    void testUpdateOrder_ShouldThrowException_WhenOrderIdIsNull() {
        UpdateOrderRequest request = new UpdateOrderRequest(
                OrderStatus.COMPLETED, List.of(), List.of(), null, null
        );

        assertThrows(InvalidUpdateOrderException.class, () -> updateOrderImpl.execute(null, request));

        verify(orderGateway, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw InvalidUpdateOrderException when orderId is zero or negative")
    void testUpdateOrder_ShouldThrowException_WhenOrderIdIsInvalid() {
        UpdateOrderRequest request = new UpdateOrderRequest(
                OrderStatus.COMPLETED, List.of(), List.of(), null, null
        );

        assertThrows(InvalidUpdateOrderException.class, () -> updateOrderImpl.execute(0, request));

        verify(orderGateway, never()).findById(any());
    }

    @Test
    @DisplayName("Should not change status when input status is null")
    void testUpdateOrder_WithNullStatus_ShouldNotChangeStatus() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderGateway.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderRequest request = new UpdateOrderRequest(
                null, List.of(), List.of(), "New customer description", null
        );

        updateOrderImpl.execute(1, request);

        assertEquals(OrderStatus.PENDING, existingOrder.getStatus());
        assertEquals("New customer description", existingOrder.getCustomerDescription());
        assertEquals("Original technical", existingOrder.getTechnicalDescription());
        verify(metricsGateway, never()).incrementOrderCompleted();
        verify(orderGateway, times(1)).save(existingOrder);
    }
}

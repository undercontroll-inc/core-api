package com.undercontroll.application.usecase;

import com.undercontroll.application.dto.UpdateOrderItemDto;
import com.undercontroll.application.dto.PartDto;
import com.undercontroll.domain.exception.InvalidUpdateOrderException;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.enums.OrderStatus;
import com.undercontroll.domain.port.in.UpdateOrderPort;
import com.undercontroll.domain.port.out.MetricsPort;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
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
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private MetricsPort metricsPort;

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
                .description("Original description")
                .build();
    }

    @Test
    @DisplayName("Should update order status successfully")
    void testUpdateOrder_ShouldUpdateSuccessfully() {
        when(orderRepositoryPort.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderRepositoryPort.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderPort.Input input = new UpdateOrderPort.Input(
                1, OrderStatus.IN_ANALYSIS, List.of(), List.of(), "Updated description"
        );

        UpdateOrderPort.Output output = updateOrderImpl.execute(input);

        assertNotNull(output);
        assertTrue(output.success());
        assertEquals("Order updated successfully", output.message());

        verify(orderRepositoryPort, times(1)).findById(1);
        verify(orderRepositoryPort, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should increment completed metric when status is COMPLETED")
    void testUpdateOrder_ShouldIncrementCompletedMetric_WhenStatusIsCompleted() {
        when(orderRepositoryPort.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderRepositoryPort.save(any(Order.class))).thenReturn(existingOrder);
        doNothing().when(metricsPort).incrementOrderCompleted();

        UpdateOrderPort.Input input = new UpdateOrderPort.Input(
                1, OrderStatus.COMPLETED, List.of(), List.of(), null
        );

        updateOrderImpl.execute(input);

        verify(metricsPort, times(1)).incrementOrderCompleted();
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void testUpdateOrder_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepositoryPort.findById(999)).thenReturn(Optional.empty());
        doNothing().when(metricsPort).incrementOrderUpdateFailed();

        UpdateOrderPort.Input input = new UpdateOrderPort.Input(
                999, OrderStatus.COMPLETED, List.of(), List.of(), null
        );

        assertThrows(OrderNotFoundException.class, () -> updateOrderImpl.execute(input));

        verify(orderRepositoryPort, times(1)).findById(999);
        verify(orderRepositoryPort, never()).save(any());
        verify(metricsPort, times(1)).incrementOrderUpdateFailed();
    }

    @Test
    @DisplayName("Should throw InvalidUpdateOrderException when orderId is null")
    void testUpdateOrder_ShouldThrowException_WhenOrderIdIsNull() {
        UpdateOrderPort.Input input = new UpdateOrderPort.Input(
                null, OrderStatus.COMPLETED, List.of(), List.of(), null
        );

        assertThrows(InvalidUpdateOrderException.class, () -> updateOrderImpl.execute(input));

        verify(orderRepositoryPort, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw InvalidUpdateOrderException when orderId is zero or negative")
    void testUpdateOrder_ShouldThrowException_WhenOrderIdIsInvalid() {
        UpdateOrderPort.Input input = new UpdateOrderPort.Input(
                0, OrderStatus.COMPLETED, List.of(), List.of(), null
        );

        assertThrows(InvalidUpdateOrderException.class, () -> updateOrderImpl.execute(input));

        verify(orderRepositoryPort, never()).findById(any());
    }

    @Test
    @DisplayName("Should not change status when input status is null")
    void testUpdateOrder_WithNullStatus_ShouldNotChangeStatus() {
        when(orderRepositoryPort.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderRepositoryPort.save(any(Order.class))).thenReturn(existingOrder);

        UpdateOrderPort.Input input = new UpdateOrderPort.Input(
                1, null, List.of(), List.of(), "New description"
        );

        updateOrderImpl.execute(input);

        assertEquals(OrderStatus.PENDING, existingOrder.getStatus());
        verify(metricsPort, never()).incrementOrderCompleted();
        verify(orderRepositoryPort, times(1)).save(existingOrder);
    }
}

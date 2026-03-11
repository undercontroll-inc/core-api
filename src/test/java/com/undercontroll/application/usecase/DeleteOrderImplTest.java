package com.undercontroll.application.usecase;

import com.undercontroll.application.usecase.order.impl.DeleteOrderImpl;
import com.undercontroll.domain.exception.InvalidDeleteOrderException;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.application.usecase.order.DeleteOrderPort;
import com.undercontroll.domain.repository.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteOrderImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @InjectMocks
    private DeleteOrderImpl deleteOrderImpl;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1)
                .status(OrderStatus.PENDING)
                .total(100.0)
                .build();
    }

    @Test
    @DisplayName("Should delete order successfully")
    void testDeleteOrder_ShouldDeleteSuccessfully() {
        when(orderRepositoryPort.findById(1)).thenReturn(Optional.of(order));
        doNothing().when(orderRepositoryPort).deleteById(1);

        DeleteOrderPort.Input input = new DeleteOrderPort.Input(1);
        DeleteOrderPort.Output output = deleteOrderImpl.execute(input);

        assertNotNull(output);
        assertTrue(output.success());
        assertEquals("Order deleted successfully", output.message());

        verify(orderRepositoryPort, times(1)).findById(1);
        verify(orderRepositoryPort, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("Should throw InvalidDeleteOrderException when orderId is null")
    void testDeleteOrder_ShouldThrowException_WhenOrderIdIsNull() {
        DeleteOrderPort.Input input = new DeleteOrderPort.Input(null);

        assertThrows(InvalidDeleteOrderException.class, () -> deleteOrderImpl.execute(input));

        verify(orderRepositoryPort, never()).findById(any());
        verify(orderRepositoryPort, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw InvalidDeleteOrderException when orderId is zero or negative")
    void testDeleteOrder_ShouldThrowException_WhenOrderIdIsInvalid() {
        DeleteOrderPort.Input input = new DeleteOrderPort.Input(-1);

        assertThrows(InvalidDeleteOrderException.class, () -> deleteOrderImpl.execute(input));

        verify(orderRepositoryPort, never()).findById(any());
        verify(orderRepositoryPort, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order does not exist")
    void testDeleteOrder_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepositoryPort.findById(999)).thenReturn(Optional.empty());

        DeleteOrderPort.Input input = new DeleteOrderPort.Input(999);

        assertThrows(OrderNotFoundException.class, () -> deleteOrderImpl.execute(input));

        verify(orderRepositoryPort, times(1)).findById(999);
        verify(orderRepositoryPort, never()).deleteById(any());
    }
}

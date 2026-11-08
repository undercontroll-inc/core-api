package com.undercontroll.application.usecase;

import com.undercontroll.domain.usecase.order.impl.DeleteOrderImpl;
import com.undercontroll.domain.exception.InvalidDeleteOrderException;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.gateway.OrderGateway;
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
    private OrderGateway orderGateway;

    @InjectMocks
    private DeleteOrderImpl deleteOrderImpl;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1)
                .status(OrderStatus.RECEIVED)
                .total(100.0)
                .build();
    }

    @Test
    @DisplayName("Should delete order successfully")
    void testDeleteOrder_ShouldDeleteSuccessfully() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(order));
        doNothing().when(orderGateway).deleteById(1);

        deleteOrderImpl.execute(1);

        verify(orderGateway, times(1)).findById(1);
        verify(orderGateway, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("Should throw InvalidDeleteOrderException when orderId is null")
    void testDeleteOrder_ShouldThrowException_WhenOrderIdIsNull() {
        assertThrows(InvalidDeleteOrderException.class, () -> deleteOrderImpl.execute(null));

        verify(orderGateway, never()).findById(any());
        verify(orderGateway, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw InvalidDeleteOrderException when orderId is zero or negative")
    void testDeleteOrder_ShouldThrowException_WhenOrderIdIsInvalid() {
        assertThrows(InvalidDeleteOrderException.class, () -> deleteOrderImpl.execute(-1));

        verify(orderGateway, never()).findById(any());
        verify(orderGateway, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order does not exist")
    void testDeleteOrder_ShouldThrowException_WhenOrderNotFound() {
        when(orderGateway.findById(999)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> deleteOrderImpl.execute(999));

        verify(orderGateway, times(1)).findById(999);
        verify(orderGateway, never()).deleteById(any());
    }
}

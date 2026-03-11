package com.undercontroll.application.usecase;

import com.undercontroll.application.usecase.order.impl.GetOrdersByUserIdImpl;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.application.usecase.order.GetOrdersByUserIdPort;
import com.undercontroll.domain.repository.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetOrdersByUserIdImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @InjectMocks
    private GetOrdersByUserIdImpl getOrdersByUserIdImpl;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1)
                .status(OrderStatus.PENDING)
                .total(100.0)
                .discount(0.0)
                .build();
    }

    @Test
    @DisplayName("Should return orders for the given userId")
    void testGetOrdersByUserId_ShouldReturnUserOrders() {
        when(orderRepositoryPort.findByUserId(1)).thenReturn(List.of(order));

        GetOrdersByUserIdPort.Input input = new GetOrdersByUserIdPort.Input(1);
        GetOrdersByUserIdPort.Output output = getOrdersByUserIdImpl.execute(input);

        assertNotNull(output);
        assertNotNull(output.orders());
        assertNotNull(output.orders().data());
        assertEquals(1, output.orders().data().size());
        assertEquals(1, output.orders().data().get(0).id());

        verify(orderRepositoryPort, times(1)).findByUserId(1);
    }

    @Test
    @DisplayName("Should return empty list when user has no orders")
    void testGetOrdersByUserId_ShouldReturnEmptyList_WhenNoOrders() {
        when(orderRepositoryPort.findByUserId(2)).thenReturn(List.of());

        GetOrdersByUserIdPort.Input input = new GetOrdersByUserIdPort.Input(2);
        GetOrdersByUserIdPort.Output output = getOrdersByUserIdImpl.execute(input);

        assertNotNull(output);
        assertNotNull(output.orders());
        assertTrue(output.orders().data().isEmpty());

        verify(orderRepositoryPort, times(1)).findByUserId(2);
    }
}

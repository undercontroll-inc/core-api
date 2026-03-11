package com.undercontroll.application.usecase;

import com.undercontroll.application.dto.OrderEnrichedDto;
import com.undercontroll.application.usecase.order.impl.GetOrdersImpl;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.application.usecase.order.GetOrdersPort;
import com.undercontroll.domain.repository.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetOrdersImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @InjectMocks
    private GetOrdersImpl getOrdersImpl;

    private Order order;

    @BeforeEach
    void setUp() {
        // updatedAt must be non-null because GetOrdersImpl calls order.getUpdatedAt().format(formatter)
        order = Order.builder()
                .id(1)
                .status(OrderStatus.PENDING)
                .total(100.0)
                .discount(10.0)
                .orderItems(new ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should return all orders successfully")
    void testGetOrders_ShouldReturnAllOrders() {
        when(orderRepositoryPort.findAll()).thenReturn(List.of(order));
        when(orderRepositoryPort.calculatePartsTotalByOrderId(1)).thenReturn(5.0);

        GetOrdersPort.Output output = getOrdersImpl.execute(new GetOrdersPort.Input());

        assertNotNull(output);
        assertNotNull(output.orders());
        assertEquals(1, output.orders().size());

        OrderEnrichedDto dto = output.orders().get(0);
        assertEquals(1, dto.id());
        assertEquals(OrderStatus.PENDING, dto.status());

        verify(orderRepositoryPort, times(1)).findAll();
        verify(orderRepositoryPort, times(1)).calculatePartsTotalByOrderId(1);
    }

    @Test
    @DisplayName("Should return empty list when no orders exist")
    void testGetOrders_ShouldReturnEmptyList_WhenNoOrders() {
        when(orderRepositoryPort.findAll()).thenReturn(List.of());

        GetOrdersPort.Output output = getOrdersImpl.execute(new GetOrdersPort.Input());

        assertNotNull(output);
        assertTrue(output.orders().isEmpty());

        verify(orderRepositoryPort, times(1)).findAll();
        verify(orderRepositoryPort, never()).calculatePartsTotalByOrderId(anyInt());
    }
}

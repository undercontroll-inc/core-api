package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.enums.OrderStatus;
import com.undercontroll.domain.port.in.GetOrderByIdPort;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
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
class GetOrderByIdImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @InjectMocks
    private GetOrderByIdImpl getOrderByIdImpl;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1)
                .status(OrderStatus.PENDING)
                .total(100.0)
                .discount(0.0)
                .description("Service description")
                .nf("NF123")
                .returnGuarantee(true)
                .build();
    }

    @Test
    @DisplayName("Should return order when it exists")
    void testGetOrderById_ShouldReturnOrder_WhenExists() {
        when(orderRepositoryPort.findById(1)).thenReturn(Optional.of(order));

        GetOrderByIdPort.Input input = new GetOrderByIdPort.Input(1, null);
        GetOrderByIdPort.Output output = getOrderByIdImpl.execute(input);

        assertNotNull(output);
        assertNotNull(output.order());
        assertNotNull(output.order().data());
        assertEquals(1, output.order().data().id());
        assertEquals(OrderStatus.PENDING, output.order().data().status());

        verify(orderRepositoryPort, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should return null order when order does not exist")
    void testGetOrderById_ShouldReturnNullOrder_WhenNotFound() {
        when(orderRepositoryPort.findById(999)).thenReturn(Optional.empty());

        GetOrderByIdPort.Input input = new GetOrderByIdPort.Input(999, null);
        GetOrderByIdPort.Output output = getOrderByIdImpl.execute(input);

        assertNotNull(output);
        assertNull(output.order());

        verify(orderRepositoryPort, times(1)).findById(999);
    }
}

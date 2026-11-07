package com.undercontroll.application.usecase;

import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.gateway.CurrentUserAdminPort;
import com.undercontroll.domain.usecase.order.impl.GetOrderByIdImpl;
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
class GetOrderByIdImplTest {

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private CurrentUserAdminPort currentUserAdminPort;

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
                .customerDescription("Não gela")
                .technicalDescription("Compressor queimado")
                .nf("NF123")
                .returnGuarantee(true)
                .build();
    }

    @Test
    @DisplayName("Should return order when it exists")
    void testGetOrderById_ShouldReturnOrder_WhenExists() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(order));

        Optional<OrderOutput> output = getOrderByIdImpl.execute(1);

        assertTrue(output.isPresent());
        assertEquals(1, output.get().id());
        assertEquals(OrderStatus.PENDING, output.get().status());
        assertEquals("Não gela", output.get().customerDescription());
        assertNull(output.get().technicalDescription());

        verify(orderGateway, times(1)).findById(1);
    }

    @Test
    @DisplayName("Administrator mapping includes the technical description flag")
    void administratorMapsWithTechnicalDescription() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(true);
        when(orderGateway.findById(1)).thenReturn(Optional.of(order));

        Optional<OrderOutput> output = getOrderByIdImpl.execute(1);

        assertTrue(output.isPresent());
        assertEquals("Compressor queimado", output.get().technicalDescription());
    }

    @Test
    @DisplayName("Should return empty when order does not exist")
    void testGetOrderById_ShouldReturnNullOrder_WhenNotFound() {
        when(orderGateway.findById(999)).thenReturn(Optional.empty());

        Optional<OrderOutput> output = getOrderByIdImpl.execute(999);

        assertTrue(output.isEmpty());

        verify(orderGateway, times(1)).findById(999);
    }
}

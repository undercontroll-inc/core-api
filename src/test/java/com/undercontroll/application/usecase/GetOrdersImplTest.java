package com.undercontroll.application.usecase;

import com.undercontroll.domain.usecase.order.GetOrdersPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.gateway.CurrentUserAdminPort;
import com.undercontroll.domain.usecase.order.impl.GetOrdersImpl;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.gateway.OrderGateway;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetOrdersImplTest {

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private CurrentUserAdminPort currentUserAdminPort;

    @InjectMocks
    private GetOrdersImpl getOrdersImpl;

    private Order order;

    private static final Integer DEFAULT_PAGE = 0;
    private static final Integer DEFAULT_SIZE = 10;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1)
                .status(OrderStatus.PENDING)
                .total(100.0)
                .discount(10.0)
                .customerDescription("Não gela")
                .technicalDescription("Compressor queimado")
                .orderItems(new ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should return all orders successfully when no userId filter is given")
    void testGetOrders_ShouldReturnAllOrders() {
        when(orderGateway.findAllPaginated(DEFAULT_PAGE, DEFAULT_SIZE)).thenReturn(new PaginatedResult<>(List.of(order), 1L));

        GetOrdersPort.Output output = getOrdersImpl.execute(null, DEFAULT_PAGE, DEFAULT_SIZE);

        assertNotNull(output);
        assertNotNull(output.data());
        assertEquals(1, output.data().size());

        OrderOutput mapped = output.data().get(0);
        assertEquals(1, mapped.id());
        assertEquals(OrderStatus.PENDING, mapped.status());
        assertNull(mapped.technicalDescription());
        assertEquals(1L, output.totalElements());
        assertEquals(1, output.totalPages());

        verify(orderGateway, times(1)).findAllPaginated(DEFAULT_PAGE, DEFAULT_SIZE);
        verify(orderGateway, never()).findAll();
        verify(orderGateway, never()).findByUserId(any());
    }

    @Test
    @DisplayName("Should return empty list when no orders exist")
    void testGetOrders_ShouldReturnEmptyList_WhenNoOrders() {
        when(orderGateway.findAllPaginated(DEFAULT_PAGE, DEFAULT_SIZE)).thenReturn(new PaginatedResult<>(List.of(), 0L));

        GetOrdersPort.Output output = getOrdersImpl.execute(null, DEFAULT_PAGE, DEFAULT_SIZE);

        assertNotNull(output);
        assertTrue(output.data().isEmpty());

        verify(orderGateway, times(1)).findAllPaginated(DEFAULT_PAGE, DEFAULT_SIZE);
        verify(orderGateway, never()).findAll();
    }

    @Test
    @DisplayName("Should forward page and size to gateway correctly")
    void testGetOrders_ShouldForwardOffsetAndLimitToGateway() {
        Integer page = 5;
        Integer size = 3;

        when(orderGateway.findAllPaginated(page, size)).thenReturn(new PaginatedResult<>(List.of(), 0L));

        getOrdersImpl.execute(null, page, size);

        verify(orderGateway, times(1)).findAllPaginated(page, size);
    }

    @Test
    @DisplayName("Should map each order to enriched DTO")
    void testGetOrders_ShouldMapEachOrderToEnrichedDto() {
        Order order2 = Order.builder()
                .id(2)
                .status(OrderStatus.COMPLETED)
                .total(200.0)
                .discount(0.0)
                .orderItems(new ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderGateway.findAllPaginated(DEFAULT_PAGE, DEFAULT_SIZE)).thenReturn(new PaginatedResult<>(List.of(order, order2), 2L));

        GetOrdersPort.Output output = getOrdersImpl.execute(null, DEFAULT_PAGE, DEFAULT_SIZE);

        assertEquals(2, output.data().size());
        assertEquals(OrderStatus.PENDING, output.data().get(0).status());
        assertEquals(OrderStatus.COMPLETED, output.data().get(1).status());

    }

    @Test
    @DisplayName("Administrator mapping includes the technical description flag")
    void administratorMapsWithTechnicalDescription() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(true);
        when(orderGateway.findAllPaginated(DEFAULT_PAGE, DEFAULT_SIZE)).thenReturn(new PaginatedResult<>(List.of(order), 1L));

        GetOrdersPort.Output output = getOrdersImpl.execute(null, DEFAULT_PAGE, DEFAULT_SIZE);

        assertEquals("Compressor queimado", output.data().get(0).technicalDescription());
    }

    @Test
    @DisplayName("Should return orders for the given userId without pagination")
    void testGetOrders_ShouldReturnUserOrders_WhenUserIdGiven() {
        when(orderGateway.findByUserId(1)).thenReturn(List.of(order));

        GetOrdersPort.Output output = getOrdersImpl.execute(1, DEFAULT_PAGE, DEFAULT_SIZE);

        assertNotNull(output);
        assertNotNull(output.data());
        assertEquals(1, output.data().size());
        assertEquals(1, output.data().get(0).id());

        verify(orderGateway, times(1)).findByUserId(1);
        verify(orderGateway, never()).findAllPaginated(any(), any());
    }

    @Test
    @DisplayName("Should return empty list when the given user has no orders")
    void testGetOrders_ShouldReturnEmptyList_WhenUserHasNoOrders() {
        when(orderGateway.findByUserId(2)).thenReturn(List.of());

        GetOrdersPort.Output output = getOrdersImpl.execute(2, DEFAULT_PAGE, DEFAULT_SIZE);

        assertNotNull(output);
        assertNotNull(output.data());
        assertTrue(output.data().isEmpty());

        verify(orderGateway, times(1)).findByUserId(2);
    }
}

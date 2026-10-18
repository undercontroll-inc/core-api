package com.undercontroll.application.usecase;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.UnauthorizedOrderOperation;
import com.undercontroll.domain.gateway.CurrentUserAdminPort;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.usecase.order.GetOrdersPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.usecase.order.impl.GetOrdersImpl;
import com.undercontroll.domain.usecase.order.impl.OrderListingQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetOrdersImplTest {

    private static final Integer DEFAULT_PAGE = 0;
    private static final Integer DEFAULT_SIZE = 10;

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private CurrentUserAdminPort currentUserAdminPort;

    @Mock
    private CurrentUserIdPort currentUserIdPort;

    private GetOrdersImpl getOrdersImpl;

    private Order order;

    @BeforeEach
    void setUp() {
        getOrdersImpl = new GetOrdersImpl(new OrderListingQuery(orderGateway), currentUserAdminPort, currentUserIdPort);
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
    @DisplayName("Administrator lists all orders paginated with the technical description")
    void administratorListsAllOrders() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(true);
        when(orderGateway.findAllPaginated(DEFAULT_PAGE, DEFAULT_SIZE)).thenReturn(new PaginatedResult<>(List.of(order), 11L));

        GetOrdersPort.Output output = getOrdersImpl.execute(null, DEFAULT_PAGE, DEFAULT_SIZE);

        assertEquals(1, output.data().size());
        OrderOutput mapped = output.data().get(0);
        assertEquals(1, mapped.id());
        assertEquals("Compressor queimado", mapped.technicalDescription());
        assertEquals(11L, output.totalElements());
        assertEquals(2, output.totalPages());
        verify(orderGateway, never()).findByUserId(any());
        verifyNoInteractions(currentUserIdPort);
    }

    @Test
    @DisplayName("Administrator gets an empty page and the page arguments are forwarded")
    void administratorEmptyPage() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(true);
        when(orderGateway.findAllPaginated(5, 3)).thenReturn(new PaginatedResult<>(List.of(), 0L));

        GetOrdersPort.Output output = getOrdersImpl.execute(null, 5, 3);

        assertTrue(output.data().isEmpty());
        assertEquals(0, output.totalPages());
        verify(orderGateway).findAllPaginated(5, 3);
    }

    @Test
    @DisplayName("Administrator lists the orders of a given user")
    void administratorListsUserOrders() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(true);
        when(orderGateway.findByUserId(1)).thenReturn(List.of(order));

        GetOrdersPort.Output output = getOrdersImpl.execute(1, DEFAULT_PAGE, DEFAULT_SIZE);

        assertEquals(1, output.data().size());
        verify(orderGateway, never()).findAllPaginated(any(), any());
    }

    @Test
    @DisplayName("Customer lists their own orders without the technical description")
    void customerListsOwnOrders() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(false);
        when(currentUserIdPort.require()).thenReturn(1);
        when(orderGateway.findByUserId(1)).thenReturn(List.of(order));

        GetOrdersPort.Output output = getOrdersImpl.execute(1, DEFAULT_PAGE, DEFAULT_SIZE);

        assertEquals(1, output.data().size());
        assertNull(output.data().get(0).technicalDescription());
        assertEquals(1, output.totalPages());
    }

    @Test
    @DisplayName("Customer cannot list all orders")
    void customerCannotListAll() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(false);

        assertThrows(UnauthorizedOrderOperation.class, () -> getOrdersImpl.execute(null, DEFAULT_PAGE, DEFAULT_SIZE));

        verifyNoInteractions(orderGateway);
    }

    @Test
    @DisplayName("Customer cannot list another user's orders")
    void customerCannotListOtherUsers() {
        when(currentUserAdminPort.isAdministrator()).thenReturn(false);
        when(currentUserIdPort.require()).thenReturn(1);

        assertThrows(UnauthorizedOrderOperation.class, () -> getOrdersImpl.execute(2, DEFAULT_PAGE, DEFAULT_SIZE));

        verifyNoInteractions(orderGateway);
    }
}

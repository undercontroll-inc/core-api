package com.undercontroll.application.usecase;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.gateway.CurrentUserAdminPort;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.OrderPhotoGateway;
import com.undercontroll.domain.gateway.OrderStatusEventGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.ShopProfile;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.usecase.order.impl.GetOrderByIdImpl;
import com.undercontroll.domain.usecase.order.impl.OrderAccess;
import com.undercontroll.domain.usecase.order.impl.OrderDetailsAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetOrderByIdImplTest {

    private OrderGateway orderGateway;
    private CurrentUserAdminPort admin;
    private CurrentUserIdPort currentUser;
    private OrderStatusEventGateway events;
    private OrderPhotoGateway photos;
    private GetOrderByIdImpl getOrderByIdImpl;
    private Order order;

    @BeforeEach
    void setUp() {
        orderGateway = mock(OrderGateway.class);
        admin = mock(CurrentUserAdminPort.class);
        currentUser = mock(CurrentUserIdPort.class);
        events = mock(OrderStatusEventGateway.class);
        photos = mock(OrderPhotoGateway.class);
        ShopProfile shop = new ShopProfile("Loja", null, null, null, null, 90, ZoneId.of("America/Sao_Paulo"), "http://localhost");
        OrderAccess access = new OrderAccess(orderGateway, currentUser, admin);
        OrderDetailsAssembler assembler = new OrderDetailsAssembler(events, photos, mock(ObjectStorageGateway.class), shop);
        getOrderByIdImpl = new GetOrderByIdImpl(access, assembler);
        order = Order.builder()
                .id(1)
                .user(User.builder().id(7).name("Maria").build())
                .status(OrderStatus.RECEIVED)
                .total(100.0)
                .discount(0.0)
                .customerDescription("Não gela")
                .technicalDescription("Compressor queimado")
                .nf("NF123")
                .returnGuarantee(true)
                .build();
        when(events.findByOrderId(1)).thenReturn(List.of());
        when(photos.findByOrderId(1)).thenReturn(List.of());
    }

    @Test
    @DisplayName("customer sees their own order without the technical description, with history and photos")
    void ownOrder() {
        when(currentUser.require()).thenReturn(7);
        when(orderGateway.findById(1)).thenReturn(Optional.of(order));

        Optional<OrderOutput> output = getOrderByIdImpl.execute(1);

        assertTrue(output.isPresent());
        assertEquals(OrderStatus.RECEIVED, output.get().status());
        assertNull(output.get().technicalDescription());
        assertEquals(List.of(), output.get().statusHistory());
        assertEquals(List.of(), output.get().photos());
    }

    @Test
    @DisplayName("customer cannot see someone else's order")
    void otherCustomer() {
        when(currentUser.require()).thenReturn(99);
        when(orderGateway.findById(1)).thenReturn(Optional.of(order));

        assertTrue(getOrderByIdImpl.execute(1).isEmpty());
    }

    @Test
    @DisplayName("administrator sees any order with the technical description")
    void administrator() {
        when(admin.isAdministrator()).thenReturn(true);
        when(orderGateway.findById(1)).thenReturn(Optional.of(order));

        assertEquals("Compressor queimado", getOrderByIdImpl.execute(1).orElseThrow().technicalDescription());
    }

    @Test
    @DisplayName("unknown order is empty")
    void notFound() {
        when(orderGateway.findById(999)).thenReturn(Optional.empty());

        assertTrue(getOrderByIdImpl.execute(999).isEmpty());
    }
}

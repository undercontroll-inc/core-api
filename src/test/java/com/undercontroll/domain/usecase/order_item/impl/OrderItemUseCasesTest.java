package com.undercontroll.domain.usecase.order_item.impl;

import com.undercontroll.domain.exception.InvalidOrderItemException;
import com.undercontroll.domain.exception.OrderItemNotFoundException;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.OrderItemGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.usecase.order_item.CreateOrderItemPort;
import com.undercontroll.domain.usecase.order_item.OrderItemOutput;
import com.undercontroll.domain.usecase.order_item.UpdateOrderItemPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemUseCasesTest {

    @Mock
    private OrderItemGateway orderItemGateway;

    @Mock
    private OrderGateway orderGateway;

    private final Order order = Order.builder().id(1).orderItems(new ArrayList<>()).build();

    private static OrderItem item() {
        return OrderItem.builder().id(10).type("TV").brand("LG").model("X1").laborValue(80.0).build();
    }

    @Test
    @DisplayName("create saves the item and attaches it to the order")
    void create() {
        when(orderGateway.findById(1)).thenReturn(Optional.of(order));
        when(orderItemGateway.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });

        OrderItemOutput output = new CreateOrderItemImpl(orderItemGateway, orderGateway)
                .execute(1, new CreateOrderItemPort.Input("LG", "X1", "TV", "img", "220V", "SN1", 80.0));

        assertEquals(10, output.id());
        assertEquals("img", output.imageUrl());
        assertEquals(1, order.getOrderItems().size());
        verify(orderGateway).save(order);
    }

    @Test
    @DisplayName("create rejects negative labor and unknown orders")
    void createInvalid() {
        CreateOrderItemImpl useCase = new CreateOrderItemImpl(orderItemGateway, orderGateway);
        when(orderGateway.findById(2)).thenReturn(Optional.empty());

        assertThrows(InvalidOrderItemException.class,
                () -> useCase.execute(1, new CreateOrderItemPort.Input(null, null, null, null, null, null, -1.0)));
        assertThrows(OrderNotFoundException.class,
                () -> useCase.execute(2, new CreateOrderItemPort.Input(null, null, null, null, null, null, 1.0)));
    }

    @Test
    @DisplayName("update overwrites only provided fields of an item that belongs to the order")
    void update() {
        OrderItem existing = item();
        LocalDateTime completedAt = LocalDateTime.of(2026, 1, 10, 12, 0);
        when(orderGateway.findOrderByOrderItemId(10)).thenReturn(Optional.of(order));
        when(orderItemGateway.findById(10)).thenReturn(Optional.of(existing));

        new UpdateOrderItemImpl(orderItemGateway, orderGateway).execute(1, 10,
                new UpdateOrderItemPort.Input("img", 90.0, "110V", "SN2", "Fogão", "Brastemp", "B2", completedAt));

        assertEquals("img", existing.getImageUrl());
        assertEquals(90.0, existing.getLaborValue());
        assertEquals("110V", existing.getVolt());
        assertEquals("SN2", existing.getSeries());
        assertEquals("Fogão", existing.getType());
        assertEquals("Brastemp", existing.getBrand());
        assertEquals("B2", existing.getModel());
        assertEquals(completedAt, existing.getCompletedAt());
        verify(orderItemGateway).save(existing);
    }

    @Test
    @DisplayName("update rejects invalid input and items from another order")
    void updateInvalid() {
        UpdateOrderItemImpl useCase = new UpdateOrderItemImpl(orderItemGateway, orderGateway);
        UpdateOrderItemPort.Input empty = new UpdateOrderItemPort.Input(null, null, null, null, null, null, null, null);
        when(orderGateway.findOrderByOrderItemId(10)).thenReturn(Optional.of(Order.builder().id(2).build()));
        when(orderGateway.findOrderByOrderItemId(11)).thenReturn(Optional.empty());
        when(orderGateway.findOrderByOrderItemId(12)).thenReturn(Optional.of(order));
        when(orderItemGateway.findById(12)).thenReturn(Optional.empty());

        assertThrows(InvalidOrderItemException.class, () -> useCase.execute(1, null, empty));
        assertThrows(InvalidOrderItemException.class,
                () -> useCase.execute(1, 10, new UpdateOrderItemPort.Input(null, -1.0, null, null, null, null, null, null)));
        assertThrows(OrderItemNotFoundException.class, () -> useCase.execute(1, 10, empty));
        assertThrows(OrderItemNotFoundException.class, () -> useCase.execute(1, 11, empty));
        assertThrows(OrderItemNotFoundException.class, () -> useCase.execute(1, 12, empty));
        verify(orderItemGateway, never()).save(any());
    }

    @Test
    @DisplayName("reads items only through their owning order")
    void read() {
        Order detailed = Order.builder().id(1).orderItems(new ArrayList<>(List.of(item()))).build();
        when(orderGateway.findDetailById(1)).thenReturn(Optional.of(detailed));
        when(orderGateway.findDetailById(2)).thenReturn(Optional.empty());
        when(orderGateway.findOrderByOrderItemId(10)).thenReturn(Optional.of(order));
        when(orderGateway.findOrderByOrderItemId(11)).thenReturn(Optional.of(Order.builder().id(3).build()));
        when(orderItemGateway.findById(10)).thenReturn(Optional.of(item()));

        assertEquals(1, new GetOrderItemsImpl(orderGateway).execute(1).size());
        assertThrows(OrderNotFoundException.class, () -> new GetOrderItemsImpl(orderGateway).execute(2));
        GetOrderItemByIdImpl byId = new GetOrderItemByIdImpl(orderItemGateway, orderGateway);
        assertEquals("LG", byId.execute(1, 10).orElseThrow().brand());
        assertTrue(byId.execute(1, 11).isEmpty());
    }

    @Test
    @DisplayName("delete only removes items of the given order")
    void delete() {
        DeleteOrderItemImpl useCase = new DeleteOrderItemImpl(orderItemGateway, orderGateway);
        when(orderGateway.findOrderByOrderItemId(10)).thenReturn(Optional.of(order));
        when(orderGateway.findOrderByOrderItemId(11)).thenReturn(Optional.of(Order.builder().id(3).build()));

        useCase.execute(1, 10);

        verify(orderItemGateway).deleteById(10);
        assertThrows(OrderItemNotFoundException.class, () -> useCase.execute(1, 11));
        assertThrows(InvalidOrderItemException.class, () -> useCase.execute(1, 0));
    }
}

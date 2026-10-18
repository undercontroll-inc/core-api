package com.undercontroll.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderTest {

    @Test
    @DisplayName("parts total multiplies each quantity by the component price")
    void partsTotalMultipliesQuantityByPrice() {
        Order order = Order.builder()
                .discount(10.0)
                .orderItems(new ArrayList<>(List.of(OrderItem.builder().laborValue(50.0).build())))
                .demands(new ArrayList<>(List.of(
                        demand(3L, 2.5),
                        demand(2L, 10.0)
                )))
                .build();

        assertEquals(27.5, order.calculatePartsTotal());
        assertEquals(67.5, order.calculateTotal());
    }

    private static Demand demand(long quantity, double price) {
        return Demand.builder()
                .quantity(quantity)
                .component(ComponentPart.builder().price(price).build())
                .build();
    }
}

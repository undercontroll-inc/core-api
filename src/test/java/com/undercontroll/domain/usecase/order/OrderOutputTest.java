package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderOutputTest {

    private static Order order() {
        ComponentPart component = ComponentPart.builder().id(5).name("Capacitor").price(2.0).build();
        return Order.builder()
                .id(7)
                .user(User.builder().id(3).name("Ana").password("secret").CEP("01000-000").build())
                .orderItems(new ArrayList<>(List.of(OrderItem.builder().id(1).brand("LG").laborValue(80.0).build())))
                .demands(new ArrayList<>(List.of(Demand.builder().id(9).quantity(1L).component(component).build())))
                .status(OrderStatus.PENDING)
                .discount(0.0)
                .total(83.0)
                .customerDescription("Não gela")
                .technicalDescription("Compressor queimado")
                .nf("NF-1")
                .returnGuarantee(true)
                .received_at(LocalDate.of(2026, 1, 10))
                .updatedAt(LocalDateTime.of(2026, 1, 11, 9, 30))
                .build();
    }

    @Test
    @DisplayName("includes technicalDescription when the caller is admin")
    void includesTechnicalWhenRequested() {
        OrderOutput output = OrderOutput.from(order(), true);

        assertEquals("Não gela", output.customerDescription());
        assertEquals("Compressor queimado", output.technicalDescription());
    }

    @Test
    @DisplayName("hides technicalDescription for a customer caller and maps the order tree")
    void hidesTechnicalWhenNotRequested() {
        OrderOutput output = OrderOutput.from(order(), false);

        assertNull(output.technicalDescription());
        assertEquals("01000-000", output.user().CEP());
        assertEquals("LG", output.appliances().get(0).brand());
        assertEquals("Capacitor", output.parts().get(0).item());
        assertEquals(80.0, output.laborTotal());
        assertEquals("2026-01-10", output.receivedAt());
        assertEquals("2026-01-11T09:30", output.updatedAt());
        assertNull(output.deadline());
        assertEquals(83.0, output.totalValue());
    }

    @Test
    @DisplayName("keeps optional dates and user null")
    void nullOptionalFields() {
        Order order = Order.builder().id(1).discount(0.0).build();

        OrderOutput output = OrderOutput.from(order, false);

        assertNull(output.user());
        assertNull(output.receivedAt());
        assertNull(output.updatedAt());
    }
}

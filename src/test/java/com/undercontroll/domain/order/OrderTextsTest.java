package com.undercontroll.domain.order;

import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTextsTest {

    private static final String URL = "https://loja.example/p/7Hq2xLmP9vKa3TzR8dNw4c";

    private static Order order(OrderStatus status, OrderOutcome outcome) {
        return Order.builder()
                .id(123)
                .status(status)
                .outcome(outcome)
                .budgetTotal(1180.0)
                .user(User.builder().name("maria aparecida").lastName("Souza").cpf("123").phone("11999").build())
                .orderItems(new ArrayList<>(List.of(OrderItem.builder().type("Liquidificador").brand("Arno").build())))
                .build();
    }

    @Test
    @DisplayName("WhatsApp texts per status use only the first name, the device and the link")
    void whatsapp() {
        assertEquals("Olá, Maria! O orçamento do conserto do seu liquidificador Arno ficou em R$ 1.180,00. "
                        + "Para aprovar ou recusar, toque no link: " + URL,
                OrderMessages.whatsapp(order(OrderStatus.AWAITING_APPROVAL, null), URL));
        assertEquals("Olá, Maria! Seu liquidificador Arno está pronto. Pode buscar na loja. Detalhes: " + URL,
                OrderMessages.whatsapp(order(OrderStatus.READY_FOR_PICKUP, OrderOutcome.REPAIRED), URL));
        assertEquals("Olá, Maria! Seu liquidificador Arno já pode ser retirado na loja, sem custo. Detalhes: " + URL,
                OrderMessages.whatsapp(order(OrderStatus.READY_FOR_PICKUP, OrderOutcome.NOT_REPAIRED), URL));
        assertEquals("Olá, Maria! Acompanhe o conserto do seu liquidificador Arno por aqui: " + URL,
                OrderMessages.whatsapp(order(OrderStatus.IN_REPAIR, null), URL));
        String text = OrderMessages.whatsapp(order(OrderStatus.AWAITING_APPROVAL, null), URL);
        assertFalse(text.contains("Souza"));
        assertFalse(text.contains("11999"));
    }

    @Test
    @DisplayName("tracking tokens are 22 base62 characters and unique")
    void tokens() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            String token = TrackingTokens.generate();
            assertTrue(TrackingTokens.wellFormed(token), token);
            assertTrue(seen.add(token));
        }
        assertFalse(TrackingTokens.wellFormed("short"));
        assertFalse(TrackingTokens.wellFormed("7Hq2xLmP9vKa3TzR8dNw4-"));
        assertFalse(TrackingTokens.wellFormed(null));
    }
}

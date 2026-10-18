package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.gateway.ShopSnapshotLoader;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.model.chat.ShopSnapshot;
import com.undercontroll.domain.model.chat.ShopSnapshotFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendChatMessageImplTest {

    @Mock
    private ObjectProvider<AnaChatGateway> anaChatGateway;

    @Mock
    private AnaChatGateway gateway;

    @Mock
    private CurrentUserIdPort currentUserIdPort;

    @Mock
    private ShopSnapshotLoader shopSnapshotLoader;

    @Mock
    private OrderGateway orderGateway;

    @InjectMocks
    private SendChatMessageImpl useCase;

    @Test
    @DisplayName("returns the assistant reply with a shop snapshot briefing")
    void replies() {
        when(anaChatGateway.getIfAvailable()).thenReturn(gateway);
        when(currentUserIdPort.require()).thenReturn(7);
        when(shopSnapshotLoader.load()).thenReturn(ShopSnapshotFactory.from(
                List.of(
                        Order.builder()
                                .id(12)
                                .status(OrderStatus.PENDING)
                                .user(User.builder().name("Maria").lastName("Souza").build())
                                .orderItems(List.of(
                                        OrderItem.builder().type("liquidificador").brand("Mondial").build()
                                ))
                                .build(),
                        Order.builder()
                                .id(20)
                                .status(OrderStatus.COMPLETED)
                                .user(User.builder().name("João").lastName("Lima").build())
                                .orderItems(List.of(
                                        OrderItem.builder().type("airfryer").brand("Philco").build()
                                ))
                                .build(),
                        Order.builder()
                                .id(99)
                                .status(OrderStatus.DELIVERED)
                                .user(User.builder().name("Entregue").lastName("Silva").build())
                                .orderItems(List.of())
                                .build()
                ),
                List.of(),
                List.of(),
                null
        ));
        ArgumentCaptor<String> briefing = ArgumentCaptor.forClass(String.class);
        when(gateway.reply(eq("7"), eq("Quais consertos estão abertos?"), briefing.capture()))
                .thenReturn("Tem 3 em andamento.");

        String response = useCase.execute(
                "Quais consertos estão abertos?");

        assertEquals("Tem 3 em andamento.", response);
        assertTrue(briefing.getValue().contains("Maria"));
        assertTrue(briefing.getValue().contains("liquidificador"));
        assertTrue(briefing.getValue().contains("João"));
        assertTrue(briefing.getValue().contains("em andamento"));
        assertTrue(briefing.getValue().contains("pronto para buscar"));
        assertFalse(briefing.getValue().contains("Entregue"));
    }

    @Test
    @DisplayName("throws when the LLM bean is missing")
    void missingLlm() {
        when(anaChatGateway.getIfAvailable()).thenReturn(null);

        String request = "Oi";
        assertThrows(AnaUnavailableException.class, () -> useCase.execute(request));
    }

    @Test
    @DisplayName("loads a cited order into the briefing instead of waiting for a tool")
    void inlinesCitedOrder() {
        when(anaChatGateway.getIfAvailable()).thenReturn(gateway);
        when(currentUserIdPort.require()).thenReturn(7);
        when(shopSnapshotLoader.load()).thenReturn(ShopSnapshot.empty());
        when(orderGateway.findDetailById(12)).thenReturn(Optional.of(
                Order.builder()
                        .id(12)
                        .status(OrderStatus.PENDING)
                        .user(User.builder().name("Maria").lastName("Souza").build())
                        .orderItems(List.of(OrderItem.builder().type("liquidificador").brand("Mondial").build()))
                        .build()
        ));
        ArgumentCaptor<String> briefing = ArgumentCaptor.forClass(String.class);
        when(gateway.reply(eq("7"), eq("Me fala do pedido 12"), briefing.capture()))
                .thenReturn("O liquidificador da Maria ainda não foi olhado.");

        useCase.execute("Me fala do pedido 12");

        assertTrue(briefing.getValue().contains("pedido 12"));
        assertTrue(briefing.getValue().contains("Maria"));
        assertTrue(briefing.getValue().contains("liquidificador"));
    }

    @Test
    @DisplayName("inlines an order cited in a previous turn of the conversation")
    void inlinesCitedOrderFromHistory() {
        when(anaChatGateway.getIfAvailable()).thenReturn(gateway);
        when(currentUserIdPort.require()).thenReturn(7);
        when(shopSnapshotLoader.load()).thenReturn(ShopSnapshot.empty());
        when(gateway.recentConversationTexts("7")).thenReturn(List.of("Me fala do pedido 12"));
        when(orderGateway.findDetailById(12)).thenReturn(Optional.of(
                Order.builder()
                        .id(12)
                        .status(OrderStatus.PENDING)
                        .user(User.builder().name("Maria").lastName("Souza").build())
                        .orderItems(List.of(OrderItem.builder().type("liquidificador").brand("Mondial").build()))
                        .build()
        ));
        ArgumentCaptor<String> briefing = ArgumentCaptor.forClass(String.class);
        when(gateway.reply(eq("7"), eq("Quanto custa?"), briefing.capture()))
                .thenReturn("O conserto da Maria ainda não tem valor.");

        useCase.execute("Quanto custa?");

        assertTrue(briefing.getValue().contains("pedido 12"));
        assertTrue(briefing.getValue().contains("Maria"));
    }
}

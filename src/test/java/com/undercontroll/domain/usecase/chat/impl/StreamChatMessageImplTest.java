package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.application.dto.chat.SendChatMessageRequest;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.ChatStreamSink;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.model.chat.ShopSnapshot;
import com.undercontroll.domain.model.chat.ShopSuggestionComposer;
import com.undercontroll.domain.usecase.chat.StreamChatSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamChatMessageImplTest {

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
    private StreamChatMessageImpl useCase;

    @Test
    @DisplayName("prepares a stream session with a shop snapshot briefing")
    void preparesSession() {
        when(anaChatGateway.getIfAvailable()).thenReturn(gateway);
        when(currentUserIdPort.require()).thenReturn(7);
        when(shopSnapshotLoader.load()).thenReturn(ShopSuggestionComposer.from(
                List.of(
                        Order.builder()
                                .id(12)
                                .status(OrderStatus.PENDING)
                                .user(User.builder().name("Maria").lastName("Souza").build())
                                .orderItems(List.of(
                                        OrderItem.builder().type("liquidificador").brand("Mondial").build()
                                ))
                                .build()
                ),
                List.of(),
                List.of(),
                null
        ));

        StreamChatSession session = useCase.prepare(new SendChatMessageRequest("Quais consertos estão abertos?"));

        assertEquals("7", session.conversationId());
        assertEquals("Quais consertos estão abertos?", session.message());
        assertTrue(session.briefing().contains("Maria"));
        assertTrue(session.briefing().contains("liquidificador"));
    }

    @Test
    @DisplayName("throws when the LLM bean is missing")
    void missingLlm() {
        when(anaChatGateway.getIfAvailable()).thenReturn(null);

        SendChatMessageRequest request = new SendChatMessageRequest("Oi");
        assertThrows(AnaUnavailableException.class, () -> useCase.prepare(request));
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

        StreamChatSession session = useCase.prepare(new SendChatMessageRequest("Me fala do pedido 12"));

        assertTrue(session.briefing().contains("pedido 12"));
        assertTrue(session.briefing().contains("Maria"));
    }

    @Test
    @DisplayName("starts the sink and streams through the gateway")
    void streamsThroughGateway() {
        when(anaChatGateway.getIfAvailable()).thenReturn(gateway);
        RecordingSink sink = new RecordingSink();
        StreamChatSession session = new StreamChatSession("7", "Oi", "briefing");

        useCase.stream(session, sink);

        assertEquals(List.of("start:7", "status:generating"), sink.events);
        verify(gateway).streamReply(eq("7"), eq("Oi"), eq("briefing"), eq(sink));
    }

    @Test
    @DisplayName("emits an error event when the gateway disappears after prepare")
    void errorsWhenGatewayMissingDuringStream() {
        when(anaChatGateway.getIfAvailable()).thenReturn(null);
        RecordingSink sink = new RecordingSink();

        useCase.stream(new StreamChatSession("7", "Oi", "briefing"), sink);

        assertEquals(List.of("error:CHAT_UNAVAILABLE"), sink.events);
    }

    private static final class RecordingSink implements ChatStreamSink {

        private final List<String> events = new ArrayList<>();

        @Override
        public void start(String conversationId) {
            events.add("start:" + conversationId);
        }

        @Override
        public void status(String phase) {
            events.add("status:" + phase);
        }

        @Override
        public void delta(String chunk) {
            events.add("delta:" + chunk);
        }

        @Override
        public void complete(String fullContent) {
            events.add("done:" + fullContent);
        }

        @Override
        public void error(String code, String message) {
            events.add("error:" + code);
        }
    }
}

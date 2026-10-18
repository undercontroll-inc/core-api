package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.exception.ConversationBusyException;
import com.undercontroll.domain.exception.ConversationMemoryUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.AnaConversationMemory;
import com.undercontroll.domain.gateway.AnaConversationMemory.ConversationLock;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.ShopSnapshotLoader;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.model.chat.ShopSnapshot;
import com.undercontroll.domain.usecase.chat.AnaChatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnaTurnPreparerTest {

    private static final ConversationKey KEY = new ConversationKey(7, "kitchen");
    private static final Duration LOCK_TTL = Duration.ofSeconds(90);

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
    @Mock
    private AnaConversationMemory memory;
    @Mock
    private ConversationLock lock;

    private AnaTurnPreparer preparer;

    @BeforeEach
    void setUp() {
        preparer = new AnaTurnPreparer(anaChatGateway, currentUserIdPort, shopSnapshotLoader, orderGateway, memory, LOCK_TTL);
    }

    @Test
    @DisplayName("reads history once and uses it for cited orders and the web search decision")
    void preparesTurnFromHistory() {
        ready();
        when(memory.tryLock(KEY, LOCK_TTL)).thenReturn(Optional.of(lock));
        when(memory.load(KEY)).thenReturn(List.of(
                ChatMessage.user("Me fala do pedido 12 e do manual"),
                ChatMessage.assistant("Ok")
        ));
        when(orderGateway.findDetailById(12)).thenReturn(Optional.of(Order.builder()
                .id(12)
                .status(OrderStatus.PENDING)
                .user(User.builder().name("Maria").lastName("Souza").build())
                .orderItems(List.of(OrderItem.builder().type("liquidificador").build()))
                .build()));

        AnaChatSession session = preparer.prepare("E a voltagem?", "kitchen");

        assertEquals(KEY, session.key());
        assertTrue(session.memoryAvailable());
        assertEquals(2, session.turn().history().size());
        assertTrue(session.turn().webSearch());
        assertTrue(session.turn().briefing().contains("Maria"));
        assertEquals("kitchen", session.turn().conversationId());
        verify(memory, times(1)).load(KEY);
    }

    @Test
    @DisplayName("blank conversation id falls back to the default conversation")
    void defaultConversation() {
        ready();
        ConversationKey key = new ConversationKey(7, ConversationKey.DEFAULT_ID);
        when(memory.tryLock(key, LOCK_TTL)).thenReturn(Optional.of(lock));
        when(memory.load(key)).thenReturn(List.of());

        assertEquals(ConversationKey.DEFAULT_ID, preparer.prepare("Oi", " ").conversationId());
    }

    @Test
    @DisplayName("a turn already running in the same conversation is rejected as busy")
    void busy() {
        ready();
        when(memory.tryLock(KEY, LOCK_TTL)).thenReturn(Optional.empty());

        assertThrows(ConversationBusyException.class, () -> preparer.prepare("Oi", "kitchen"));
        verify(memory, never()).load(any());
    }

    @Test
    @DisplayName("memory outage answers without history, lock or persistence")
    void degradedWhenMemoryDown() {
        ready();
        when(memory.tryLock(KEY, LOCK_TTL)).thenThrow(new ConversationMemoryUnavailableException(new IllegalStateException("redis")));

        AnaChatSession session = preparer.prepare("Oi", "kitchen");
        preparer.finish(session, AnaReply.completed("Olá"));

        assertFalse(session.memoryAvailable());
        assertNull(session.lock());
        assertTrue(session.turn().history().isEmpty());
        verify(memory, never()).append(any(), any(), any());
    }

    @Test
    @DisplayName("failed history read keeps the lock but skips persistence")
    void historyReadFails() {
        ready();
        when(memory.tryLock(KEY, LOCK_TTL)).thenReturn(Optional.of(lock));
        when(memory.load(KEY)).thenThrow(new ConversationMemoryUnavailableException(new IllegalStateException("redis")));

        AnaChatSession session = preparer.prepare("Oi", "kitchen");
        preparer.finish(session, AnaReply.completed("Olá"));

        assertFalse(session.memoryAvailable());
        verify(memory, never()).append(any(), any(), any());
        verify(lock).release();
    }

    @Test
    @DisplayName("completed replies are appended once and the lock is released")
    void finishCompleted() {
        AnaChatSession session = session(true);

        preparer.finish(session, AnaReply.completed("Tem 2."));

        verify(memory).append(KEY, ChatMessage.user("Oi"), ChatMessage.assistant("Tem 2."));
        verify(lock).release();
    }

    @Test
    @DisplayName("partial or missing replies are not stored")
    void finishPartial() {
        AnaChatSession session = session(true);

        preparer.finish(session, AnaReply.partial("Tem", "max_tokens"));
        preparer.finish(session, null);
        preparer.abandon(session);

        verify(memory, never()).append(any(), any(), any());
        verify(lock, times(3)).release();
    }

    @Test
    @DisplayName("a memory write failure does not fail the turn and lock release errors are swallowed")
    void finishSwallowsFailures() {
        AnaChatSession session = session(true);
        doThrow(new ConversationMemoryUnavailableException(new IllegalStateException("redis")))
                .when(memory).append(any(), any(), any());
        doThrow(new IllegalStateException("redis")).when(lock).release();

        preparer.finish(session, AnaReply.completed("Tem 2."));

        verify(lock).release();
    }

    @Test
    @DisplayName("an error while preparing releases the lock")
    void prepareFailureReleasesLock() {
        ready();
        when(memory.tryLock(KEY, LOCK_TTL)).thenReturn(Optional.of(lock));
        when(memory.load(KEY)).thenReturn(List.of());
        when(shopSnapshotLoader.load()).thenThrow(new IllegalStateException("db"));

        assertThrows(IllegalStateException.class, () -> preparer.prepare("Oi", "kitchen"));
        verify(lock).release();
    }

    @Test
    @DisplayName("missing Ana gateway is unavailable before touching memory")
    void missingGateway() {
        when(anaChatGateway.getIfAvailable()).thenReturn(null);

        assertThrows(AnaUnavailableException.class, () -> preparer.prepare("Oi", "kitchen"));
        verify(memory, never()).tryLock(any(), any());
    }

    private void ready() {
        when(anaChatGateway.getIfAvailable()).thenReturn(gateway);
        when(currentUserIdPort.require()).thenReturn(7);
        lenient().when(shopSnapshotLoader.load()).thenReturn(ShopSnapshot.empty());
    }

    private AnaChatSession session(boolean memoryAvailable) {
        return new AnaChatSession(
                KEY,
                new AnaTurnRequest("kitchen", "Oi", "b", List.of(), false),
                lock,
                memoryAvailable
        );
    }
}

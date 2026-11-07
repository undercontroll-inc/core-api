package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.exception.ConversationNotFoundException;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AppendedMessage;
import com.undercontroll.domain.model.chat.ChatCursor;
import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.model.chat.StoredChatMessage;
import com.undercontroll.infrastructure.mapper.ChatPersistenceMapperImpl;
import com.undercontroll.infrastructure.persistence.repository.ChatConversationJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.ChatMessageJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ConversationGatewayImpl.class, ChatPersistenceMapperImpl.class})
class ConversationGatewayImplTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

    @Autowired
    private ConversationGatewayImpl gateway;

    @Autowired
    private ChatConversationJpaRepository conversations;

    @Autowired
    private ChatMessageJpaRepository messages;

    @AfterEach
    void cleanUp() {
        messages.deleteAll();
        conversations.deleteAll();
    }

    @Test
    @DisplayName("first message creates the conversation with an automatic title; replies advance seq and summary")
    void appendsTurn() {
        ConversationKey key = new ConversationKey(1, "kitchen");
        AppendedMessage user = gateway.appendUserMessage(key, "Quantos motores temos?", NOW);
        AppendedMessage reply = gateway.appendAssistantMessage(user.conversation().id(), "Temos 2 motores.",
                AnaReply.State.COMPLETED, null, NOW.plusSeconds(3));

        assertEquals(1, user.seq());
        assertEquals(2, reply.seq());
        Conversation conversation = gateway.findActive(1, "kitchen").orElseThrow();
        assertEquals("Quantos motores temos?", conversation.title());
        assertEquals(Conversation.TitleSource.AUTO, conversation.titleSource());
        assertEquals("Temos 2 motores.", conversation.preview());
        assertEquals(2, conversation.messageCount());
        assertEquals(2, conversation.lastSeq());
        assertEquals(NOW.plusSeconds(3), conversation.lastMessageAt());

        List<StoredChatMessage> latest = gateway.messagesBefore(conversation.id(), null, 10);
        assertEquals(List.of(2, 1), latest.stream().map(StoredChatMessage::seq).toList());
        assertEquals(ChatMessage.Role.ASSISTANT, latest.getFirst().role());
        assertEquals(List.of(1), gateway.messagesBefore(conversation.id(), 2, 10).stream().map(StoredChatMessage::seq).toList());
    }

    @Test
    @DisplayName("the same public id is isolated per user")
    void isolatesUsers() {
        gateway.appendUserMessage(new ConversationKey(1, "default"), "Oi", NOW);
        gateway.appendUserMessage(new ConversationKey(2, "default"), "Olá", NOW);

        assertEquals("Oi", gateway.findActive(1, "default").orElseThrow().title());
        assertEquals("Olá", gateway.findActive(2, "default").orElseThrow().title());
        assertTrue(gateway.findActive(3, "default").isEmpty());
        assertEquals(1, gateway.list(1, null, 10).size());
    }

    @Test
    @DisplayName("list walks by last activity with a keyset cursor and no duplicates")
    void listsWithKeyset() {
        for (int i = 0; i < 5; i++) {
            gateway.appendUserMessage(new ConversationKey(1, "c" + i), "Pergunta " + i, NOW.plusSeconds(i));
        }
        gateway.appendUserMessage(new ConversationKey(1, "c0"), "De novo", NOW.plusSeconds(10));

        List<Conversation> first = gateway.list(1, null, 2);
        List<Conversation> second = gateway.list(1, cursor(first.getLast()), 2);
        List<Conversation> third = gateway.list(1, cursor(second.getLast()), 2);

        List<String> order = new ArrayList<>();
        first.forEach(c -> order.add(c.publicId()));
        second.forEach(c -> order.add(c.publicId()));
        third.forEach(c -> order.add(c.publicId()));
        assertEquals(List.of("c0", "c4", "c3", "c2", "c1"), order);
    }

    @Test
    @DisplayName("rename sets a user title; deleted conversations disappear and reject new messages")
    void renameAndDelete() {
        ConversationKey key = new ConversationKey(1, "kitchen");
        gateway.appendUserMessage(key, "Oi", NOW);

        Conversation renamed = gateway.rename(1, "kitchen", "Estoque", NOW.plusSeconds(1)).orElseThrow();
        assertEquals("Estoque", renamed.title());
        assertEquals(Conversation.TitleSource.USER, renamed.titleSource());
        assertTrue(gateway.rename(2, "kitchen", "x", NOW).isEmpty());

        assertTrue(gateway.softDelete(1, "kitchen", NOW.plusSeconds(2)));
        assertFalse(gateway.softDelete(1, "kitchen", NOW.plusSeconds(3)));
        assertTrue(gateway.findActive(1, "kitchen").isEmpty());
        assertTrue(gateway.list(1, null, 10).isEmpty());
        assertTrue(gateway.rename(1, "kitchen", "x", NOW).isEmpty());
        assertThrows(ConversationNotFoundException.class, () -> gateway.appendUserMessage(key, "Voltei", NOW));
    }

    @Test
    @DisplayName("purge removes conversations deleted before the cutoff with their messages")
    void purges() {
        AppendedMessage old = gateway.appendUserMessage(new ConversationKey(1, "old"), "Oi", NOW);
        gateway.appendUserMessage(new ConversationKey(1, "recent"), "Oi", NOW);
        gateway.softDelete(1, "old", NOW);
        gateway.softDelete(1, "recent", NOW.plus(Duration.ofDays(40)));

        assertEquals(1, gateway.purgeDeleted(NOW.plus(Duration.ofDays(30))));
        assertTrue(conversations.findById(old.conversation().id()).isEmpty());
        assertTrue(gateway.messagesBefore(old.conversation().id(), null, 10).isEmpty());
        assertEquals(1, conversations.count());
    }

    @Test
    @DisplayName("concurrent appends to one conversation get distinct consecutive seq numbers")
    void concurrentAppends() throws Exception {
        ConversationKey key = new ConversationKey(1, "busy");
        gateway.appendUserMessage(key, "Primeira", NOW);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Integer>> tasks = IntStream.range(0, 8)
                    .<Callable<Integer>>mapToObj(i -> () -> gateway.appendUserMessage(key, "Msg " + i, NOW).seq())
                    .toList();
            List<Integer> seqs = new ArrayList<>();
            for (Future<Integer> future : pool.invokeAll(tasks)) {
                seqs.add(future.get());
            }
            assertEquals(IntStream.rangeClosed(2, 9).boxed().toList(), seqs.stream().sorted().toList());
        } finally {
            pool.shutdownNow();
        }
        Conversation conversation = gateway.findActive(1, "busy").orElseThrow();
        assertEquals(9, conversation.lastSeq());
        assertEquals(9, conversation.messageCount());
    }

    @Test
    @DisplayName("concurrent first messages create the conversation once")
    void concurrentCreate() throws Exception {
        ConversationKey key = new ConversationKey(1, "fresh");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Integer>> tasks = IntStream.range(0, 4)
                    .<Callable<Integer>>mapToObj(i -> () -> gateway.appendUserMessage(key, "Oi " + i, NOW).seq())
                    .toList();
            List<Integer> seqs = new ArrayList<>();
            for (Future<Integer> future : pool.invokeAll(tasks)) {
                seqs.add(future.get());
            }
            assertEquals(List.of(1, 2, 3, 4), seqs.stream().sorted().toList());
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, conversations.count());
    }

    private static ChatCursor.ConversationPosition cursor(Conversation last) {
        return ChatCursor.parseConversations(ChatCursor.conversations(last));
    }
}

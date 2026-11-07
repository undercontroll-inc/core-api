package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.gateway.ConversationGateway;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.ChatCursor;
import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.ConversationPage;
import com.undercontroll.domain.model.chat.MessagePage;
import com.undercontroll.domain.model.chat.StoredChatMessage;
import com.undercontroll.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(ConversationReadQueryCacheTest.Config.class)
class ConversationReadQueryCacheTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

    @Configuration
    @EnableCaching
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        ConversationReadQuery conversationReadQuery(ConversationGateway conversations) {
            return new ConversationReadQuery(conversations);
        }
    }

    @MockitoBean
    private ConversationGateway conversations;

    @Autowired
    private ConversationReadQuery query;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        clearInvocations(conversations);
        when(conversations.list(eq(1), any(), anyInt())).thenReturn(List.of(conversation(3L), conversation(2L), conversation(1L)));
        when(conversations.messagesBefore(eq(9L), any(), anyInt())).thenReturn(List.of(message(3), message(2), message(1)));
    }

    @Test
    @DisplayName("the list is cached per version; a bumped version reads again without evicting anything")
    void listVersioned() {
        ConversationPage page = query.conversations(1, 4, null, 2);
        query.conversations(1, 4, null, 2);
        verify(conversations, times(1)).list(1, null, 3);
        assertEquals(2, page.items().size());
        assertEquals(2L, ChatCursor.parseConversations(page.nextCursor()).id());

        query.conversations(1, 5, null, 2);
        verify(conversations, times(2)).list(1, null, 3);
    }

    @Test
    @DisplayName("an unavailable version skips the cache")
    void listWithoutVersion() {
        query.conversations(1, -1, null, 2);
        query.conversations(1, -1, null, 2);
        verify(conversations, times(2)).list(1, null, 3);
    }

    @Test
    @DisplayName("the latest page is keyed by lastSeq and older pages are cached as immutable")
    void messagesVersioned() {
        MessagePage latest = query.messages(9L, 3, null, 2);
        query.messages(9L, 3, null, 2);
        verify(conversations, times(1)).messagesBefore(eq(9L), isNull(), eq(3));
        assertEquals(List.of(2, 3), latest.items().stream().map(StoredChatMessage::seq).toList());
        assertEquals(2, ChatCursor.parseMessages(latest.nextCursor()));

        query.messages(9L, 4, null, 2);
        verify(conversations, times(2)).messagesBefore(eq(9L), isNull(), eq(3));

        query.messages(9L, 4, 2, 2);
        query.messages(9L, 7, 2, 2);
        verify(conversations, times(1)).messagesBefore(9L, 2, 3);
    }

    @Test
    @DisplayName("cached pages survive the Redis JSON serializer round-trip")
    void redisRoundTrip() {
        GenericJackson2JsonRedisSerializer serializer = new RedisConfig().redisSerializer();
        ConversationPage page = query.conversations(1, 1, null, 2);
        MessagePage messages = query.messages(9L, 3, null, 5);

        assertEquals(page, serializer.deserialize(serializer.serialize(page)));
        assertEquals(messages, serializer.deserialize(serializer.serialize(messages)));
        assertNull(messages.nextCursor());
    }

    private static Conversation conversation(long id) {
        return new Conversation(id, 1, "c" + id, "t", Conversation.TitleSource.AUTO, "p", 1, 1, NOW, NOW, NOW.plusSeconds(id));
    }

    private static StoredChatMessage message(int seq) {
        return new StoredChatMessage((long) seq, seq, ChatMessage.Role.USER, "m" + seq, AnaReply.State.COMPLETED, null, NOW);
    }
}

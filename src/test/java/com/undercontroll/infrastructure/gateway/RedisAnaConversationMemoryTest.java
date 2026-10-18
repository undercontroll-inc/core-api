package com.undercontroll.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.exception.ConversationMemoryUnavailableException;
import com.undercontroll.domain.gateway.AnaConversationMemory.ConversationLock;
import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.ConversationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAnaConversationMemoryTest {

    private static final ConversationKey KEY = new ConversationKey(7, "kitchen");

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ListOperations<String, String> lists;

    @Mock
    private ValueOperations<String, String> values;

    private RedisAnaConversationMemory memory;

    @BeforeEach
    void setUp() {
        memory = new RedisAnaConversationMemory(redis, new ObjectMapper(), 10, 24);
    }

    @Test
    @DisplayName("keys are scoped by user and conversation")
    void keys() {
        assertEquals("ana:memory:7:kitchen", RedisAnaConversationMemory.memoryKey(KEY));
        assertEquals("ana:lock:7:kitchen", RedisAnaConversationMemory.lockKey(KEY));
    }

    @Test
    @DisplayName("loads only the configured window of messages")
    void loadsWindow() {
        when(redis.opsForList()).thenReturn(lists);
        when(lists.range("ana:memory:7:kitchen", -10, -1)).thenReturn(List.of(
                "{\"role\":\"USER\",\"text\":\"Oi\"}",
                "{\"role\":\"ASSISTANT\",\"text\":\"Olá\"}"
        ));

        assertEquals(List.of(ChatMessage.user("Oi"), ChatMessage.assistant("Olá")), memory.load(KEY));
    }

    @Test
    @DisplayName("missing conversation is empty history")
    void loadsNothing() {
        when(redis.opsForList()).thenReturn(lists);
        when(lists.range(anyString(), eq(-10L), eq(-1L))).thenReturn(null);

        assertTrue(memory.load(KEY).isEmpty());
    }

    @Test
    @DisplayName("Redis failures and corrupt entries surface as unavailable memory, never as empty history")
    void loadFailures() {
        when(redis.opsForList()).thenReturn(lists);
        when(lists.range(anyString(), eq(-10L), eq(-1L)))
                .thenThrow(new RedisConnectionFailureException("down"))
                .thenReturn(List.of("not json"));

        assertThrows(ConversationMemoryUnavailableException.class, () -> memory.load(KEY));
        assertThrows(ConversationMemoryUnavailableException.class, () -> memory.load(KEY));
    }

    @Test
    @DisplayName("appends the user and assistant messages in one atomic script with trim and TTL")
    void appendsAtomically() {
        memory.append(KEY, ChatMessage.user("Oi"), ChatMessage.assistant("Olá"));

        verify(redis).execute(
                RedisAnaConversationMemory.APPEND,
                List.of("ana:memory:7:kitchen"),
                "{\"role\":\"USER\",\"text\":\"Oi\"}",
                "{\"role\":\"ASSISTANT\",\"text\":\"Olá\"}",
                "10",
                String.valueOf(Duration.ofHours(24).toMillis())
        );
    }

    @Test
    @DisplayName("append failures are reported as unavailable memory")
    void appendFailure() {
        when(redis.execute(eq(RedisAnaConversationMemory.APPEND), any(List.class), any(), any(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("down"));
        ChatMessage user = ChatMessage.user("Oi");
        ChatMessage assistant = ChatMessage.assistant("Olá");

        assertThrows(ConversationMemoryUnavailableException.class, () -> memory.append(KEY, user, assistant));
    }

    @Test
    @DisplayName("lock is exclusive and released only by its owner token")
    void locks() {
        when(redis.opsForValue()).thenReturn(values);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        when(values.setIfAbsent(eq("ana:lock:7:kitchen"), token.capture(), eq(Duration.ofSeconds(90))))
                .thenReturn(true)
                .thenReturn(false);

        Optional<ConversationLock> first = memory.tryLock(KEY, Duration.ofSeconds(90));
        Optional<ConversationLock> second = memory.tryLock(KEY, Duration.ofSeconds(90));
        first.orElseThrow().release();

        assertTrue(second.isEmpty());
        verify(redis).execute(RedisAnaConversationMemory.RELEASE, List.of("ana:lock:7:kitchen"), token.getAllValues().get(0));
    }

    @Test
    @DisplayName("lock attempts against a down Redis are reported as unavailable memory")
    void lockFailure() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("down"));
        Duration ttl = Duration.ofSeconds(90);

        assertThrows(ConversationMemoryUnavailableException.class, () -> memory.tryLock(KEY, ttl));
    }
}

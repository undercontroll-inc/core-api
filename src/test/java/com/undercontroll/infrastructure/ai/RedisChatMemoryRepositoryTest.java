package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChatMemoryRepositoryTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    private RedisChatMemoryRepository repository;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        repository = new RedisChatMemoryRepository(redis, new ObjectMapper(), 24);
    }

    @Test
    @DisplayName("round-trips user and assistant messages")
    void roundTrip() {
        when(values.get("ana:memory:12")).thenReturn(
                "[{\"type\":\"USER\",\"text\":\"Oi\"},{\"type\":\"ASSISTANT\",\"text\":\"Olá\"}]"
        );

        var loaded = repository.findByConversationId("12");
        assertEquals(2, loaded.size());
        assertEquals("Oi", loaded.get(0).getText());

        repository.saveAll("12", List.of(new UserMessage("Oi"), new AssistantMessage("Olá")));
        verify(values).set(
                "ana:memory:12",
                "[{\"type\":\"USER\",\"text\":\"Oi\"},{\"type\":\"ASSISTANT\",\"text\":\"Olá\"}]",
                Duration.ofHours(24)
        );
    }

    @Test
    @DisplayName("does not persist system briefing messages")
    void skipsSystem() {
        when(values.get("ana:memory:12")).thenReturn(
                "[{\"type\":\"SYSTEM\",\"text\":\"briefing\"},{\"type\":\"USER\",\"text\":\"Oi\"}]"
        );

        var loaded = repository.findByConversationId("12");
        assertEquals(1, loaded.size());
        assertEquals("Oi", loaded.get(0).getText());

        repository.saveAll("12", List.of(
                new org.springframework.ai.chat.messages.SystemMessage("briefing"),
                new UserMessage("Oi"),
                new AssistantMessage("Olá")
        ));
        verify(values).set(
                "ana:memory:12",
                "[{\"type\":\"USER\",\"text\":\"Oi\"},{\"type\":\"ASSISTANT\",\"text\":\"Olá\"}]",
                Duration.ofHours(24)
        );
    }
}

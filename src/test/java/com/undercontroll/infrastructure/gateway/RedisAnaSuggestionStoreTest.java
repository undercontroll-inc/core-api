package com.undercontroll.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAnaSuggestionStoreTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    private RedisAnaSuggestionStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        store = new RedisAnaSuggestionStore(redis, new ObjectMapper(), 12);
    }

    @Test
    @DisplayName("saves suggestions as JSON with the configured TTL")
    void saves() {
        store.save(3, List.of("Pergunta 1"));

        verify(values).set("ana:suggestions:3", "[\"Pergunta 1\"]", Duration.ofHours(12));
    }

    @Test
    @DisplayName("reads stored suggestions and ignores missing, empty or broken values")
    void reads() {
        when(values.get("ana:suggestions:3")).thenReturn("[\"Pergunta 1\"]");
        when(values.get("ana:suggestions:4")).thenReturn("[]");
        when(values.get("ana:suggestions:5")).thenReturn("{broken");
        when(values.get("ana:suggestions:6")).thenReturn(null);

        assertEquals(Optional.of(List.of("Pergunta 1")), store.findByUserId(3));
        assertTrue(store.findByUserId(4).isEmpty());
        assertTrue(store.findByUserId(5).isEmpty());
        assertTrue(store.findByUserId(6).isEmpty());
    }

    @Test
    @DisplayName("save failures surface as illegal state")
    void saveFailure() {
        doThrow(new IllegalStateException("redis down")).when(values).set(anyString(), anyString(), any(Duration.class));

        assertThrows(IllegalStateException.class, () -> store.save(3, List.of("x")));
    }
}

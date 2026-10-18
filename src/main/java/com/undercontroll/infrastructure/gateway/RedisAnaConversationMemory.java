package com.undercontroll.infrastructure.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.exception.ConversationMemoryUnavailableException;
import com.undercontroll.domain.gateway.AnaConversationMemory;
import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.ConversationKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisAnaConversationMemory implements AnaConversationMemory {

    static final String MEMORY_PREFIX = "ana:memory:";
    static final String LOCK_PREFIX = "ana:lock:";

    static final RedisScript<Long> APPEND = new DefaultRedisScript<>("""
            redis.call('RPUSH', KEYS[1], ARGV[1], ARGV[2])
            redis.call('LTRIM', KEYS[1], -tonumber(ARGV[3]), -1)
            redis.call('PEXPIRE', KEYS[1], ARGV[4])
            return redis.call('LLEN', KEYS[1])
            """, Long.class);

    static final RedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final int window;
    private final Duration ttl;

    public RedisAnaConversationMemory(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${undercontroll.ai.ana.memory-window:10}") int window,
            @Value("${undercontroll.ai.ana.memory-ttl-hours:24}") int ttlHours
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.window = Math.max(2, window);
        this.ttl = Duration.ofHours(ttlHours);
    }

    @Override
    public List<ChatMessage> load(ConversationKey key) {
        try {
            List<String> entries = redis.opsForList().range(memoryKey(key), -window, -1);
            if (entries == null || entries.isEmpty()) {
                return List.of();
            }
            List<ChatMessage> messages = new ArrayList<>(entries.size());
            for (String entry : entries) {
                messages.add(objectMapper.readValue(entry, ChatMessage.class));
            }
            return List.copyOf(messages);
        } catch (JsonProcessingException | RuntimeException ex) {
            throw new ConversationMemoryUnavailableException(ex);
        }
    }

    @Override
    public void append(ConversationKey key, ChatMessage user, ChatMessage assistant) {
        try {
            redis.execute(
                    APPEND,
                    List.of(memoryKey(key)),
                    objectMapper.writeValueAsString(user),
                    objectMapper.writeValueAsString(assistant),
                    String.valueOf(window),
                    String.valueOf(ttl.toMillis())
            );
        } catch (JsonProcessingException | RuntimeException ex) {
            throw new ConversationMemoryUnavailableException(ex);
        }
    }

    @Override
    public Optional<ConversationLock> tryLock(ConversationKey key, Duration lockTtl) {
        String lockKey = lockKey(key);
        String token = UUID.randomUUID().toString();
        Boolean acquired;
        try {
            acquired = redis.opsForValue().setIfAbsent(lockKey, token, lockTtl);
        } catch (RuntimeException ex) {
            throw new ConversationMemoryUnavailableException(ex);
        }
        if (!Boolean.TRUE.equals(acquired)) {
            return Optional.empty();
        }
        return Optional.of(() -> redis.execute(RELEASE, List.of(lockKey), token));
    }

    static String memoryKey(ConversationKey key) {
        return MEMORY_PREFIX + key.userId() + ":" + key.conversationId();
    }

    static String lockKey(ConversationKey key) {
        return LOCK_PREFIX + key.userId() + ":" + key.conversationId();
    }
}

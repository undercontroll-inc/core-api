package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository("anaChatMemoryRepository")
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    static final String KEY_PREFIX = "ana:memory:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration memoryTtl;

    public RedisChatMemoryRepository(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${undercontroll.ai.ana.memory-ttl-hours:24}") int memoryTtlHours
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.memoryTtl = Duration.ofHours(memoryTtlHours);
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redis.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(key -> key.substring(KEY_PREFIX.length()))
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String json = redis.opsForValue().get(key(conversationId));
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<StoredMessage> stored = objectMapper.readValue(json, new TypeReference<>() {
            });
            List<Message> messages = new ArrayList<>();
            for (StoredMessage item : stored) {
                Message mapped = toMessage(item);
                if (mapped != null) {
                    messages.add(mapped);
                }
            }
            return messages;
        } catch (Exception ex) {
            log.warn("Chat memory deserialize failed conversationId={}", conversationId, ex);
            return List.of();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<StoredMessage> stored = messages == null
                ? List.of()
                : messages.stream()
                .map(RedisChatMemoryRepository::fromMessage)
                .filter(item -> item != null)
                .toList();
        try {
            String json = objectMapper.writeValueAsString(stored);
            String redisKey = key(conversationId);
            redis.opsForValue().set(redisKey, json, memoryTtl);
        } catch (Exception ex) {
            log.warn("Chat memory save failed conversationId={}", conversationId, ex);
            throw new IllegalStateException("Failed to persist chat memory", ex);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redis.delete(key(conversationId));
    }

    private static String key(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    private static StoredMessage fromMessage(Message message) {
        if (message == null || message.getMessageType() == null) {
            return null;
        }
        MessageType type = message.getMessageType();
        if (type != MessageType.USER && type != MessageType.ASSISTANT) {
            return null;
        }
        return new StoredMessage(type.name(), message.getText());
    }

    private static Message toMessage(StoredMessage stored) {
        if (stored == null || stored.type() == null) {
            return null;
        }
        String text = stored.text() == null ? "" : stored.text();
        return switch (stored.type()) {
            case "USER" -> new UserMessage(text);
            case "ASSISTANT" -> new AssistantMessage(text);
            default -> null;
        };
    }

    public record StoredMessage(String type, String text) {
    }
}

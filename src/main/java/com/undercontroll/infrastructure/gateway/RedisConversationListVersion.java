package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.gateway.ConversationListVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class RedisConversationListVersion implements ConversationListVersion {

    static final String PREFIX = "ana:chats:ver:";
    static final Duration TTL = Duration.ofDays(1);

    static final RedisScript<Long> BUMP = new DefaultRedisScript<>("""
            local version = redis.call('INCR', KEYS[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[1])
            return version
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisConversationListVersion(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long current(Integer userId) {
        try {
            String value = redis.opsForValue().get(key(userId));
            return value == null ? 0L : Long.parseLong(value);
        } catch (RuntimeException ex) {
            log.warn("Conversation list version unavailable, skipping cache cause={}", ex.toString());
            return UNAVAILABLE;
        }
    }

    @Override
    public void bump(Integer userId) {
        try {
            redis.execute(BUMP, List.of(key(userId)), String.valueOf(TTL.toMillis()));
        } catch (RuntimeException ex) {
            log.warn("Conversation list version bump failed cause={}", ex.toString());
        }
    }

    static String key(Integer userId) {
        return PREFIX + userId;
    }
}

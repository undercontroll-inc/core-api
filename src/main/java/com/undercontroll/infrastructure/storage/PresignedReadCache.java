package com.undercontroll.infrastructure.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.undercontroll.domain.storage.StorageArea;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class PresignedReadCache {

    record Signed(String url, Instant expiresAt) {
    }

    private final Cache<String, Signed> cache;
    private final Clock clock;

    public PresignedReadCache(Clock clock, long maximumSize) {
        this.clock = clock;
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(Duration.ofHours(1))
                .build();
    }

    public String get(StorageArea area, String key, Supplier<String> signer) {
        Duration ttl = area.policy().readTtl();
        String cacheKey = area.name() + ":" + key;
        Instant now = clock.instant();
        Signed cached = cache.getIfPresent(cacheKey);
        if (cached != null && Duration.between(now, cached.expiresAt()).compareTo(ttl.dividedBy(2)) >= 0) {
            return cached.url();
        }
        String url = signer.get();
        cache.put(cacheKey, new Signed(url, now.plus(ttl)));
        return url;
    }

    long size() {
        cache.cleanUp();
        return cache.estimatedSize();
    }
}

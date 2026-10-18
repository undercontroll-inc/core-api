package com.undercontroll.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    @Test
    @DisplayName("cache keys carry a version prefix so entries written with older value types are not read back")
    void versionedKeys() {
        RedisCacheConfiguration configuration = new RedisConfig().cacheConfiguration();

        assertThat(configuration.getKeyPrefixFor("orders")).isEqualTo("v2:orders::");
        assertThat(configuration.getTtlFunction().getTimeToLive("k", "v")).isEqualTo(Duration.ofMinutes(10));
        assertThat(configuration.getAllowCacheNullValues()).isFalse();
    }
}

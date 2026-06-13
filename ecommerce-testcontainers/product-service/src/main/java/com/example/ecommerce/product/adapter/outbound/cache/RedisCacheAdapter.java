package com.example.ecommerce.product.adapter.outbound.cache;

import com.example.ecommerce.product.domain.port.outbound.CachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache adapter using Redis + JSON. Implementation is intentionally simple:
 * any non-trivial value is serialised to JSON via Jackson.
 */
@Component
public class RedisCacheAdapter implements CachePort {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisCacheAdapter(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        String raw = redis.opsForValue().get(key);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(raw, type));
        } catch (Exception e) {
            throw new IllegalStateException("failed to deserialise cache value at " + key, e);
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialise cache value at " + key, e);
        }
    }

    @Override
    public void invalidate(String key) {
        redis.delete(key);
    }
}

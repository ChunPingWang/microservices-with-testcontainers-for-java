package com.example.ecommerce.product.adapter.cache;

import com.example.ecommerce.product.adapter.outbound.cache.RedisCacheAdapter;
import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase.ProductView;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure adapter integration test — no Spring context, no auto-config.
 * Faster to start than @SpringBootTest and proves the adapter works in
 * isolation against a real Redis container.
 */
@Tag("integration")
class RedisCacheAdapterIT {

    @SuppressWarnings("resource")
    static GenericContainer<?> redis;
    static LettuceConnectionFactory connectionFactory;
    static StringRedisTemplate template;
    static RedisCacheAdapter cache;

    @BeforeAll
    static void startup() {
        PodmanCompatibility.apply();
        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redis.start();
        var standalone = new RedisStandaloneConfiguration(redis.getHost(), redis.getFirstMappedPort());
        connectionFactory = new LettuceConnectionFactory(standalone);
        connectionFactory.afterPropertiesSet();
        template = new StringRedisTemplate(connectionFactory);
        cache = new RedisCacheAdapter(template, new ObjectMapper());
    }

    @AfterAll
    static void shutdown() {
        if (connectionFactory != null) connectionFactory.destroy();
        if (redis != null) redis.stop();
    }

    @BeforeEach
    void clear() {
        template.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void put_then_get_roundtrip() {
        ProductView view = new ProductView(
                UUID.randomUUID().toString(), "SKU-A", "Apple",
                new BigDecimal("1.99"), "USD", true);
        cache.put("k1", view, Duration.ofMinutes(1));
        Optional<ProductView> read = cache.get("k1", ProductView.class);
        assertThat(read).contains(view);
    }

    @Test
    void miss_returns_empty() {
        assertThat(cache.get("missing", ProductView.class)).isEmpty();
    }

    @Test
    void invalidate_removes() {
        cache.put("k2", "hello", Duration.ofMinutes(1));
        assertThat(cache.get("k2", String.class)).contains("hello");
        cache.invalidate("k2");
        assertThat(cache.get("k2", String.class)).isEmpty();
    }

    @Test
    void expires_after_ttl() throws InterruptedException {
        cache.put("k3", "soon-gone", Duration.ofMillis(200));
        assertThat(cache.get("k3", String.class)).isPresent();
        Thread.sleep(500);
        assertThat(cache.get("k3", String.class)).isEmpty();
    }
}

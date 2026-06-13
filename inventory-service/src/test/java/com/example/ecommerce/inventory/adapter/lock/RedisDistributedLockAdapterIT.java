package com.example.ecommerce.inventory.adapter.lock;

import com.example.ecommerce.inventory.adapter.outbound.lock.RedisDistributedLockAdapter;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class RedisDistributedLockAdapterIT {

    @SuppressWarnings("resource")
    static GenericContainer<?> redis;
    static RedissonClient redisson;
    static RedisDistributedLockAdapter adapter;

    @BeforeAll
    static void startup() {
        PodmanCompatibility.apply();
        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redis.start();

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redis.getHost() + ":" + redis.getFirstMappedPort());
        redisson = Redisson.create(config);
        adapter = new RedisDistributedLockAdapter(redisson);
    }

    @AfterAll
    static void shutdown() {
        if (redisson != null) redisson.shutdown();
        if (redis != null) redis.stop();
    }

    @Test
    void runs_action_under_lock() {
        int result = adapter.withLock("test-key", Duration.ofSeconds(1), Duration.ofSeconds(5), () -> 42);
        assertThat(result).isEqualTo(42);
    }

    @Test
    void serialises_concurrent_increments() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        int threads = 8;
        int incrementsPerThread = 25;

        ExecutorService es = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            es.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        adapter.withLock("inc-key", Duration.ofSeconds(5), Duration.ofSeconds(2), () -> {
                            int v = counter.get();
                            // Race window — without the lock, threads would overwrite each other.
                            try { Thread.sleep(1); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                            counter.set(v + 1);
                            return null;
                        });
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        es.shutdownNow();
        assertThat(counter.get()).isEqualTo(threads * incrementsPerThread);
    }
}

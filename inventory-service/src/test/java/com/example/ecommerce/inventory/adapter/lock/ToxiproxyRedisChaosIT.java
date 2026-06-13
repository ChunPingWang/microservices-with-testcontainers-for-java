package com.example.ecommerce.inventory.adapter.lock;

import com.example.ecommerce.inventory.adapter.outbound.lock.RedisDistributedLockAdapter;
import com.example.ecommerce.inventory.domain.port.outbound.DistributedLockPort;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chaos test: introduces controllable latency / connection failures between
 * the application and Redis using Toxiproxy. We verify the lock adapter
 * gracefully fails with {@link DistributedLockPort.LockAcquisitionFailedException}
 * rather than hanging indefinitely.
 */
@Tag("integration")
@org.junit.jupiter.api.Disabled("Toxiproxy 2.5 container port wiring with Testcontainers 1.20 needs revisiting — the lock adapter is exercised by RedisDistributedLockAdapterIT in the meantime.")
class ToxiproxyRedisChaosIT {

    @SuppressWarnings("resource")
    static Network net;
    @SuppressWarnings("resource")
    static GenericContainer<?> redis;
    static ToxiproxyContainer toxiproxy;
    static ToxiproxyClient toxiClient;
    static Proxy redisProxy;
    static RedissonClient redisson;
    static DistributedLockPort lock;

    @BeforeAll
    static void startup() throws IOException {
        PodmanCompatibility.apply();
        net = Network.newNetwork();

        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withNetwork(net)
                .withNetworkAliases("redis");
        redis.start();

        toxiproxy = new ToxiproxyContainer(DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.5.0"))
                .withNetwork(net)
                .withExposedPorts(8474, 8666);
        toxiproxy.start();

        toxiClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        redisProxy = toxiClient.createProxy("redis", "0.0.0.0:8666", "redis:6379");

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + toxiproxy.getHost() + ":" + toxiproxy.getMappedPort(8666))
                .setConnectTimeout(1500)
                .setTimeout(1500)
                .setRetryAttempts(0);
        redisson = Redisson.create(config);
        lock = new RedisDistributedLockAdapter(redisson);
    }

    @AfterAll
    static void shutdown() {
        if (redisson != null) redisson.shutdown();
        if (toxiproxy != null) toxiproxy.stop();
        if (redis != null) redis.stop();
        if (net != null) net.close();
    }

    @Test
    void normal_acquisition_succeeds() {
        Integer v = lock.withLock("chaos-normal", Duration.ofSeconds(2), Duration.ofSeconds(5), () -> 1);
        assertThat(v).isEqualTo(1);
    }

    @Test
    void cuts_connection_then_recovers() throws Exception {
        // Sever the connection — Redisson should fail fast
        redisProxy.disable();
        assertThatThrownBy(() -> lock.withLock("chaos-cut",
                Duration.ofMillis(500), Duration.ofSeconds(2), () -> 1))
                .isInstanceOf(DistributedLockPort.LockAcquisitionFailedException.class);

        // Restore
        redisProxy.enable();
        Integer v = lock.withLock("chaos-cut-restore", Duration.ofSeconds(5), Duration.ofSeconds(5), () -> 7);
        assertThat(v).isEqualTo(7);
    }

    @Test
    void high_latency_blows_lock_wait_timeout() throws Exception {
        // Add 2-second downstream latency; with waitFor=500ms acquisition will fail.
        redisProxy.toxics().latency("delay", ToxicDirection.DOWNSTREAM, 2000);
        try {
            assertThatThrownBy(() -> lock.withLock("chaos-latency",
                    Duration.ofMillis(500), Duration.ofSeconds(2), () -> 1))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            redisProxy.toxics().get("delay").remove();
        }
    }
}

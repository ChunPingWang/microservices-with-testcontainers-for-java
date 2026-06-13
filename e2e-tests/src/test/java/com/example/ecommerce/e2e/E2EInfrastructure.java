package com.example.ecommerce.e2e;

import com.example.ecommerce.test.podman.PodmanCompatibility;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

/**
 * Shared singletons for E2E. Bringing up these containers takes ~30-60s;
 * lazy-init lets us reuse them across tests in the same JVM.
 */
public final class E2EInfrastructure {

    public static final String VAULT_TOKEN = "e2e-root-token";

    static {
        PodmanCompatibility.apply();
    }

    private E2EInfrastructure() {}

    @SuppressWarnings("resource")
    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("postgres")
            .withUsername("postgres")
            .withPassword("postgres")
            // ports are mapped after start(); we'll create individual DBs in @BeforeAll
            .withReuse(false);

    @SuppressWarnings("resource")
    public static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    public static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @SuppressWarnings("resource")
    public static final VaultContainer<?> VAULT = new VaultContainer<>(
            DockerImageName.parse("hashicorp/vault:1.16"))
            .withVaultToken(VAULT_TOKEN);

    @SuppressWarnings("resource")
    public static final GenericContainer<?> MINIO = new GenericContainer<>(
            DockerImageName.parse("minio/minio:RELEASE.2024-08-17T01-24-54Z"))
            .withCommand("server", "/data")
            .withEnv("MINIO_ROOT_USER", "minio")
            .withEnv("MINIO_ROOT_PASSWORD", "minio12345")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    public static synchronized void startAll() {
        if (!POSTGRES.isRunning()) POSTGRES.start();
        if (!REDIS.isRunning()) REDIS.start();
        if (!KAFKA.isRunning()) KAFKA.start();
        if (!VAULT.isRunning()) VAULT.start();
        if (!MINIO.isRunning()) MINIO.start();
    }

    public static synchronized void stopAll() {
        // Containers are kept alive for reuse across multiple test classes in
        // the same JVM. They are torn down when the JVM exits.
    }
}

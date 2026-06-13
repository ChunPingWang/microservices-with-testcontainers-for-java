package com.example.ecommerce.test.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import com.example.ecommerce.test.podman.PodmanCompatibility;

/**
 * Singleton containers shared across tests. Each container is started lazily
 * on first access and reused for the entire JVM lifetime — perfect for the
 * E2E suite where startup latency dwarfs test time.
 */
public final class SharedContainers {

    static {
        PodmanCompatibility.apply();
    }

    private SharedContainers() {}

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("ecommerce")
            .withUsername("ecommerce")
            .withPassword("ecommerce")
            .withReuse(true);

    public static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    public static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .withReuse(true);

    public static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.4"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx512m")
            .withReuse(true);

    public static final GenericContainer<?> MINIO = new GenericContainer<>(
            DockerImageName.parse("minio/minio:RELEASE.2024-08-17T01-24-54Z"))
            .withCommand("server", "/data")
            .withEnv("MINIO_ROOT_USER", "minio")
            .withEnv("MINIO_ROOT_PASSWORD", "minio12345")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000))
            .withReuse(true);
}

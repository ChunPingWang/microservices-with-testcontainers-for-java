package com.example.ecommerce.product.adapter.storage;

import com.example.ecommerce.product.adapter.outbound.storage.MinioObjectStorageAdapter;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class MinioObjectStorageAdapterIT {

    @SuppressWarnings("resource")
    static GenericContainer<?> minio;
    static MinioObjectStorageAdapter adapter;

    @BeforeAll
    static void startup() {
        PodmanCompatibility.apply();
        minio = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2024-08-17T01-24-54Z"))
                .withCommand("server", "/data")
                .withEnv("MINIO_ROOT_USER", "minio")
                .withEnv("MINIO_ROOT_PASSWORD", "minio12345")
                .withExposedPorts(9000)
                .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));
        minio.start();

        String endpoint = "http://" + minio.getHost() + ":" + minio.getFirstMappedPort();
        adapter = new MinioObjectStorageAdapter(endpoint, "minio", "minio12345");
    }

    @AfterAll
    static void shutdown() {
        if (minio != null) minio.stop();
    }

    @Test
    void store_and_retrieve_round_trip() throws Exception {
        byte[] payload = "hello world".getBytes();
        URI uri = adapter.store("test-bucket", "hello.txt",
                new ByteArrayInputStream(payload), payload.length, "text/plain");

        assertThat(uri).hasToString("s3://test-bucket/hello.txt");
        try (var in = adapter.retrieve("test-bucket", "hello.txt");
             var scanner = new Scanner(in)) {
            assertThat(scanner.useDelimiter("\\A").next()).isEqualTo("hello world");
        }
    }

    @Test
    void presigned_get_url_includes_signature_params() {
        byte[] payload = "presigned-content".getBytes();
        adapter.store("test-bucket-2", "doc.txt",
                new ByteArrayInputStream(payload), payload.length, "text/plain");

        URI url = adapter.presignedGetUrl("test-bucket-2", "doc.txt", Duration.ofMinutes(5));

        // AWS SigV4 presigned URLs always carry these query parameters
        assertThat(url.getQuery())
                .contains("X-Amz-Signature=")
                .contains("X-Amz-Expires=");
    }
}

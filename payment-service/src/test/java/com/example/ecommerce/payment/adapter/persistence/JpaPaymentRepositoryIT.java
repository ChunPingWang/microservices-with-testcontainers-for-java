package com.example.ecommerce.payment.adapter.persistence;

import com.example.ecommerce.payment.adapter.outbound.persistence.JpaPaymentRepository;
import com.example.ecommerce.payment.domain.model.IdempotencyKey;
import com.example.ecommerce.payment.domain.model.Payment;
import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.payment.domain.model.PaymentStatus;
import com.example.ecommerce.shared.domain.Money;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.example.ecommerce.payment.adapter.outbound.persistence")
@EnableJpaRepositories("com.example.ecommerce.payment.adapter.outbound.persistence")
@ComponentScan(basePackages = "com.example.ecommerce.payment.adapter.outbound.persistence")
@Testcontainers
@Tag("integration")
class JpaPaymentRepositoryIT {

    @BeforeAll
    static void enablePodman() {
        PodmanCompatibility.apply();
    }

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("payment")
            .withUsername("payment")
            .withPassword("payment");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.flyway.url", postgres::getJdbcUrl);
        r.add("spring.flyway.user", postgres::getUsername);
        r.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    JpaPaymentRepository repo;

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void round_trips_payment_with_status_changes() {
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer-1",
                new IdempotencyKey("key-rt"), PaymentMethod.CREDIT_CARD,
                Money.of("12.50", "USD"), FIXED);
        p = repo.save(p);
        p.authorise("AUTH-1", FIXED);
        p.complete("s3://r/1.pdf", FIXED);
        repo.save(p);

        Payment loaded = repo.findById(p.id()).orElseThrow();
        assertThat(loaded.status()).isInstanceOf(PaymentStatus.Completed.class);
        assertThat(loaded.amount().amount()).isEqualByComparingTo("12.50");
    }

    @Test
    void idempotency_key_lookup_works() {
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer-2",
                new IdempotencyKey("key-lookup-A"), PaymentMethod.WALLET,
                Money.of("5.00", "USD"), FIXED);
        repo.save(p);

        assertThat(repo.findByIdempotencyKey(new IdempotencyKey("key-lookup-A"))).isPresent();
        assertThat(repo.findByIdempotencyKey(new IdempotencyKey("key-lookup-MISSING"))).isEmpty();
    }

    @TestConfiguration
    @Import(JpaPaymentRepository.class)
    static class Config {}
}

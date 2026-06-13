package com.example.ecommerce.product.adapter.persistence;

import com.example.ecommerce.product.adapter.outbound.persistence.JpaOrderWriteRepository;
import com.example.ecommerce.product.domain.model.Order;
import com.example.ecommerce.product.domain.model.OrderLine;
import com.example.ecommerce.product.domain.model.OrderStatus;
import com.example.ecommerce.shared.domain.Money;
import com.example.ecommerce.shared.domain.Quantity;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.example.ecommerce.product.adapter.outbound.persistence")
@EnableJpaRepositories("com.example.ecommerce.product.adapter.outbound.persistence")
@ComponentScan(basePackages = "com.example.ecommerce.product.adapter.outbound.persistence")
@Testcontainers
@Tag("integration")
class JpaOrderWriteRepositoryIT {

    @BeforeAll
    static void enablePodman() {
        PodmanCompatibility.apply();
    }

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("product")
            .withUsername("product")
            .withPassword("product");

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
    JpaOrderWriteRepository repo;

    private static final Clock FIXED = Clock.fixed(
            Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void round_trips_an_order_with_lines() {
        Order order = Order.place("buyer-1", List.of(
                new OrderLine("SKU-A", Quantity.of(2), Money.of("10.00", "USD")),
                new OrderLine("SKU-B", Quantity.of(1), Money.of("5.50", "USD"))
        ), FIXED);

        Order saved = repo.save(order);
        Order loaded = repo.findById(saved.id()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.buyerId()).isEqualTo("buyer-1");
        assertThat(loaded.totalAmount().amount()).isEqualByComparingTo("25.50");
        assertThat(loaded.lines()).hasSize(2);
        assertThat(loaded.status()).isInstanceOf(OrderStatus.Created.class);
        assertThat(loaded.version()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void persists_status_transition_paid() {
        Order order = Order.place("buyer-2", List.of(
                new OrderLine("SKU-A", Quantity.of(1), Money.of("10.00", "USD"))
        ), FIXED);
        order = repo.save(order);
        order.markPaid(UUID.randomUUID(), FIXED);
        repo.save(order);

        Order loaded = repo.findById(order.id()).orElseThrow();
        assertThat(loaded.status()).isInstanceOf(OrderStatus.Paid.class);
    }

    @Test
    void persists_status_transition_cancelled_with_reason() {
        Order order = Order.place("buyer-3", List.of(
                new OrderLine("SKU-A", Quantity.of(1), Money.of("10.00", "USD"))
        ), FIXED);
        order = repo.save(order);
        order.cancel("buyer remorse", FIXED);
        repo.save(order);

        Order loaded = repo.findById(order.id()).orElseThrow();
        assertThat(loaded.status()).isInstanceOfSatisfying(
                OrderStatus.Cancelled.class,
                c -> assertThat(c.reason()).isEqualTo("buyer remorse"));
    }

    @TestConfiguration
    @Import(JpaOrderWriteRepository.class)
    static class Config {}
}

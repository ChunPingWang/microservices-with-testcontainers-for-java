package com.example.ecommerce.inventory.adapter.persistence;

import com.example.ecommerce.inventory.adapter.outbound.persistence.JpaStockRepository;
import com.example.ecommerce.inventory.domain.model.SkuId;
import com.example.ecommerce.inventory.domain.model.Stock;
import com.example.ecommerce.shared.domain.Quantity;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.example.ecommerce.inventory.adapter.outbound.persistence")
@EnableJpaRepositories("com.example.ecommerce.inventory.adapter.outbound.persistence")
@ComponentScan(basePackages = "com.example.ecommerce.inventory.adapter.outbound.persistence")
@Testcontainers
@Tag("integration")
class JpaStockRepositoryIT {

    @BeforeAll
    static void enablePodman() {
        PodmanCompatibility.apply();
    }

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("inventory")
            .withUsername("inventory")
            .withPassword("inventory");

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
    JpaStockRepository repo;

    @Test
    void round_trips_stock_with_reservations() {
        SkuId sku = new SkuId("SKU-IT-A");
        Stock stock = Stock.seed(sku, 10);
        UUID orderId = UUID.randomUUID();
        stock.reserve(orderId, Quantity.of(3));
        stock = repo.save(stock);
        stock.commit(orderId);
        repo.save(stock);

        Stock loaded = repo.findBySku(sku).orElseThrow();
        assertThat(loaded.available().value()).isEqualTo(7);
        assertThat(loaded.level().reserved().value()).isZero();
    }

    @Test
    void reservation_persists_until_committed() {
        SkuId sku = new SkuId("SKU-IT-B");
        Stock stock = Stock.seed(sku, 5);
        UUID orderId = UUID.randomUUID();
        stock.reserve(orderId, Quantity.of(2));
        repo.save(stock);

        Stock loaded = repo.findBySku(sku).orElseThrow();
        assertThat(loaded.level().reserved().value()).isEqualTo(2);
    }

    @TestConfiguration
    @Import(JpaStockRepository.class)
    static class Config {}
}

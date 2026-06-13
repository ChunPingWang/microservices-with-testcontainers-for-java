package com.example.ecommerce.product.contract;

import com.example.ecommerce.product.adapter.outbound.persistence.JpaOrderWriteRepository;
import com.example.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.example.ecommerce.product.adapter.outbound.persistence")
@EnableJpaRepositories("com.example.ecommerce.product.adapter.outbound.persistence")
@ComponentScan(basePackages = "com.example.ecommerce.product.adapter.outbound.persistence")
@Testcontainers
@Tag("integration")
class JpaOrderWriteRepositoryContractTest extends OrderWriteRepositoryContract {

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
    JpaOrderWriteRepository jpaRepo;

    @Override
    protected OrderWriteRepository repository() {
        return jpaRepo;
    }

    @TestConfiguration
    @Import(JpaOrderWriteRepository.class)
    static class Config {}
}

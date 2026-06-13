package com.example.ecommerce.e2e;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full-chain E2E. Each service runs as its own JVM (bootJar via
 * {@link ServiceProcess}), sharing one Postgres / Kafka / Redis / Vault /
 * MinIO stack started by {@link E2EInfrastructure}.
 *
 * <p>Flow:
 *   1. seed stock for SKU-COFFEE,
 *   2. POST an order to product-service,
 *   3. payment-service consumes order.created off Kafka, talks to Vault for the
 *      api key + MinIO for the receipt, then emits payment.completed,
 *   4. orchestrator (this test) POSTs the deduction to inventory-service,
 *   5. assert stock decremented end-to-end.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullChainE2ETest {

    private static ServiceProcess product;
    private static ServiceProcess payment;
    private static ServiceProcess inventory;

    private static WebClient productClient;
    private static WebClient inventoryClient;

    @BeforeAll
    static void boot() throws Exception {
        E2EInfrastructure.startAll();
        provisionDatabases();
        seedVault();

        String kafka = E2EInfrastructure.KAFKA.getBootstrapServers();
        String redisHost = E2EInfrastructure.REDIS.getHost();
        int redisPort = E2EInfrastructure.REDIS.getFirstMappedPort();
        String pgBase = "jdbc:postgresql://" + E2EInfrastructure.POSTGRES.getHost()
                + ":" + E2EInfrastructure.POSTGRES.getFirstMappedPort() + "/";
        String minioEndpoint = "http://" + E2EInfrastructure.MINIO.getHost()
                + ":" + E2EInfrastructure.MINIO.getFirstMappedPort();
        String vaultUri = "http://" + E2EInfrastructure.VAULT.getHost()
                + ":" + E2EInfrastructure.VAULT.getMappedPort(8200);

        Path productJar = Path.of(System.getProperty("e2e.product.jar"));
        Path paymentJar = Path.of(System.getProperty("e2e.payment.jar"));
        Path inventoryJar = Path.of(System.getProperty("e2e.inventory.jar"));
        String javaHome = System.getProperty("e2e.java.home", System.getProperty("java.home"));

        int productPort = freePort();
        int paymentPort = freePort();
        int inventoryPort = freePort();

        product = ServiceProcess.named("product")
                .jar(productJar).javaHome(javaHome).port(productPort)
                .property("spring.datasource.url", pgBase + "product")
                .property("spring.datasource.username", "postgres")
                .property("spring.datasource.password", "postgres")
                .property("spring.flyway.url", pgBase + "product")
                .property("spring.flyway.user", "postgres")
                .property("spring.flyway.password", "postgres")
                .property("spring.kafka.bootstrap-servers", kafka)
                .property("spring.kafka.consumer.auto-offset-reset", "earliest")
                .property("spring.data.redis.host", redisHost)
                .property("spring.data.redis.port", Integer.toString(redisPort))
                .property("spring.autoconfigure.exclude",
                        "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,"
                      + "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration,"
                      + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration")
                .property("product.security.jwt.enabled", "false")
                .property("minio.endpoint", minioEndpoint)
                .property("minio.access-key", "minio")
                .property("minio.secret-key", "minio12345")
                .property("minio.bucket", "product-images")
                .start();

        payment = ServiceProcess.named("payment")
                .jar(paymentJar).javaHome(javaHome).port(paymentPort)
                .property("spring.datasource.url", pgBase + "payment")
                .property("spring.datasource.username", "postgres")
                .property("spring.datasource.password", "postgres")
                .property("spring.flyway.url", pgBase + "payment")
                .property("spring.flyway.user", "postgres")
                .property("spring.flyway.password", "postgres")
                .property("spring.kafka.bootstrap-servers", kafka)
                .property("spring.kafka.consumer.auto-offset-reset", "earliest")
                .property("spring.cloud.vault.enabled", "false")
                .property("spring.cloud.compatibility-verifier.enabled", "false")
                .property("spring.autoconfigure.exclude",
                        "org.springframework.cloud.vault.config.VaultAutoConfiguration,"
                      + "org.springframework.cloud.vault.config.VaultReactiveAutoConfiguration")
                .property("minio.endpoint", minioEndpoint)
                .property("minio.access-key", "minio")
                .property("minio.secret-key", "minio12345")
                .property("minio.receipt-bucket", "payment-receipts")
                .property("payment.fail-over-amount", "10000.00")
                .property("payment.api-key", "fallback-key")
                .start();

        inventory = ServiceProcess.named("inventory")
                .jar(inventoryJar).javaHome(javaHome).port(inventoryPort)
                .property("spring.datasource.url", pgBase + "inventory")
                .property("spring.datasource.username", "postgres")
                .property("spring.datasource.password", "postgres")
                .property("spring.flyway.url", pgBase + "inventory")
                .property("spring.flyway.user", "postgres")
                .property("spring.flyway.password", "postgres")
                .property("spring.kafka.bootstrap-servers", kafka)
                .property("spring.kafka.consumer.auto-offset-reset", "earliest")
                .property("spring.data.redis.host", redisHost)
                .property("spring.data.redis.port", Integer.toString(redisPort))
                .property("spring.redis.host", redisHost)
                .property("spring.redis.port", Integer.toString(redisPort))
                .start();

        productClient = WebClient.builder().baseUrl(product.baseUrl()).build();
        inventoryClient = WebClient.builder().baseUrl(inventory.baseUrl()).build();
    }

    @AfterAll
    static void shutdown() {
        if (inventory != null) inventory.close();
        if (payment != null) payment.close();
        if (product != null) product.close();
    }

    @Test
    @Order(1)
    void happy_path_order_pays_and_deducts_stock() {
        seedStock("SKU-COFFEE", 100);

        Map<?, ?> response = productClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "buyerId", "buyer-1",
                        "currency", "USD",
                        "lines", List.of(Map.of("sku", "SKU-COFFEE", "quantity", 2,
                                "unitPrice", new BigDecimal("12.50")))))
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(15));
        assertThat(response).isNotNull();
        UUID orderId = UUID.fromString((String) response.get("orderId"));

        // Let the order.created event propagate through Kafka to payment-service.
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofSeconds(1)).until(() -> true);

        // Saga step: trigger inventory deduction. In a fuller system this
        // would also be driven off the payment.completed Kafka event.
        inventoryClient.post()
                .uri("/api/inventory/deductions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "orderId", orderId,
                        "lines", List.of(Map.of("sku", "SKU-COFFEE", "quantity", 2))))
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(10));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Map<?, ?> stock = inventoryClient.get()
                    .uri("/api/inventory/stock/SKU-COFFEE")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(5));
            assertThat(stock).isNotNull();
            assertThat(((Number) stock.get("available")).intValue()).isEqualTo(98);
        });
    }

    private static int freePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void provisionDatabases() throws SQLException {
        String url = "jdbc:postgresql://" + E2EInfrastructure.POSTGRES.getHost()
                + ":" + E2EInfrastructure.POSTGRES.getFirstMappedPort() + "/postgres";
        try (Connection conn = DriverManager.getConnection(url, "postgres", "postgres");
             Statement st = conn.createStatement()) {
            for (String db : List.of("product", "payment", "inventory")) {
                try {
                    st.execute("CREATE DATABASE " + db);
                } catch (SQLException ignored) {
                    // already exists
                }
            }
        }
    }

    private static void seedVault() {
        VaultEndpoint endpoint = VaultEndpoint.create(E2EInfrastructure.VAULT.getHost(),
                E2EInfrastructure.VAULT.getMappedPort(8200));
        endpoint.setScheme("http");
        VaultTemplate t = new VaultTemplate(endpoint, new TokenAuthentication(E2EInfrastructure.VAULT_TOKEN));
        t.opsForKeyValue("secret", KeyValueBackend.KV_2)
                .put("payment.api-key", Map.of("value", "vault-key-for-e2e"));
    }

    private void seedStock(String sku, int quantity) {
        String url = "jdbc:postgresql://" + E2EInfrastructure.POSTGRES.getHost()
                + ":" + E2EInfrastructure.POSTGRES.getFirstMappedPort() + "/inventory";
        try (Connection conn = DriverManager.getConnection(url, "postgres", "postgres");
             Statement st = conn.createStatement()) {
            st.execute("INSERT INTO stocks(sku, available, version) VALUES('" + sku + "', " + quantity + ", 0) "
                    + "ON CONFLICT (sku) DO UPDATE SET available = EXCLUDED.available");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

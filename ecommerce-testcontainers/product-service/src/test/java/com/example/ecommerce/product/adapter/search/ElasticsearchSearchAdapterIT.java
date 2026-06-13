package com.example.ecommerce.product.adapter.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.example.ecommerce.product.adapter.outbound.search.ElasticsearchSearchAdapter;
import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase.ProductView;
import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase.SearchQuery;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class ElasticsearchSearchAdapterIT {

    static ElasticsearchContainer es;
    static RestClient restClient;
    static ElasticsearchSearchAdapter adapter;

    @BeforeAll
    static void startup() {
        PodmanCompatibility.apply();
        es = new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.4"))
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node")
                // Keep ES under 512 MB heap so it fits in small VMs / CI runners.
                .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx512m")
                .withStartupTimeout(Duration.ofMinutes(3));
        es.start();

        HttpHost host = HttpHost.create(es.getHttpHostAddress());
        restClient = RestClient.builder(host).build();
        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        var client = new ElasticsearchClient(transport);
        var template = new ElasticsearchTemplate(client);
        adapter = new ElasticsearchSearchAdapter(template);
    }

    @AfterAll
    static void shutdown() throws Exception {
        if (restClient != null) restClient.close();
        if (es != null) es.stop();
    }

    @Test
    void index_and_search_by_keyword() {
        ProductView coffee = new ProductView(
                UUID.randomUUID().toString(), "SKU-COFFEE", "Coffee Bean",
                new BigDecimal("12.00"), "USD", true);
        ProductView tea = new ProductView(
                UUID.randomUUID().toString(), "SKU-TEA", "Tea Leaf",
                new BigDecimal("8.00"), "USD", true);
        adapter.index(coffee);
        adapter.index(tea);

        List<ProductView> matches = adapter.search(new SearchQuery("coffee", 0, 10));
        assertThat(matches).extracting(ProductView::name).contains("Coffee Bean");
        assertThat(matches).extracting(ProductView::name).doesNotContain("Tea Leaf");
    }

    @Test
    void search_blank_returns_paged_all() {
        for (int i = 0; i < 3; i++) {
            adapter.index(new ProductView(
                    UUID.randomUUID().toString(), "SKU-BULK-" + i, "Item " + i,
                    new BigDecimal("1.00"), "USD", true));
        }
        List<ProductView> page = adapter.search(new SearchQuery("", 0, 2));
        assertThat(page).hasSize(2);
    }
}

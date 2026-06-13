package com.example.ecommerce.product.application;

import com.example.ecommerce.product.application.query.SearchProductQueryHandler;
import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase;
import com.example.ecommerce.product.fakes.InMemoryCachePort;
import com.example.ecommerce.product.fakes.InMemorySearchPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchProductQueryHandlerTest {

    private final InMemorySearchPort search = new InMemorySearchPort();
    private final InMemoryCachePort cache = new InMemoryCachePort();
    private final SearchProductQueryHandler handler = new SearchProductQueryHandler(search, cache);

    @Test
    void returns_search_results_on_miss_and_populates_cache() {
        search.index(new SearchProductUseCase.ProductView(
                "id-1", "SKU-COFFEE", "Coffee Bean", new BigDecimal("12.00"), "USD", true));
        search.index(new SearchProductUseCase.ProductView(
                "id-2", "SKU-TEA", "Tea Leaf", new BigDecimal("8.00"), "USD", true));

        List<SearchProductUseCase.ProductView> result = handler.search(
                new SearchProductUseCase.SearchQuery("coffee", 0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Coffee Bean");

        // Second call → should be served from cache, even after we wipe the search engine
        search.clear();
        List<SearchProductUseCase.ProductView> cached = handler.search(
                new SearchProductUseCase.SearchQuery("coffee", 0, 10));
        assertThat(cached).hasSize(1);
        assertThat(cached.getFirst().name()).isEqualTo("Coffee Bean");
    }

    @Test
    void empty_keyword_returns_all_paged() {
        for (int i = 0; i < 5; i++) {
            search.index(new SearchProductUseCase.ProductView(
                    "id-" + i, "SKU-" + i, "Item " + i,
                    new BigDecimal("1.00"), "USD", true));
        }
        List<SearchProductUseCase.ProductView> page0 = handler.search(
                new SearchProductUseCase.SearchQuery("", 0, 2));
        assertThat(page0).hasSize(2);
    }
}

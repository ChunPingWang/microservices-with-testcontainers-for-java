package com.example.ecommerce.product.application.query;

import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase;
import com.example.ecommerce.product.domain.port.outbound.CachePort;
import com.example.ecommerce.product.domain.port.outbound.SearchPort;

import java.time.Duration;
import java.util.List;

/**
 * Cache-aside read path: try cache → fall back to search engine → cache result.
 */
public class SearchProductQueryHandler implements SearchProductUseCase {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final SearchPort searchPort;
    private final CachePort cachePort;

    public SearchProductQueryHandler(SearchPort searchPort, CachePort cachePort) {
        this.searchPort = searchPort;
        this.cachePort = cachePort;
    }

    @Override
    public List<ProductView> search(SearchQuery query) {
        String cacheKey = "search:" + query.keyword() + ":" + query.page() + ":" + query.size();
        return cachePort.get(cacheKey, ProductViewList.class)
                .map(ProductViewList::items)
                .orElseGet(() -> {
                    List<ProductView> result = searchPort.search(query);
                    cachePort.put(cacheKey, new ProductViewList(result), CACHE_TTL);
                    return result;
                });
    }

    /** Wrapper so the cache adapter can serialise a list under a single key. */
    public record ProductViewList(List<ProductView> items) {}
}
